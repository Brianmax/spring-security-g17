package com.jwt.codigo.controller;

import com.jwt.codigo.dto.common.PageResponse;
import com.jwt.codigo.dto.user.CreateUserRequest;
import com.jwt.codigo.dto.user.UpdateUserRequest;
import com.jwt.codigo.dto.user.UserResponse;
import com.jwt.codigo.exception.ApiErrorDocumentation;
import com.jwt.codigo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Manage virtual banking users")
@ApiErrorDocumentation
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create:any')")
    @Operation(summary = "Create a user")
    @ApiResponse(responseCode = "201", description = "User created",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('user:read:any')")
    @Operation(summary = "List users", description = "Returns a zero-based page; default size is 20 and maximum size is 100")
    @ApiResponse(responseCode = "200", description = "User page")
    public PageResponse<UserResponse> findAll(
            @ParameterObject @PageableDefault(sort = "createdAt") Pageable pageable
    ) {
        return userService.findAll(pageable);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:read:any') or (hasAuthority('user:read:self') and @bankingAuthorization.isSelf(authentication, #userId))")
    @Operation(summary = "Get a user")
    @ApiResponse(responseCode = "200", description = "User found",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public UserResponse findById(
            @Parameter(description = "User UUID") @PathVariable UUID userId
    ) {
        return userService.findById(userId);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:update:any') or (hasAuthority('user:update:self') and @bankingAuthorization.isSelf(authentication, #userId))")
    @Operation(summary = "Update a user")
    @ApiResponse(responseCode = "200", description = "User updated",
            content = @Content(schema = @Schema(implementation = UserResponse.class)))
    public UserResponse update(
            @Parameter(description = "User UUID") @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return userService.update(userId, request);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('user:deactivate:any')")
    @Operation(summary = "Delete or deactivate a user",
            description = "Hard-deletes a user with no accounts; otherwise changes the user status to INACTIVE")
    @ApiResponse(responseCode = "204", description = "User deleted or deactivated", content = @Content)
    public ResponseEntity<Void> delete(
            @Parameter(description = "User UUID") @PathVariable UUID userId
    ) {
        userService.deleteOrDeactivate(userId);
        return ResponseEntity.noContent().build();
    }
}
