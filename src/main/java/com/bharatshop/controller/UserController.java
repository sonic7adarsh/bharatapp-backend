package com.bharatshop.controller;

import com.bharatshop.entity.UserEntity;
import com.bharatshop.entity.UserRoleEntity;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.security.JwtService;
import com.bharatshop.service.UserRoleService;
import com.bharatshop.service.AuthService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
public class UserController {
    
    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final JwtService jwtService;
    private final AuthService authService;
    
    public UserController(UserRepository userRepository, 
                         UserRoleService userRoleService,
                         JwtService jwtService,
                         AuthService authService) {
        this.userRepository = userRepository;
        this.userRoleService = userRoleService;
        this.jwtService = jwtService;
        this.authService = authService;
    }
    
    @PostMapping("/switch-role")
    public ResponseEntity<Map<String, Object>> switchRole(@RequestHeader("Authorization") String authHeader,
                                                           @RequestBody Map<String, String> request) {
        try {
            String token = extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
            }
            
            JwtService.Payload payload = jwtService.parse(token);
            if (payload == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
            }
            
            String targetRole = request.get("role");
            if (targetRole == null || targetRole.isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Role is required"));
            }
            
            // Switch role
            UserRoleEntity newRole = userRoleService.switchUserRole(payload.userId(), targetRole);
            
            // Generate new token with updated role and full allowed roles
            java.util.List<String> allowedRoles = userRoleService.getUserRoles(payload.userId());
            if (allowedRoles == null || allowedRoles.isEmpty()) {
                allowedRoles = java.util.List.of("CUSTOMER");
            }
            String newToken = jwtService.generateTokenWithRoles(
                payload.userId(),
                payload.name(),
                targetRole,
                payload.tenantId(),
                targetRole,
                allowedRoles
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", newToken);
            response.put("active_role", targetRole);
            response.put("allowed_roles", allowedRoles);
            response.put("message", "Role switched successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to switch role: " + e.getMessage()));
        }
    }
    
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = extractToken(authHeader);
            if (token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
            }
            
            JwtService.Payload payload = jwtService.parse(token);
            if (payload == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid token"));
            }
            
            // Get user details (tenant-scoped)
            UserEntity user = userRepository.findByIdAndTenantId(payload.userId(), payload.tenantId())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get user roles - ensure we always return valid roles
            List<UserRoleEntity> userRoles = userRoleService.getUserRoleEntities(payload.userId());
            List<String> allowedRoles;
            
            if (userRoles.isEmpty()) {
                // Ensure at least CUSTOMER role exists and is active
                userRoleService.initializeCustomerRole(payload.userId());
                allowedRoles = List.of("CUSTOMER");
            } else {
                allowedRoles = userRoles.stream()
                    .map(UserRoleEntity::getRole)
                    .collect(Collectors.toList());
            }
            
            // Get active role - ensure consistency
            String activeRole = payload.activeRole();
            if (activeRole == null || activeRole.isBlank()) {
                // If still no active role, use the first available role
                activeRole = allowedRoles.isEmpty() ? "CUSTOMER" : allowedRoles.get(0);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("name", user.getName());
            response.put("email", user.getEmail());
            response.put("phone", user.getPhone());
            response.put("active_role", activeRole);
            response.put("allowed_roles", allowedRoles);
            response.put("tenant_id", payload.tenantId());
            response.put("created_at", user.getCreatedAt());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get user info: " + e.getMessage()));
        }
    }

    @PostMapping("/roles/assign")
    public ResponseEntity<Map<String, Object>> assignRole(@RequestHeader("Authorization") String authHeader,
                                                          @RequestBody Map<String, String> request) {
        String token = extractToken(authHeader);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }
        JwtService.Payload payload = jwtService.parse(token);
        if (payload == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "Invalid token"));
        }

        String role = request.get("role");
        if (role == null || role.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }
        String normalized = role.trim().toUpperCase();
        if (!("SELLER".equals(normalized) || "RIDER".equals(normalized))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only SELLER or RIDER can be assigned"));
        }

        // Assign role to current user
        userRoleService.addRoleToUser(payload.userId(), normalized);
        java.util.List<String> allowedRoles = userRoleService.getUserRoles(payload.userId());
        if (allowedRoles == null || allowedRoles.isEmpty()) {
            allowedRoles = java.util.List.of("CUSTOMER");
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "assigned_role", normalized,
                "allowed_roles", allowedRoles
        ));
    }
    
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }
}