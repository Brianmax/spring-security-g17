package com.jwt.codigo.service;

import com.jwt.codigo.dto.auth.LoginRequest;
import com.jwt.codigo.dto.auth.RegisterRequest;
import com.jwt.codigo.dto.auth.TokenResponse;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.entity.RoleEntity;
import com.jwt.codigo.entity.UserCredentialEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.mapper.UserMapper;
import com.jwt.codigo.repository.RoleRepository;
import com.jwt.codigo.repository.UserCredentialRepository;
import com.jwt.codigo.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final UserCredentialRepository credentialRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthenticationService(UserRepository userRepository, UserCredentialRepository credentialRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional
    public UserResponse register(RegisterRequest registerRequest) {
        // validacion
        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "Email ya esta siendo usado");
        }

        RoleEntity customerRole =
                roleRepository.findByCode("CUSTOMER")
                        .orElseThrow(() -> new IllegalStateException("No existe role de customer"));

        UserEntity user = userRepository.save(new UserEntity(
                registerRequest.firstName().trim(),
                registerRequest.lastName().trim(),
                email
        ));


        UserCredentialEntity uce = new UserCredentialEntity
                (user, passwordEncoder.encode(registerRequest.password()),customerRole);
        credentialRepository.save(uce);
        userRepository.flush();
        credentialRepository.flush();

        return userMapper.toResponse(user);
    }

//    public TokenResponse login(LoginRequest request) {
//        UserCredentialEntity credential = credentialRepository.findByUserEmailIgnoreCase()
//    }
}
