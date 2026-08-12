package com.jwt.codigo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;
import org.springframework.data.domain.Persistable;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "auth_credentials")
public class UserCredentialEntity extends AuditableEntity implements Persistable<UUID> {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Column(name = "auth_version", nullable = false)
    private long authVersion;

    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles = new LinkedHashSet<>();

    @Transient
    private boolean newEntity = true;

    protected UserCredentialEntity() {
    }

    public UserCredentialEntity(UserEntity user, String passwordHash, RoleEntity initialRole) {
        this.user = user;
        this.userId = user.getId();
        this.passwordHash = passwordHash;
        this.authVersion = 1;
        this.passwordChangedAt = Instant.now();
        this.roles.add(initialRole);
    }

    public void changePassword(String newHash) {
        passwordHash = newHash;
        passwordChangedAt = Instant.now();
        incrementAuthVersion();
    }

    public void replaceRoles(Set<RoleEntity> newRoles) {
        roles.clear();
        roles.addAll(newRoles);
        incrementAuthVersion();
    }

    public void incrementAuthVersion() {
        authVersion++;
    }

    @Override
    public UUID getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        newEntity = false;
    }

    public UUID getUserId() {
        return userId;
    }

    public UserEntity getUser() {
        return user;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public long getAuthVersion() {
        return authVersion;
    }

    public Instant getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public Set<RoleEntity> getRoles() {
        return roles;
    }
}
