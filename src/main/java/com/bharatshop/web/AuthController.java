package com.bharatshop.web;

import com.bharatshop.domain.User;
import com.bharatshop.dto.LoginRequest;
import com.bharatshop.dto.PhoneLoginRequest;
import com.bharatshop.dto.RegisterRequest;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/storefront/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) { this.authService = authService; }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        log.info("Register storefront user: email={}", req.getEmail());
        var session = authService.register(req.getName(), req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Storefront register success: userId={} tokenPresent=true", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginGeneric(@Valid @RequestBody LoginRequest req) {
        log.info("Login storefront (generic): email={}", req.getEmail());
        var session = authService.loginEmail(req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Storefront login success: userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/login/email")
    public ResponseEntity<?> loginEmail(@Valid @RequestBody LoginRequest req) {
        log.info("Login storefront (email): email={}", req.getEmail());
        var session = authService.loginEmail(req.getEmail(), req.getPassword());
        User user = authService.getProfile(session.token());
        log.info("Storefront login(email) success: userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @PostMapping("/login/phone")
    public ResponseEntity<?> loginPhone(@Valid @RequestBody PhoneLoginRequest req) {
        log.info("Login storefront (phone): phone={} otpPresent={} ", req.getPhone(), req.getOtp() != null);
        var session = authService.loginPhone(req.getPhone(), req.getOtp());
        User user = authService.getProfile(session.token());
        log.info("Storefront login(phone) success: userId={}", user.getId());
        return ResponseEntity.ok(Map.of("token", session.token(), "user", user));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) token = token.substring(7);
        log.info("Profile requested: tokenPresent={} ", token != null);
        var user = authService.getProfile(token);
        if (user == null) return ResponseEntity.status(401).body(Map.of("message", "Unauthorized"));
        log.info("Profile success: userId={}", user.getId());
        return ResponseEntity.ok(user);
    }

    @PostMapping("/otp/send")
    public ResponseEntity<?> sendOtp(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") == null ? null : body.get("phone").toString();
        log.info("OTP send requested: phone={}", phone);
        return ResponseEntity.ok(authService.sendOtp(phone));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") == null ? null : body.get("phone").toString();
        String otp = body.get("otp") == null ? null : body.get("otp").toString();
        log.info("OTP verify requested: phone={} otpPresent={}", phone, otp != null);
        var session = authService.verifyOtp(phone, otp);
        if (session == null) return ResponseEntity.status(400).body(Map.of("success", false));
        User user = authService.getProfile(session.token());
        log.info("OTP verify success: userId={} tokenPresent=true", user.getId());
        return ResponseEntity.ok(Map.of("success", true, "token", session.token(), "user", user));
    }

    @PostMapping("/otp/resend")
    public ResponseEntity<?> resendOtp(@RequestBody Map<String, Object> body) {
        String phone = body.get("phone") == null ? null : body.get("phone").toString();
        String otpId = body.get("otpId") == null ? null : body.get("otpId").toString();
        log.info("OTP resend requested: phone={} otpIdPresent={}", phone, otpId != null);
        return ResponseEntity.ok(authService.resendOtp(phone, otpId));
    }
}