package com.bharatshop.service;

import com.bharatshop.domain.User;
import com.bharatshop.entity.UserEntity;
import com.bharatshop.entity.UserRoleEntity;
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
    private final UserRoleService userRoleService;
    private final NotificationService notificationService;
    private final WhatsAppService whatsAppService;
    private final Map<String, Session> sessionsByToken = new ConcurrentHashMap<>();
    private final Map<String, OtpInfo> otpsByPhone = new ConcurrentHashMap<>();
    private final com.bharatshop.security.JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       UserRoleService userRoleService,
                       NotificationService notificationService,
                       WhatsAppService whatsAppService,
                       com.bharatshop.security.JwtService jwtService,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.notificationService = notificationService;
        this.whatsAppService = whatsAppService;
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
        // Stop using legacy role field; rely on user_roles
        entity.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(entity);
        
        // Initialize multi-role system
        userRoleService.initializeCustomerRole(entity.getId());
        
        var session = createSession(fromEntity(entity));
        log.info("User registered: id={}, token={}", entity.getId(), session.token());
        return session;
    }

    // Tenant-aware registration for admin/tenant scoped auth
    public Session registerWithTenant(String tenant, String name, String email, String password) {
        // tenant parameter ignored in local-first platform
        log.info("Registering user (tenant-ignored): email={}", email);
        Optional<UserEntity> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            log.warn("Registration failed: email already registered: {}", email);
            throw new IllegalArgumentException("Email already registered");
        }
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(name);
        entity.setEmail(email);
        // tenantId removed
        entity.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(entity);

        userRoleService.initializeCustomerRole(entity.getId());
        var session = createSession(fromEntity(entity));
        log.info("User registered: userId={} token={}", entity.getId(), session.token());
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
        // Stop using legacy role field; rely on user_roles
        entity.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(entity);
        
        // Initialize multi-role system with SELLER role
        userRoleService.addRoleToUser(entity.getId(), "CUSTOMER");
        userRoleService.addRoleToUser(entity.getId(), "SELLER");
        userRoleService.switchUserRole(entity.getId(), "SELLER"); // Make SELLER active
        
        var session = createSession(fromEntity(entity));
        log.info("Seller registered: id={}, token={}", entity.getId(), session.token());
        return session;
    }

    public Session loginEmail(String email, String password) {
        log.info("Login (storefront) attempt: email={}", email);
        // Tenant context removed
        UserEntity entity = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName(email.split("@")[0]);
            e.setEmail(email);
            // tenantId removed
            e.setPasswordHash(passwordEncoder.encode(password));
            UserEntity saved = userRepository.save(e);
            // Ensure roles exist
            userRoleService.initializeCustomerRole(saved.getId());
            return saved;
        });
        if (entity.getPasswordHash() != null && password != null && !passwordEncoder.matches(password, entity.getPasswordHash())) {
            log.warn("Login (storefront) failed: invalid credentials for email={}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }
        var session = createSession(fromEntity(entity));
        log.info("Login (storefront) success: userId={} token={} activeRole={}", entity.getId(), session.token(), session.role());
        return session;
    }

    // Tenant-aware email login
    public Session loginEmailWithTenant(String tenant, String email, String password) {
        // tenant parameter ignored
        log.info("Login (tenant-ignored) attempt: email={}", email);
        UserEntity entity = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName(email.split("@")[0]);
            e.setEmail(email);
            // tenantId removed
            e.setPasswordHash(passwordEncoder.encode(password));
            UserEntity saved = userRepository.save(e);
            userRoleService.initializeCustomerRole(saved.getId());
            return saved;
        });
        if (entity.getPasswordHash() != null && password != null && !passwordEncoder.matches(password, entity.getPasswordHash())) {
            log.warn("Login failed: invalid credentials for email={}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }
        var session = createSession(fromEntity(entity));
        log.info("Login success: userId={} token={} activeRole={}", entity.getId(), session.token(), session.role());
        return session;
    }

    public Session loginSellerEmail(String email, String password) {
        log.info("Login (seller) attempt: email={}", email);
        UserEntity entity = userRepository.findByEmail(email).orElseGet(() -> {
            UserEntity e = new UserEntity();
            e.setId(UUID.randomUUID().toString());
            e.setName(email.split("@")[0]);
            e.setEmail(email);
            e.setPasswordHash(passwordEncoder.encode(password));
            UserEntity saved = userRepository.save(e);
            userRoleService.addRoleToUser(saved.getId(), "CUSTOMER");
            userRoleService.addRoleToUser(saved.getId(), "SELLER");
            userRoleService.switchUserRole(saved.getId(), "SELLER");
            return saved;
        });
        if (entity.getPasswordHash() != null && password != null && !passwordEncoder.matches(password, entity.getPasswordHash())) {
            log.warn("Login (seller) failed: invalid credentials for email={}", email);
            throw new IllegalArgumentException("Invalid credentials");
        }
        var session = createSession(fromEntity(entity));
        log.info("Login (seller) success: userId={} token={} activeRole={} ", entity.getId(), session.token(), session.role());
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
                userRoleService.addRoleToUser(e.getId(), "SELLER");
                userRoleService.switchUserRole(e.getId(), "SELLER");
                log.info("Upgraded existing user to SELLER via email: userId={}", e.getId());
                return createSession(fromEntity(e));
            }
        }

        // Then phone
        if (trimmedPhone != null) {
            Optional<UserEntity> byPhone = userRepository.findByPhone(trimmedPhone);
            if (byPhone.isPresent()) {
                UserEntity e = byPhone.get();
                userRoleService.addRoleToUser(e.getId(), "SELLER");
                userRoleService.switchUserRole(e.getId(), "SELLER");
                log.info("Upgraded existing user to SELLER via phone: userId={}", e.getId());
                return createSession(fromEntity(e));
            }
        }

        // Create new SELLER
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setName(trimmedName != null ? trimmedName : (trimmedEmail != null ? trimmedEmail.split("@")[0] : trimmedPhone));
        entity.setEmail(trimmedEmail);
        entity.setPhone(trimmedPhone);
        // Stop using legacy role field; rely on user_roles
        entity.setPasswordHash(password != null && !password.isBlank() ? passwordEncoder.encode(password) : null);
        userRepository.save(entity);
        userRoleService.addRoleToUser(entity.getId(), "CUSTOMER");
        userRoleService.addRoleToUser(entity.getId(), "SELLER");
        userRoleService.switchUserRole(entity.getId(), "SELLER");
        var session = createSession(fromEntity(entity));
        log.info("Created new SELLER: userId={} email={} phone={} tokenPresent=true", entity.getId(), entity.getEmail(), entity.getPhone());
        return session;
    }

    public Session loginPhone(String phone, String otp) {
        log.info("Login (phone) attempt: phone={}", phone);
        // Tenant context removed
        java.util.Optional<UserEntity> userOpt = userRepository.findByPhone(phone);
        UserEntity entity = userOpt.orElseGet(() -> createUserWithPhone(phone));
        var session = createSession(fromEntity(entity));
        log.info("Login (phone) success: userId={} token={} activeRole={} ", entity.getId(), session.token(), session.role());
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
        // Send OTP via WhatsApp
        try {
            // notificationService.sendOtp(phone, info.otp); // Legacy
            whatsAppService.sendOtp(phone, info.otp); // Direct call to use updated template logic
        } catch (Exception e) {
            log.error("Failed to send OTP via WhatsApp to {}", phone, e);
        }
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

    /**
     * Role-aware OTP verification. Requires frontend to provide desired loginRole (e.g., CUSTOMER | SELLER | RIDER).
     * Validates OTP and role, ensures the user has the requested role, and issues a JWT with activeRole=loginRole.
     * If isRegistration is true, it will assign the requested role to the user if not already present.
     */
    public Session verifyOtpWithRole(String phone, String otp, String loginRole, boolean isRegistration) {
        log.info("Verifying OTP (role-aware) for phone={} role={} isRegistration={}", phone, loginRole, isRegistration);
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

        // Validate tenant presence - REMOVED

        // Validate and canonicalize requested login role
        if (loginRole == null || loginRole.isBlank()) {
            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "ROLE_REQUIRED", "loginRole is required");
        }
        String requested = canonicalRole(loginRole);
        java.util.List<String> allowed = java.util.List.of("CUSTOMER", "SELLER", "RIDER");
        if (!allowed.contains(requested)) {
            throw new com.bharatshop.error.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "ROLE_INVALID", "Unsupported login role");
        }

        // Get or create user by phone (initialize CUSTOMER on creation)
        UserEntity entity = getOrCreateUserByPhone(phone);
        
        // If this is a registration flow or implicit role assignment, add the role
        if (isRegistration) {
             userRoleService.addRoleToUser(entity.getId(), requested);
        }

        // Check that the user actually has the requested role
        java.util.List<String> userRoles = userRoleService.getUserRoles(entity.getId());
        if (userRoles == null || userRoles.isEmpty() || userRoles.stream().noneMatch(r -> requested.equalsIgnoreCase(canonicalRole(r)))) {
            throw new com.bharatshop.error.ApiException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "ROLE_NOT_ASSIGNED",
                    "User does not have requested role",
                    java.util.Map.of("requestedRole", requested)
            );
        }

        // Switch active role to requested role
        userRoleService.switchUserRole(entity.getId(), requested);

        // Issue a token with activeRole=requested and roles from userRoles
        var session = createSession(fromEntity(entity), requested);
        log.info("Login (OTP role-aware) success: userId={} activeRole={} tokenPresent=true", entity.getId(), requested);
        return session;
    }

    // Legacy overload for backward compatibility if needed, though mostly unused now
    public Session verifyOtpWithRole(String phone, String otp, String loginRole) {
        return verifyOtpWithRole(phone, otp, loginRole, false);
    }

    // Helper: get or create user by phone without creating session
    private UserEntity getOrCreateUserByPhone(String phone) {
        // Tenant context removed
        Optional<UserEntity> userOpt = userRepository.findByPhone(phone);
        return userOpt.orElseGet(() -> createUserWithPhone(phone));
    }

    private UserEntity createUserWithPhone(String phone) {
        UserEntity e = new UserEntity();
        e.setId(UUID.randomUUID().toString());
        e.setName("User" + (phone == null ? "" : phone.substring(Math.max(0, phone.length()-4))));
        e.setPhone(phone);
        // tenantId removed
        UserEntity saved = userRepository.save(e);
        userRoleService.initializeCustomerRole(saved.getId());
        return saved;
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
        // Log OTP details server-side for testing convenience
        log.info("OTP resend: phone={} otpId={} otp={} ttlSeconds={}", phone, info.otpId, info.otp, 120);
        // Include OTP in response for test environments; remove in production if needed
        return Map.of("success", true, "otpId", info.otpId, "ttlSeconds", 120, "otp", info.otp);
    }

    public Session getSessionByToken(String token) {
        if (jwtService.isEnabled()) {
            var payload = jwtService.parse(token);
            if (payload == null) return null;
            String role = (payload.activeRole() != null && !payload.activeRole().isBlank()) ? payload.activeRole() : payload.role();
            return new Session(token, payload.userId(), payload.name(), role);
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
        User user = userRepository.findById(s.userId()).map(this::fromEntity).orElse(null);
        // Ensure the returned user object reflects the session's active role
        if (user != null && s.role() != null) {
            user.setRole(s.role());
        }
        return user;
    }

    private Session createSession(User user) {
        String token;
        // Determine active role from user_roles
        String activeRole = userRoleService.getActiveRole(user.getId())
                .map(UserRoleEntity::getRole)
                .orElseGet(() -> {
                    // Ensure at least CUSTOMER; then read it
                    userRoleService.initializeCustomerRole(user.getId());
                    return userRoleService.getActiveRole(user.getId())
                            .map(UserRoleEntity::getRole)
                            .orElse("CUSTOMER");
                });
        // Collect allowed roles for JWT roles claim
        java.util.List<String> roles = userRoleService.getUserRoles(user.getId());
        if (roles == null || roles.isEmpty()) {
            roles = java.util.List.of("CUSTOMER");
        }
        if (jwtService.isEnabled()) {
            token = jwtService.generateTokenWithRoles(
                    user.getId(),
                    user.getName(),
                    activeRole,
                    activeRole,
                    roles
            );
        } else {
            token = UUID.randomUUID().toString();
            sessionsByToken.put(token, new Session(token, user.getId(), user.getName(), activeRole));
        }
        log.info("Session created: userId={} activeRole={}", user.getId(), activeRole);
        return new Session(token, user.getId(), user.getName(), activeRole);
    }

    // Overload: create session with explicit activeRole override
    private Session createSession(User user, String activeRoleOverride) {
        String token;
        String activeRole = canonicalRole(activeRoleOverride);
        // Collect allowed roles for JWT roles claim
        java.util.List<String> roles = userRoleService.getUserRoles(user.getId());
        if (roles == null || roles.isEmpty()) {
            roles = java.util.List.of("CUSTOMER");
        }
        if (jwtService.isEnabled()) {
            token = jwtService.generateTokenWithRoles(
                    user.getId(),
                    user.getName(),
                    activeRole,
                    activeRole,
                    roles
            );
        } else {
            token = UUID.randomUUID().toString();
            sessionsByToken.put(token, new Session(token, user.getId(), user.getName(), activeRole));
        }
        log.info("Session created (override): userId={} activeRole={}", user.getId(), activeRole);
        return new Session(token, user.getId(), user.getName(), activeRole);
    }

    private String canonicalRole(String role) {
        if (role == null || role.isBlank()) return "CUSTOMER";
        String r = role.trim().toUpperCase();
        return switch (r) {
            case "SELLER", "MERCHANT", "PARTNER" -> "SELLER";
            case "VENDOR" -> "VENDOR";
            case "ADMIN" -> "ADMIN";
            case "RIDER" -> "RIDER";
            default -> "CUSTOMER";
        };
    }

    private User fromEntity(UserEntity e) {
        // Prefer active role from user_roles; fallback to CUSTOMER
        String activeRole = userRoleService.getActiveRole(e.getId())
                .map(UserRoleEntity::getRole)
                .orElse("CUSTOMER");
        return new User(e.getId(), e.getName(), e.getEmail(), e.getPhone(), canonicalRole(activeRole));
    }

    /**
     * Upgrade the user's role (e.g., after first store creation) without forcing re-login.
     * - Persists new role in DB (stored uppercase)
     * - Updates in-memory sessions (if JWT disabled) to reflect normalized role immediately
     */
    public User upgradeRoleForUser(String userId, String newRole) {
        if (userId == null || newRole == null || newRole.isBlank()) return null;
        String storeRole = canonicalRole(newRole);
        Optional<UserEntity> opt = userRepository.findById(userId);
        if (opt.isEmpty()) return null;
        UserEntity e = opt.get();
        // Activate SELLER/VENDOR via user_roles
        userRoleService.addRoleToUser(userId, storeRole);
        userRoleService.switchUserRole(userId, storeRole);
        // Update in-memory sessions so profile reflects change immediately when JWT is disabled
        if (!jwtService.isEnabled()) {
            sessionsByToken.replaceAll((token, sess) -> {
                if (sess != null && userId.equals(sess.userId())) {
                    return new Session(sess.token(), sess.userId(), sess.name(), storeRole);
                }
                return sess;
            });
        }
        log.info("User role upgraded via user_roles: userId={} activeRole={}", userId, storeRole);
        return fromEntity(e);
    }
}