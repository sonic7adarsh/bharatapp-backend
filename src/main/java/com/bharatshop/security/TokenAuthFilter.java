package com.bharatshop.security;

import com.bharatshop.service.AuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtService jwtService;

    public TokenAuthFilter(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isEnabled()) {
                var payload = jwtService.parse(token);
                if (payload != null) {
                    // Prefer activeRole when available; fall back to legacy role
                    String effectiveRole = payload.activeRole() != null && !payload.activeRole().isBlank()
                            ? payload.activeRole()
                            : payload.role();
                    var principal = new UserPrincipal(payload.userId(), payload.name(), effectiveRole);
                    var ctx = SecurityContextHolder.getContext();
                    ctx.setAuthentication(principal);
                }
            } else {
                var session = authService.getSessionByToken(token);
                if (session != null) {
                    var principal = new UserPrincipal(session.userId(), session.name(), session.role());
                    var ctx = SecurityContextHolder.getContext();
                    ctx.setAuthentication(principal);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}