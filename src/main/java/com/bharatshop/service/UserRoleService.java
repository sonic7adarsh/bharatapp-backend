package com.bharatshop.service;

import com.bharatshop.entity.UserEntity;
import com.bharatshop.entity.UserRoleEntity;
import com.bharatshop.repository.UserRepository;
import com.bharatshop.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserRoleService {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    
    public UserRoleService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }
    
    @Transactional
    public UserRoleEntity addRoleToUser(String userId, String role) {
        // Check if role already exists for user
        Optional<UserRoleEntity> existingRole = userRoleRepository.findByUserIdAndRole(userId, role);
        if (existingRole.isPresent()) {
            return existingRole.get();
        }
        
        // Create new role
        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setId("ur_" + UUID.randomUUID().toString());
        userRole.setUserId(userId);
        userRole.setRole(role);
        userRole.setIsActive(false); // New roles are inactive by default
        
        return userRoleRepository.save(userRole);
    }
    
    @Transactional
    public UserRoleEntity switchUserRole(String userId, String targetRole) {
        // Get user
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if user has the target role
        UserRoleEntity targetUserRole = userRoleRepository.findByUserIdAndRole(userId, targetRole)
            .orElseThrow(() -> new RuntimeException("User does not have role: " + targetRole));
        
        // Deactivate current active role
        Optional<UserRoleEntity> currentActiveRole = userRoleRepository.findByUserIdAndIsActiveTrue(userId);
        currentActiveRole.ifPresent(role -> {
            role.setIsActive(false);
            userRoleRepository.save(role);
        });
        
        // Activate target role
        targetUserRole.setIsActive(true);
        user.setActiveRoleId(targetUserRole.getId());
        user.setRole(targetRole); // Keep legacy field updated
        
        userRoleRepository.save(targetUserRole);
        userRepository.save(user);
        
        return targetUserRole;
    }
    
    public List<String> getUserRoles(String userId) {
        return userRoleRepository.findRolesByUserId(userId);
    }
    
    public Optional<UserRoleEntity> getActiveRole(String userId) {
        return userRoleRepository.findActiveRoleByUserId(userId);
    }
    
    public List<UserRoleEntity> getUserRoleEntities(String userId) {
        return userRoleRepository.findByUserId(userId);
    }
    
    @Transactional
    public void initializeCustomerRole(String userId) {
        addRoleToUser(userId, "CUSTOMER");
        
        // If this is the first role, make it active
        List<UserRoleEntity> userRoles = userRoleRepository.findByUserId(userId);
        if (userRoles.size() == 1) {
            switchUserRole(userId, "CUSTOMER");
        }
    }
    
    @Transactional
    public void upgradeToSeller(String userId) {
        addRoleToUser(userId, "SELLER");
    }
}