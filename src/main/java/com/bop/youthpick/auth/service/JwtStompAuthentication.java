package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public final class JwtStompAuthentication extends AbstractAuthenticationToken {

    private final AuthPrincipal principal;

    public JwtStompAuthentication(Long userId, String role) {
        super(List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        this.principal = new AuthPrincipal(userId, role);
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public AuthPrincipal getPrincipal() {
        return principal;
    }

    @Override
    public String getName() {
        return principal.userId().toString();
    }
}
