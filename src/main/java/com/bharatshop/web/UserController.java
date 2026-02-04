package com.bharatshop.web;

import com.bharatshop.dto.UserAddressDto;
import com.bharatshop.dto.UserProfileDto;
import com.bharatshop.entity.UserAddressEntity;
import com.bharatshop.entity.UserEntity;
import com.bharatshop.repository.UserAddressRepository;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.security.UserPrincipal;
import com.bharatshop.error.NotFoundException;
import com.bharatshop.error.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.bharatshop.service.UserRoleService;
import com.bharatshop.security.JwtService;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final UserRoleService userRoleService;
    private final JwtService jwtService;

    public UserController(UserRepository userRepository, 
                          UserAddressRepository userAddressRepository,
                          UserRoleService userRoleService,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.userAddressRepository = userAddressRepository;
        this.userRoleService = userRoleService;
        this.jwtService = jwtService;
    }

    @PostMapping("/switch-role")
    public ResponseEntity<Map<String, Object>> switchRole(@RequestBody Map<String, String> request) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");
        
        String targetRole = request.get("role");
        if (targetRole == null || targetRole.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Role is required"));
        }

        try {
            // Switch role
            com.bharatshop.entity.UserRoleEntity newRole = userRoleService.switchUserRole(up.getUserId(), targetRole);
            
            // Generate new token
            List<String> allowedRoles = userRoleService.getUserRoles(up.getUserId());
            if (allowedRoles == null || allowedRoles.isEmpty()) {
                allowedRoles = List.of("CUSTOMER");
            }
            String newToken = jwtService.generateTokenWithRoles(
                up.getUserId(),
                up.getName(),
                targetRole,
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
             throw new BadRequestException("Failed to switch role: " + e.getMessage());
        }
    }

    // Profile Management
    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile() {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        UserEntity user = userRepository.findById(up.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        UserProfileDto dto = new UserProfileDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAlternatePhone(user.getAlternatePhone());
        
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileDto> updateProfile(@RequestBody UserProfileDto updates) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        UserEntity user = userRepository.findById(up.getUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (updates.getName() != null) user.setName(updates.getName());
        if (updates.getAlternatePhone() != null) user.setAlternatePhone(updates.getAlternatePhone());
        // Phone and Email updates might require verification, so skipping for now or allow if not null
        // Assuming phone/email update via specific flows, but if provided here we can update
        // user.setPhone(updates.getPhone()); 

        userRepository.save(user);

        // Return updated profile
        UserProfileDto dto = new UserProfileDto();
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAlternatePhone(user.getAlternatePhone());
        
        return ResponseEntity.ok(dto);
    }

    // Address Management
    @GetMapping("/addresses")
    public ResponseEntity<List<UserAddressDto>> getAddresses() {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        List<UserAddressEntity> entities = userAddressRepository.findByUserId(up.getUserId());
        List<UserAddressDto> dtos = entities.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/addresses")
    public ResponseEntity<UserAddressDto> createAddress(@jakarta.validation.Valid @RequestBody UserAddressDto req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        UserAddressEntity entity = new UserAddressEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUserId(up.getUserId());
        updateEntityFromDto(entity, req);

        // If this is the first address, make it default
        if (userAddressRepository.findByUserId(up.getUserId()).isEmpty()) {
            entity.setIsDefault(true);
        } else if (Boolean.TRUE.equals(req.getIsDefault())) {
            // Unset other defaults
            unsetOtherDefaults(up.getUserId());
            entity.setIsDefault(true);
        }

        userAddressRepository.save(entity);
        return ResponseEntity.ok(toDto(entity));
    }

    @PutMapping("/addresses/{id}")
    public ResponseEntity<UserAddressDto> updateAddress(@PathVariable String id, @RequestBody UserAddressDto req) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        UserAddressEntity entity = userAddressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found"));
        
        if (!entity.getUserId().equals(up.getUserId())) {
            throw new NotFoundException("Address not found"); // Hide existence
        }

        updateEntityFromDto(entity, req);

        if (Boolean.TRUE.equals(req.getIsDefault())) {
            unsetOtherDefaults(up.getUserId());
            entity.setIsDefault(true);
        }

        userAddressRepository.save(entity);
        return ResponseEntity.ok(toDto(entity));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable String id) {
        UserPrincipal up = UserPrincipal.current();
        if (up == null) throw new com.bharatshop.error.UnauthorizedException("Unauthorized");

        UserAddressEntity entity = userAddressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Address not found"));

        if (!entity.getUserId().equals(up.getUserId())) {
            throw new NotFoundException("Address not found");
        }

        userAddressRepository.delete(entity);
        return ResponseEntity.ok().build();
    }

    private void unsetOtherDefaults(String userId) {
        List<UserAddressEntity> addresses = userAddressRepository.findByUserId(userId);
        for (UserAddressEntity addr : addresses) {
            if (Boolean.TRUE.equals(addr.getIsDefault())) {
                addr.setIsDefault(false);
                userAddressRepository.save(addr);
            }
        }
    }

    private void updateEntityFromDto(UserAddressEntity e, UserAddressDto d) {
        if (d.getName() != null) e.setName(d.getName());
        if (d.getPhone() != null) e.setPhone(d.getPhone());
        if (d.getAlternatePhone() != null) e.setAlternatePhone(d.getAlternatePhone());
        if (d.getLine1() != null) e.setLine1(d.getLine1());
        if (d.getLine2() != null) e.setLine2(d.getLine2());
        if (d.getCity() != null) e.setCity(d.getCity());
        if (d.getState() != null) e.setState(d.getState());
        if (d.getZip() != null) e.setZip(d.getZip());
        if (d.getType() != null) e.setType(d.getType());
        // isDefault handled separately in controller logic
    }

    private UserAddressDto toDto(UserAddressEntity e) {
        UserAddressDto d = new UserAddressDto();
        d.setId(e.getId());
        d.setName(e.getName());
        d.setPhone(e.getPhone());
        d.setAlternatePhone(e.getAlternatePhone());
        d.setLine1(e.getLine1());
        d.setLine2(e.getLine2());
        d.setCity(e.getCity());
        d.setState(e.getState());
        d.setZip(e.getZip());
        d.setType(e.getType());
        d.setIsDefault(e.getIsDefault());
        return d;
    }
}
