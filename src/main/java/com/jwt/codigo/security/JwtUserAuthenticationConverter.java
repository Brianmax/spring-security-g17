package com.jwt.codigo.security;

import com.jwt.codigo.entity.PermissionEntity;
import com.jwt.codigo.entity.RoleEntity;
import com.jwt.codigo.entity.UserCredentialEntity;
import com.jwt.codigo.enums.UserStatus;
import com.jwt.codigo.repository.UserCredentialRepository;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {
    private final UserCredentialRepository repository;

    public JwtUserAuthenticationConverter(UserCredentialRepository repository) {
        this.repository = repository;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (RuntimeException exception) {
            throw new BadCredentialsException("Invalid token subject");
        }
        UserCredentialEntity credential = repository.findById(userId)
                .orElseThrow(() -> new BadCredentialsException("Token user no longer exists"));
        Number version = jwt.getClaim("ver");
        if (version == null || credential.getAuthVersion() != version.longValue()) {
            throw new BadCredentialsException("The access token has been revoked");
        }
        if (credential.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("The user account is disabled");
        }
        Set<SimpleGrantedAuthority> authorities = new LinkedHashSet<>();
        credential.getRoles().stream().map(RoleEntity::getCode)
                .map(code -> new SimpleGrantedAuthority("ROLE_" + code)).forEach(authorities::add);
        credential.getRoles().stream().flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getCode).map(SimpleGrantedAuthority::new).forEach(authorities::add);
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }
}
