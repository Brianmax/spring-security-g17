package com.jwt.codigo.security;

import com.jwt.codigo.entity.PermissionEntity;
import com.jwt.codigo.entity.RoleEntity;
import com.jwt.codigo.entity.UserCredentialEntity;
import com.jwt.codigo.repository.UserCredentialRepository;
import com.nimbusds.jwt.JWT;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractOAuth2TokenAuthenticationToken<Jwt>> {

    private final UserCredentialRepository userCredentialRepository;

    public JwtUserAuthenticationConverter(UserCredentialRepository userCredentialRepository) {
        this.userCredentialRepository = userCredentialRepository;
    }

    @Override
    public AbstractOAuth2TokenAuthenticationToken<Jwt> convert(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        UserCredentialEntity credentialEntity = userCredentialRepository.findById(userId)
                .orElse(null);

        Long tokenVersion = jwt.getClaim("ver");
        if(credentialEntity.getAuthVersion() != tokenVersion) {
            throw new BadCredentialsException("El acceso mediante este token ya no es valido");
        }


        // extraemos los roles actuales
        List<SimpleGrantedAuthority> authorities = roles(credentialEntity).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();

        return new JwtAuthenticationToken( // Authentication
                jwt,
                authorities,
                userId.toString()
        );
    }

    private Set<String> roles(UserCredentialEntity credential) {
        return credential.getRoles().stream()
                .map(RoleEntity::getCode)
                .collect(Collectors.toSet());
    }

    private Set<String> permissions(UserCredentialEntity credential) {
        return credential.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(PermissionEntity::getCode)
                .collect(Collectors.toSet());
    }
}
