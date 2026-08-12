package com.jwt.codigo.service;

import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.dto.user.CreateUserRequest;
import com.jwt.codigo.dto.user.UpdateUserRequest;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.UserMapper;
import com.jwt.codigo.repository.BankAccountRepository;
import com.jwt.codigo.repository.UserRepository;
import com.jwt.codigo.repository.UserCredentialRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BankAccountRepository accountRepository;
    private final UserMapper userMapper;
    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenService refreshTokenService;

    public UserService(UserRepository userRepository, BankAccountRepository accountRepository, UserMapper userMapper) {
        this(userRepository, accountRepository, userMapper, null, null);
    }

    @Autowired
    public UserService(UserRepository userRepository, BankAccountRepository accountRepository, UserMapper userMapper,
                       UserCredentialRepository credentialRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.userMapper = userMapper;
        this.credentialRepository = credentialRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, null);
        UserEntity user = new UserEntity(request.firstName().trim(), request.lastName().trim(), email);
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable).map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(UUID userId) {
        return userMapper.toResponse(getEntity(userId));
    }

    @Transactional
    public UserResponse update(UUID userId, UpdateUserRequest request) {
        UserEntity user = getEntity(userId);
        String email = normalizeEmail(request.email());
        ensureEmailAvailable(email, userId);
        boolean statusChanged = user.getStatus() != request.status();
        user.update(request.firstName().trim(), request.lastName().trim(), email, request.status());
        if (statusChanged) {
            invalidateAuthentication(userId);
        }
        return userMapper.toResponse(userRepository.saveAndFlush(user));
    }

    @Transactional
    public void deleteOrDeactivate(UUID userId) {
        UserEntity user = getEntity(userId);
        if (accountRepository.existsByOwnerId(userId)) {
            user.deactivate();
            invalidateAuthentication(userId);
            userRepository.save(user);
        } else {
            userRepository.delete(user);
        }
    }

    public UserEntity getEntity(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
    }

    private void invalidateAuthentication(UUID userId) {
        if (credentialRepository == null || refreshTokenService == null) {
            return;
        }
        credentialRepository.findById(userId).ifPresent(credential -> {
            credential.incrementAuthVersion();
            refreshTokenService.revokeAll(userId);
        });
    }

    private void ensureEmailAvailable(String email, UUID currentUserId) {
        boolean exists = currentUserId == null
                ? userRepository.existsByEmailIgnoreCase(email)
                : userRepository.existsByEmailIgnoreCaseAndIdNot(email, currentUserId);
        if (exists) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "A user with this email already exists");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
