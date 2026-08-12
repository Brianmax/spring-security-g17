package com.jwt.codigo.controller;

import com.jwt.codigo.dto.auth.*;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, sessions and current identity")
public class AuthController {
    private final AuthenticationService service;

    public AuthController(AuthenticationService service) {
        this.service = service;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a customer")
    @SecurityRequirements
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = service.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in")
    @SecurityRequirements
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate a refresh token")
    @SecurityRequirements
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return service.refresh(request);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke a refresh-token family")
    public ResponseEntity<Void> logout(Authentication authentication, @Valid @RequestBody LogoutRequest request) {
        service.logout(userId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Get the current identity")
    public CurrentUserResponse me(Authentication authentication) {
        return service.me(userId(authentication));
    }

    @PutMapping("/password")
    @Operation(summary = "Change the current password and revoke sessions")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(userId(authentication), request);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
