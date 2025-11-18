package com.bharatshop.web;

import com.bharatshop.domain.User;
import com.bharatshop.dto.LoginRequest;
import com.bharatshop.dto.SellerRegisterRequest;
import com.bharatshop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/seller")
public class SellerAuthController {
    private static final Logger log = LoggerFactory.getLogger(SellerAuthController.class);
    private final AuthService authService;

    public SellerAuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody SellerRegisterRequest req) {
        log.info("Register/upgrade seller: email={} phone={}", req.getEmail(), req.getPhone());
        var session = authService.registerOrUpgradeSeller(req.getName(), req.getEmail(), req.getPhone(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Seller register/upgrade success: userId={} role={}", user.getId(), user.getRole());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        log.info("Login seller: email={}", req.getEmail());
        var session = authService.loginSellerEmail(req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Seller login success: userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }
}