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
import org.springframework.util.AntPathMatcher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.IOException;
import java.util.List;

@Component
public class TokenAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final JwtService jwtService;

    public TokenAuthFilter(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    // Skip filtering for public endpoints and preflight requests to avoid 401/500 on public APIs
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        String path = request.getRequestURI();
        AntPathMatcher matcher = new AntPathMatcher();
        List<String> publicPatterns = List.of(
                "/api/storefront/**",
                "/api/auth/**",
                "/actuator/**",
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html"
        );
        for (String p : publicPatterns) {
            if (matcher.match(p, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isEnabled()) {
                JwtService.Payload payload = jwtService.parse(token);
                if (payload != null) {
                    // Prefer activeRole when available; fall back to role
                    String effectiveRole = (payload.activeRole() != null && !payload.activeRole().isBlank())
                            ? payload.activeRole()
                            : payload.role();

                    // Construct required principal
                    UserPrincipal principal = new UserPrincipal(payload.userId(), payload.name(), effectiveRole);

                    // Map ALL roles from JWT to ROLE_* authorities
                    List<String> roles = (payload.roles() != null && !payload.roles().isEmpty())
                            ? payload.roles()
                            : List.of(effectiveRole);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .filter(r -> r != null && !r.isBlank())
                            .map(r -> new SimpleGrantedAuthority("ROLE_" + r.trim()))
                            .toList();

                    // ROLE_* authorities and null credentials
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    authorities
                            );

                    // Store in SecurityContext before controller execution
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } else {
                var session = authService.getSessionByToken(token);
                if (session != null) {
                    UserPrincipal principal = new UserPrincipal(session.userId(), session.name(), session.role());

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + session.role()))
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}