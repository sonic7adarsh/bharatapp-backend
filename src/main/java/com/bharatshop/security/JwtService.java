package com.bharatshop.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class JwtService {
    private final boolean enabled;
    private final String secret;
    private final long expSeconds;
    private final String issuer;
    private final String audience;
    private final Algorithm algorithm;

    public record Payload(String userId, String name, String role, String tenantId, String activeRole, java.util.List<String> roles) {}

    public JwtService(
            @Value("${app.jwt.enabled:false}") boolean enabled,
            @Value("${app.jwt.secret:dev-insecure-secret-change}") String secret,
            @Value("${app.jwt.expSeconds:3600}") long expSeconds,
            @Value("${app.jwt.issuer:bharatshop}") String issuer,
            @Value("${app.jwt.audience:bharatshop-clients}") String audience
    ) {
        this.enabled = enabled;
        this.secret = secret;
        this.expSeconds = expSeconds;
        this.issuer = issuer;
        this.audience = audience;
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public boolean isEnabled() { return enabled; }

    public String generateToken(String userId, String name, String role, String tenantId, String activeRole) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userId)
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim("name", name)
                .withClaim("role", role == null ? "USER" : role)
                .withClaim("tenantId", tenantId)
                .withClaim("activeRole", activeRole)
                .withIssuedAt(java.util.Date.from(now))
                .withExpiresAt(java.util.Date.from(now.plusSeconds(expSeconds)))
                .sign(algorithm);
    }

    public String generateTokenWithRoles(String userId, String name, String role, String tenantId, String activeRole, java.util.List<String> roles) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userId)
                .withIssuer(issuer)
                .withAudience(audience)
                .withClaim("name", name)
                .withClaim("role", role == null ? "USER" : role)
                .withClaim("tenantId", tenantId)
                .withClaim("activeRole", activeRole)
                .withArrayClaim("roles", roles == null ? new String[]{} : roles.toArray(String[]::new))
                .withIssuedAt(java.util.Date.from(now))
                .withExpiresAt(java.util.Date.from(now.plusSeconds(expSeconds)))
                .sign(algorithm);
    }

    public Payload parse(String token) {
        try {
            DecodedJWT jwt = com.auth0.jwt.JWT.require(algorithm)
                    .withIssuer(issuer)
                    .acceptLeeway(1)
                    .build()
                    .verify(token);
            String userId = jwt.getSubject();
            String name = jwt.getClaim("name").asString();
            String role = jwt.getClaim("role").asString();
            String tenantId = jwt.getClaim("tenantId").asString();
            String activeRole = jwt.getClaim("activeRole").asString();
            java.util.List<String> roles = jwt.getClaim("roles").asList(String.class);
            return new Payload(userId, name, role, tenantId, activeRole, roles);
        } catch (Exception e) {
            return null;
        }
    }
}