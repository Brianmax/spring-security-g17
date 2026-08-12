package com.jwt.codigo.security;

import com.jwt.codigo.config.JwtProperties;
import com.jwt.codigo.entity.UserCredentialEntity;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.jwt.codigo.entity.PermissionEntity;
import com.jwt.codigo.entity.RoleEntity;

@Service
public class JwtTokenService {
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtTokenService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }


    public String createAccessToken(UserCredentialEntity credential) {
        Instant issuedAt = Instant.now();

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .audience(List.of(jwtProperties.getAudience()))
                .subject(credential.getUserId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .notBefore(issuedAt)
                .expiresAt(issuedAt.plus(jwtProperties.getAccessTokenTtl()))
                .claim("roles", credential.getRoles().stream().map(RoleEntity::getCode).sorted().toList())
                .claim("permissions", credential.getRoles().stream()
                        .flatMap(role -> role.getPermissions().stream())
                        .map(PermissionEntity::getCode).distinct().sorted().toList())
                .claim("ver", credential.getAuthVersion())
                .build();

        JwsHeader header = JwsHeader
                .with(SignatureAlgorithm.RS256)
                .type("JWT")
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet))
                .getTokenValue();
    }
}
