package com.mcloud.auth0_authenticator.infrastructure.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private final String rolesClaim;

    public RoleConverter(String rolesClaim) {
        this.rolesClaim = rolesClaim;
    }

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        List<String> roles = jwt.getClaimAsStringList(rolesClaim);

        return Stream.concat(
                        permissions != null ? permissions.stream() : Stream.empty(),
                        roles != null ? roles.stream().map(role -> "ROLE_" + role) : Stream.empty()
                ).map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }
}