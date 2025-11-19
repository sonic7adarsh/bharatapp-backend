package com.bharatshop.service;

import com.bharatshop.domain.User;
import com.bharatshop.entity.UserEntity;
import com.bharatshop.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final Map<String, Session> sessionsByToken = new ConcurrentHashMap<>();
    private final Map<String, OtpInfo> otpsByPhone = new ConcurrentHashMap<>();
    private final com.bharatshop.security.JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       NotificationService notificationService,
                       com.bharatshop.security.JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    private static class OtpInfo {
        String otp;
        String otpId;
        long expiresAt;
    }

    public record Session(String token, String userId, String name, String role) {}

    public Session register(String name, String email, String password) {
        log.info("Registering user: name={}, email={}", name, email);
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.warn("Registration failed: email already registered: {}", email);
            throw new IllegalArgumentException("Email already registered");
        }
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setEmail(email);
        entity.setRole("USER");
        entity.setPassword(passwordEncoder.encode(password));
        userRepository.save(entity);
        var session = createSession(fromEntity(entity));
        log.info("User registered: id={}, token={}", entity.getId(), session.token());
        return session;
    }

    public Session registerSeller(String name, String email, String password) {
        log.info("Registering seller: name={}, email={}", name, email);
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.warn("Seller registration failed: email already registered: {}", email);
            throw new IllegalArgumentException("Email already registered");
        }
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setEmail(email);
        entity.setRole("SELLER");
        entity.setPassword(passwordEncoder.encode(password));
        userRepository.save(entity);
        var session = createSession(fromEntity(entity));
        log.info("Seller registered: id={}, token={}", entity.getId(), session.token());
        return session;
    }

    public Session loginEmail(String email, String password) {
        log.info("Login (storefront) attempt: email={}", email);
        UserEntity entity = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName(email.split("@")[0]);
            e.setEmail(email);
            e.setRole("USER");
            e.setPassword(passwordEncoder.encode(password));
            return userRepository.save(e);
        });
        if (entity.getPassword() != null && password != null && !passwordEncoder.matches(password, entity.getPassword())) {
            log.warn("Login (storefront) failed: invalid credentials for email={}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }
        var session = createSession(fromEntity(entity));
        log.info("Login (storefront) success: userId={} token={} role={}", entity.getId(), session.token(), entity.getRole());
        return session;
    }

    public Session loginSellerEmail(String email, String password) {
        log.info("Login (seller) attempt: email={}", email);
        UserEntity entity = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName(email.split("@")[0]);
            e.setEmail(email);
            e.setRole("SELLER");
            e.setPassword(passwordEncoder.encode(password));
            return userRepository.save(e);
        });
        if (entity.getPassword() != null && password != null && !passwordEncoder.matches(password, entity.getPassword())) {
            log.warn("Login (seller) failed: invalid credentials for email={}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }
        var session = createSession(fromEntity(entity));
        log.info("Login (seller) success: userId={} token={} role={} ", entity.getId(), session.token(), entity.getRole());
        return session;
    }

    /**
     * Flexible seller registration that either creates a seller or upgrades an existing user (by email or phone).
     * - If email is present and matches an existing user, upgrade role to SELLER and issue a session.
     * - Else if phone is present and matches an existing user, upgrade role to SELLER and issue a session.
     * - Else create a new SELLER using available fields (email or phone) and issue a session.
     * Requires at least one identifier (email or phone).
     */
    public Session registerOrUpgradeSeller(String name, String email, String phone, String password) {
        String trimmedEmail = email != null && !email.isBlank() ? email.trim() : null;
        String trimmedPhone = phone != null && !phone.isBlank() ? phone.trim() : null;
        String trimmedName = name != null ? name.trim() : null;

        if (trimmedEmail == null && trimmedPhone == null) {
            log.warn("Seller register/upgrade failed: neither email nor phone provided");
            throw new IllegalArgumentException("email or phone required");
        }

        // Try email first
        if (trimmedEmail != null) {
            Optional<UserEntity> byEmail = userRepository.findByEmail(trimmedEmail);
            if (byEmail.isPresent()) {
                UserEntity e = byEmail.get();
                if (!"SELLER".equalsIgnoreCase(e.getRole())) {
                    e.setRole("SELLER");
                    userRepository.save(e);
                    log.info("Upgraded existing user to SELLER via email: userId={}", e.getId());
                }
                return createSession(fromEntity(e));
            }
        }

        // Then phone
        if (trimmedPhone != null) {
            Optional<UserEntity> byPhone = userRepository.findByPhone(trimmedPhone);
            if (byPhone.isPresent()) {
                UserEntity e = byPhone.get();
                if (!"SELLER".equalsIgnoreCase(e.getRole())) {
                    e.setRole("SELLER");
                    userRepository.save(e);
                    log.info("Upgraded existing user to SELLER via phone: userId={}", e.getId());
                }
                return createSession(fromEntity(e));
            }
        }

        // Create new SELLER
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(trimmedName != null ? trimmedName : (trimmedEmail != null ? trimmedEmail.split("@")[0] : trimmedPhone));
        entity.setEmail(trimmedEmail);
        entity.setPhone(trimmedPhone);
        entity.setRole("SELLER");
        entity.setPassword(password != null && !password.isBlank() ? passwordEncoder.encode(password) : null);
        userRepository.save(entity);
        var session = createSession(fromEntity(entity));
        log.info("Created new SELLER: userId={} email={} phone={} tokenPresent=true", entity.getId(), entity.getEmail(), entity.getPhone());
        return session;
    }

    public Session loginPhone(String phone, String otp) {
        log.info("Login (phone) attempt: phone={}", phone);
        UserEntity entity = userRepository.findByPhone(phone).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName("User" + phone.substring(Math.max(0, phone.length()-4)));
            e.setPhone(phone);
            e.setRole("USER");
            return userRepository.save(e);
        });
        var session = createSession(fromEntity(entity));
        log.info("Login (phone) success: userId={} token={} role={} ", entity.getId(), session.token(), entity.getRole());
        return session;
    }

    public Map<String, Object> sendOtp(String phone) {
        log.info("Sending OTP to phone={}", phone);
        if (phone == null || phone.isBlank()) return Map.of("success", false, "error", "phone_required");
        OtpInfo info = new OtpInfo();
        info.otp = String.valueOf(100000 + new java.util.Random().nextInt(900000));
        info.otpId = UUID.randomUUID().toString();
        info.expiresAt = System.currentTimeMillis() + 120_000L;
        otpsByPhone.put(phone, info);
        log.info("OTP generated: otpId={} expiresAt={}", info.otpId, info.expiresAt);
        // Send notifications
        String smsMessage = "Your Bharatshop OTP is " + info.otp + ". Valid for 2 minutes.";
        notificationService.sendSms(phone, smsMessage);
        userRepository.findByPhone(phone).map(UserEntity::getEmail).filter(e -> e != null && !e.isBlank())
                .ifPresent(email -> notificationService.sendEmail(email, "Your Bharatshop OTP", smsMessage));
        // Note: For now, we expose the OTP in the response to facilitate testing.
        // Replace this with SMS integration and remove the 'otp' field in production.
        return Map.of("success", true, "otpId", info.otpId, "ttlSeconds", 120, "otp", info.otp);
    }

    public Session verifyOtp(String phone, String otp) {
        log.info("Verifying OTP for phone={}", phone);
        OtpInfo info = otpsByPhone.get(phone);
        if (info == null) {
            log.warn("OTP verification failed: no OTP for phone={}", phone);
            return null;
        }
        if (System.currentTimeMillis() > info.expiresAt) {
            log.warn("OTP verification failed: expired otpId={} for phone={}", info.otpId, phone);
            return null;
        }
        if (!info.otp.equals(otp)) {
            log.warn("OTP verification failed: mismatch for phone={}", phone);
            return null;
        }
        log.info("OTP verification success for phone={}", phone);
        return loginPhone(phone, otp);
    }

    public Map<String, Object> resendOtp(String phone, String otpId) {
        OtpInfo info = otpsByPhone.get(phone);
        if (info == null) {
            log.warn("OTP resend failed: no OTP found for phone={}", phone);
            return Map.of("success", false, "error", "otp_not_found");
        }
        if (!info.otpId.equals(otpId)) {
            log.warn("OTP resend failed: otpId mismatch for phone={}, expected={} provided={}", phone, info.otpId, otpId);
            return Map.of("success", false, "error", "otp_id_mismatch");
        }
        info.expiresAt = System.currentTimeMillis() + 120_000L;
        otpsByPhone.put(phone, info);
        // Re-send notifications
        String smsMessage = "Your Bharatshop OTP is " + info.otp + ". Valid for 2 minutes.";
        notificationService.sendSms(phone, smsMessage);
        userRepository.findByPhone(phone).map(UserEntity::getEmail).filter(e -> e != null && !e.isBlank())
                .ifPresent(email -> notificationService.sendEmail(email, "Your Bharatshop OTP", smsMessage));
        // Log OTP details server-side for testing convenience
        log.info("OTP resend: phone={} otpId={} otp={} ttlSeconds={}", phone, info.otpId, info.otp, 120);
        // Include OTP in response for test environments; remove in production if needed
        return Map.of("success", true, "otpId", info.otpId, "ttlSeconds", 120, "otp", info.otp);
    }

    public Session getSessionByToken(String token) {
        if (jwtService.isEnabled()) {
            var payload = jwtService.parse(token);
            if (payload == null) return null;
            return new Session(token, payload.userId(), payload.name(), payload.role());
        }
        var s = sessionsByToken.get(token);
        if (s == null) {
            log.info("Session lookup: token not found");
        }
        return s;
    }

    public User getProfile(String token) {
        if (token == null || token.isBlank()) return null;
        // Support both JWT-backed tokens and in-memory session tokens
        Session s = getSessionByToken(token);
        if (s == null) return null;
        return userRepository.findById(s.userId()).map(this::fromEntity).orElse(null);
    }

    private Session createSession(User user) {
        String token;
        if (jwtService.isEnabled()) {
            token = jwtService.generateToken(user.getId(), user.getName(), user.getRole());
        } else {
            token = UUID.randomUUID().toString();
            sessionsByToken.put(token, new Session(token, user.getId(), user.getName(), user.getRole()));
        }
        log.info("Session created: userId={} role={}", user.getId(), user.getRole());
        return new Session(token, user.getId(), user.getName(), user.getRole());
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) return "consumer";
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "SELLER" -> "seller";
            case "VENDOR" -> "vendor";
            case "MERCHANT", "PARTNER" -> "seller";
            default -> "consumer";
        };
    }

    private User fromEntity(UserEntity e) {
        return new User(e.getId(), e.getName(), e.getEmail(), e.getPhone(), normalizeRole(e.getRole()));
    }

    /**
     * Upgrade the user's role (e.g., after first store creation) without forcing re-login.
     * - Persists new role in DB (stored uppercase)
     * - Updates in-memory sessions (if JWT disabled) to reflect normalized role immediately
     */
    public User upgradeRoleForUser(String userId, String newRole) {
        if (userId == null || newRole == null || newRole.isBlank()) return null;
        String storeRole = newRole.trim().toUpperCase();
        Optional<UserEntity> opt = userRepository.findById(userId);
        if (opt.isEmpty()) return null;
        UserEntity e = opt.get();
        String current = e.getRole();
        String currentNorm = normalizeRole(current);
        String desiredNorm = normalizeRole(storeRole);
        // If already seller/vendor, skip
        if ("seller".equals(currentNorm) || "vendor".equals(currentNorm)) {
            return fromEntity(e);
        }
        e.setRole(storeRole);
        userRepository.save(e);
        // Update in-memory sessions so profile reflects change immediately when JWT is disabled
        if (!jwtService.isEnabled()) {
            String normalized = normalizeRole(storeRole);
            sessionsByToken.replaceAll((token, sess) -> {
                if (sess != null && userId.equals(sess.userId())) {
                    return new Session(sess.token(), sess.userId(), sess.name(), normalized);
                }
                return sess;
            });
        }
        log.info("User role upgraded: userId={} role={}", userId, storeRole);
        return fromEntity(e);
    }
}