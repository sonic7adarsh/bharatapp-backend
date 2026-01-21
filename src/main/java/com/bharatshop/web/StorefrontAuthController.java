package com.bharatshop.web;

import com.bharatshop.domain.User;
import com.bharatshop.dto.LoginRequest;
import com.bharatshop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront/auth")
public class StorefrontAuthController {
    private static final Logger log = LoggerFactory.getLogger(StorefrontAuthController.class);
    private final AuthService authService;

    public StorefrontAuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.info("Login storefront customer: email={}", req.getEmail());
        var session = authService.loginEmail(req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Storefront login success: userId={} role={}", user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String name = body.get("name") == null ? null : body.get("name").toString();
        String email = body.get("email") == null ? null : body.get("email").toString();
        String password = body.get("password") == null ? null : body.get("password").toString();
        log.info("Register storefront customer: email={} namePresent={}", email, name != null);
        var session = authService.register(name != null ? name : (email != null ? email.split("@")[0] : "User"), email, password);
        User user = authService.getProfile(session.token());
        log.info("Storefront register success: userId={} role={}", user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }
}