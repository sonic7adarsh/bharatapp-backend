package com.bharatshop.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements Authentication {
    private final String userId;
    private final String name;
    private final String role;
    private boolean authenticated = true;

    public UserPrincipal(String userId, String name, String role) {
        this.userId = userId;
        this.name = name;
        this.role = role == null ? "USER" : role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public Object getCredentials() { return null; }

    @Override
    public Object getDetails() { return null; }

    @Override
    public Object getPrincipal() { return userId; }

    @Override
    public boolean isAuthenticated() { return authenticated; }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException { this.authenticated = isAuthenticated; }

    @Override
    public String getName() { return name; }

    public String getUserId() { return userId; }

    public String getRole() { return role; }

    // Minimal multi-role helpers
    public boolean hasRole(String expected) {
        if (expected == null || expected.isBlank()) return false;
        return this.role != null && this.role.equalsIgnoreCase(expected);
    }

    public boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) return false;
        for (String r : roles) {
            if (hasRole(r)) return true;
        }
        return false;
    }

    public static UserPrincipal current() {
        org.springframework.security.core.Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserPrincipal) {
            return (UserPrincipal) auth;
        }
        return null;
    }
}