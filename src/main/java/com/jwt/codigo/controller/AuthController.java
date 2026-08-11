package com.jwt.codigo.controller;

import com.jwt.codigo.dto.auth.RegisterRequest;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.service.AuthenticationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        UserResponse response = authenticationService.register(registerRequest);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }
}
