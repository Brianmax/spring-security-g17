package com.jwt.codigo.repository;

import com.jwt.codigo.entity.UserCredentialEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository extends JpaRepository<UserCredentialEntity, UUID> {

    @EntityGraph(attributePaths = {"user", "roles", "roles.permissions"})
    Optional<UserCredentialEntity> findByUserEmailIgnoreCase(String email);

    @Override
    @EntityGraph(attributePaths = {"user", "roles", "roles.permissions"})
    Optional<UserCredentialEntity> findById(UUID userId);
}
