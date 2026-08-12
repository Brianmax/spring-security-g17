package com.jwt.codigo.controller;

import com.jwt.codigo.dto.auth.PermissionResponse;
import com.jwt.codigo.dto.auth.ReplaceRolesRequest;
import com.jwt.codigo.dto.auth.RoleResponse;
import com.jwt.codigo.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/security")
@Tag(name = "Security administration", description = "Roles and permissions")
public class RoleController {
    private final RoleService service;

    public RoleController(RoleService service) {
        this.service = service;
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('role:read:any')")
    public List<RoleResponse> roles() {
        return service.roles();
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('permission:read:any')")
    public List<PermissionResponse> permissions() {
        return service.permissions();
    }

    @PutMapping("/users/{userId}/roles")
    @PreAuthorize("hasAuthority('role:assign:any')")
    public List<RoleResponse> replaceRoles(@PathVariable UUID userId,
                                           @Valid @RequestBody ReplaceRolesRequest request) {
        return service.replaceRoles(userId, request.roles());
    }
}
