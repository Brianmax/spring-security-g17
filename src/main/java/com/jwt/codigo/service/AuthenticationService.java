package com.jwt.codigo.service;

import com.jwt.codigo.config.JwtProperties;
import com.jwt.codigo.dto.auth.*;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.entity.PermissionEntity;
import com.jwt.codigo.entity.RoleEntity;
import com.jwt.codigo.entity.UserCredentialEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.UserMapper;
import com.jwt.codigo.repository.RoleRepository;
import com.jwt.codigo.repository.UserCredentialRepository;
import com.jwt.codigo.repository.UserRepository;
import com.jwt.codigo.security.JwtTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final JwtProperties jwtProperties;

    public AuthenticationService(UserRepository userRepository, UserCredentialRepository credentialRepository,
                                 RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserMapper userMapper,
                                 JwtTokenService jwtTokenService, RefreshTokenService refreshTokenService,
                                 JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenService = refreshTokenService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        validatePassword(request.password());
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "A user with this email already exists");
        }
        RoleEntity customer = roleRepository.findByCode("CUSTOMER")
                .orElseThrow(() -> new IllegalStateException("CUSTOMER role is not configured"));
        UserEntity user = userRepository.save(new UserEntity(request.firstName().trim(), request.lastName().trim(), email));
        credentialRepository.saveAndFlush(new UserCredentialEntity(user, passwordEncoder.encode(request.password()), customer));
        return userMapper.toResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request) {
        UserCredentialEntity credential = credentialRepository.findByUserEmailIgnoreCase(normalizeEmail(request.email()))
                .orElseThrow(this::invalidCredentials);
        if (credential.getUser().getStatus() != UserStatus.ACTIVE
                || !passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            throw invalidCredentials();
        }
        return issuePair(credential, refreshTokenService.issue(credential.getUser()).rawToken());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.refreshToken());
        UserCredentialEntity credential = credentialRepository.findById(rotation.user().getId())
                .orElseThrow(this::invalidRefreshToken);
        if (credential.getUser().getStatus() != UserStatus.ACTIVE) {
            refreshTokenService.revokeAll(credential.getUserId());
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "The user account is disabled");
        }
        return issuePair(credential, rotation.rawToken());
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse me(UUID userId) {
        UserCredentialEntity credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        UserEntity user = credential.getUser();
        return new CurrentUserResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(),
                user.getStatus(), roles(credential), permissions(credential));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        validatePassword(request.newPassword());
        UserCredentialEntity credential = credentialRepository.findById(userId)
                .orElseThrow(this::invalidCredentials);
        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw invalidCredentials();
        }
        if (passwordEncoder.matches(request.newPassword(), credential.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD_POLICY_VIOLATION",
                    "The new password must be different from the current password");
        }
        credential.changePassword(passwordEncoder.encode(request.newPassword()));
        refreshTokenService.revokeAll(userId);
    }

    public void logout(UUID userId, LogoutRequest request) {
        refreshTokenService.logout(userId, request.refreshToken());
    }

    private TokenResponse issuePair(UserCredentialEntity credential, String refreshToken) {
        return new TokenResponse(jwtTokenService.createAccessToken(credential), refreshToken, "Bearer",
                jwtProperties.getAccessTokenTtl().toSeconds());
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PASSWORD_POLICY_VIOLATION",
                    "Password must contain 12 to 128 characters, uppercase, lowercase and a number");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is invalid");
    }

    private ApiException invalidRefreshToken() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh token is invalid");
    }

    private Set<String> roles(UserCredentialEntity credential) {
        return credential.getRoles().stream().map(RoleEntity::getCode).collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> permissions(UserCredentialEntity credential) {
        return credential.getRoles().stream().flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getCode).collect(Collectors.toUnmodifiableSet());
    }
}
