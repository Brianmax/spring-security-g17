package com.jwt.codigo.service;

import com.jwt.codigo.config.JwtProperties;
import com.jwt.codigo.entity.RefreshTokenEntity;
import com.jwt.codigo.entity.UserEntity;
import com.jwt.codigo.exception.ApiException;
import com.jwt.codigo.repository.RefreshTokenRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional
    public IssuedRefreshToken issue(UserEntity user) {
        return create(user, UUID.randomUUID());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Rotation rotate(String rawToken) {
        RefreshTokenEntity current = findForUpdate(rawToken);
        Instant now = Instant.now();
        if (current.isRevoked()) {
            repository.revokeFamily(current.getFamilyId(), now);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSED", "Refresh token reuse was detected");
        }
        if (current.isExpired(now)) {
            current.revoke();
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "The refresh token has expired");
        }
        IssuedRefreshToken replacement = create(current.getUser(), current.getFamilyId());
        current.rotateTo(replacement.entity());
        repository.save(current);
        return new Rotation(current.getUser(), replacement.rawToken());
    }

    @Transactional
    public void logout(UUID authenticatedUserId, String rawToken) {
        RefreshTokenEntity token = findForUpdate(rawToken);
        if (!token.getUser().getId().equals(authenticatedUserId)) {
            throw invalid();
        }
        repository.revokeFamily(token.getFamilyId(), Instant.now());
    }

    @Transactional
    public void revokeAll(UUID userId) {
        repository.revokeAllForUser(userId, Instant.now());
    }

    private IssuedRefreshToken create(UserEntity user, UUID familyId) {
        byte[] value = new byte[32];
        secureRandom.nextBytes(value);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(value);
        RefreshTokenEntity entity = repository.save(new RefreshTokenEntity(
                user, hash(raw), familyId, Instant.now().plus(properties.getRefreshTokenTtl())));
        return new IssuedRefreshToken(entity, raw);
    }

    private RefreshTokenEntity findForUpdate(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.length() > 512) {
            throw invalid();
        }
        return repository.findByTokenHashForUpdate(hash(rawToken))
                .orElseThrow(this::invalid);
    }

    private ApiException invalid() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "The refresh token is invalid");
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record IssuedRefreshToken(RefreshTokenEntity entity, String rawToken) {}
    public record Rotation(UserEntity user, String rawToken) {}
}
