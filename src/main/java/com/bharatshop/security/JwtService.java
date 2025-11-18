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
    private final Algorithm algorithm;

    public record Payload(String userId, String name, String role) {}

    public JwtService(
            @Value("${app.jwt.enabled:false}") boolean enabled,
            @Value("${app.jwt.secret:dev-insecure-secret-change}") String secret,
            @Value("${app.jwt.expSeconds:3600}") long expSeconds
    ) {
        this.enabled = enabled;
        this.secret = secret;
        this.expSeconds = expSeconds;
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public boolean isEnabled() { return enabled; }

    public String generateToken(String userId, String name, String role) {
        Instant now = Instant.now();
        return JWT.create()
                .withSubject(userId)
                .withClaim("name", name)
                .withClaim("role", role == null ? "USER" : role)
                .withIssuedAt(java.util.Date.from(now))
                .withExpiresAt(java.util.Date.from(now.plusSeconds(expSeconds)))
                .sign(algorithm);
    }

    public Payload parse(String token) {
        try {
            DecodedJWT jwt = com.auth0.jwt.JWT.require(algorithm).build().verify(token);
            String userId = jwt.getSubject();
            String name = jwt.getClaim("name").asString();
            String role = jwt.getClaim("role").asString();
            return new Payload(userId, name, role);
        } catch (Exception e) {
            return null;
        }
    }
}