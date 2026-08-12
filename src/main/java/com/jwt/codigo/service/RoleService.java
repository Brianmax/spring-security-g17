package com.jwt.codigo.service;

import com.jwt.codigo.dto.auth.PermissionResponse;
import com.jwt.codigo.dto.auth.RoleResponse;
import com.jwt.codigo.entity.PermissionEntity;
import com.jwt.codigo.entity.RoleEntity;
import com.jwt.codigo.entity.UserCredentialEntity;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.repository.PermissionRepository;
import com.jwt.codigo.repository.RoleRepository;
import com.jwt.codigo.repository.UserCredentialRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserCredentialRepository credentialRepository;
    private final RefreshTokenService refreshTokenService;

    public RoleService(RoleRepository roleRepository, PermissionRepository permissionRepository,
                       UserCredentialRepository credentialRepository, RefreshTokenService refreshTokenService) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.credentialRepository = credentialRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> permissions() {
        return permissionRepository.findAll().stream()
                .map(p -> new PermissionResponse(p.getId(), p.getCode(), p.getDescription())).toList();
    }

    @Transactional
    public List<RoleResponse> replaceRoles(UUID userId, Set<String> requestedCodes) {
        Set<String> codes = requestedCodes.stream().map(String::trim).map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        List<RoleEntity> found = roleRepository.findByCodeIn(codes);
        if (found.size() != codes.size()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "One or more roles do not exist");
        }
        UserCredentialEntity credential = credentialRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        credential.replaceRoles(new LinkedHashSet<>(found));
        refreshTokenService.revokeAll(userId);
        return found.stream().map(this::toResponse).toList();
    }

    private RoleResponse toResponse(RoleEntity role) {
        Set<String> permissions = role.getPermissions().stream().map(PermissionEntity::getCode)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new RoleResponse(role.getId(), role.getCode(), role.getDescription(), permissions);
    }
}
