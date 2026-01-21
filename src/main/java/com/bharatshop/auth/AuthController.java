package com.bharatshop.auth;

import com.bharatshop.domain.User;
import com.bharatshop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.bharatshop.error.BadRequestException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    // Canonical OTP endpoints only for MVP v1

    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") == null ? null : body.get("phone").toString();
        log.info("OTP send requested: phone={}", phone);
        return ResponseEntity.ok(authService.sendOtp(phone));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") == null ? null : body.get("phone").toString();
        String otp = body.get("otp") == null ? null : body.get("otp").toString();
        log.info("OTP verify requested: phone={} otpPresent={}", phone, otp != null);
        var session = authService.verifyOtp(phone, otp);
        if (session == null) throw new BadRequestException("Invalid OTP");
        User user = authService.getProfile(session.token());
        log.info("OTP verify success: userId={} tokenPresent=true", user.getId());
        return ResponseEntity.ok(Map.of("success", true, "token", session.token(), "user", user));
    }
}