package com.certainlyinstock.auth_service.config;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Maps Keycloak realm roles (the {@code realm_access.roles} claim) to Spring Security
 * authorities, so {@code ADMIN} becomes {@code ROLE_ADMIN} and works with
 * {@code hasRole('ADMIN')} / {@code @PreAuthorize}.
 *
 * This replaces the {@code SimpleAuthorityMapper} used with the Keycloak adapter in Chapter 9.
 */
public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof Map<?, ?> realmAccessMap)) {
            return Collections.emptyList();
        }
        if (!(realmAccessMap.get("roles") instanceof List<?> roles)) {
            return Collections.emptyList();
        }
        return roles.stream()
                .filter(String.class::isInstance)
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                .toList();
    }
}
