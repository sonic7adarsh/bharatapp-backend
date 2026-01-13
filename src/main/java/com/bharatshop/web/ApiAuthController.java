package com.bharatshop.web;

import com.bharatshop.domain.User;
import com.bharatshop.dto.LoginRequest;
import com.bharatshop.dto.RegisterRequest;
import com.bharatshop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {
    private static final Logger log = LoggerFactory.getLogger(ApiAuthController.class);
    private final AuthService authService;

    public ApiAuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestHeader("X-Tenant-Domain") String tenant,
                                      @Valid @RequestBody RegisterRequest req) {
        log.info("Register (tenant-aware): tenant={} email={}", tenant, req.getEmail());
        var session = authService.registerWithTenant(tenant, req.getName(), req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Register success (tenant-aware): userId={} tokenPresent=true", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    // MVP alias: signup -> same as register
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestHeader("X-Tenant-Domain") String tenant,
                                    @Valid @RequestBody RegisterRequest req) {
        log.info("Signup (tenant-aware): tenant={} email={}", tenant, req.getEmail());
        var session = authService.registerWithTenant(tenant, req.getName(), req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Signup success (tenant-aware): userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestHeader("X-Tenant-Domain") String tenant,
                                   @Valid @RequestBody LoginRequest req) {
        log.info("Login (tenant-aware): tenant={} email={}", tenant, req.getEmail());
        var session = authService.loginEmailWithTenant(tenant, req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Login success (tenant-aware): userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        log.info("Profile (tenant-aware) requested: tokenPresent={} ", token != null);
        var user = authService.getProfile(token);
        if (user == null) return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        log.info("Profile success: userId={}", user.getId());
        return ResponseEntity.ok(user);
    }
}