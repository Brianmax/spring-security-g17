package com.jwt.codigo.repository;

import com.jwt.codigo.entity.RoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID> {

    @EntityGraph(attributePaths = "permissions")
    Optional<RoleEntity> findByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findByCodeIn(Collection<String> codes);

    @Override
    @EntityGraph(attributePaths = "permissions")
    List<RoleEntity> findAll();
}
