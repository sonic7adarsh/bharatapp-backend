-- Create user_roles table for multi-role support
CREATE TABLE user_roles (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    -- Foreign key constraint
    CONSTRAINT fk_user_roles_user 
        FOREIGN KEY (user_id) REFERENCES users(id) 
        ON DELETE CASCADE,
    
    -- Unique constraint to prevent duplicate roles per user
    CONSTRAINT uk_user_role 
        UNIQUE (user_id, role),
    
    -- Ensure only one active role per user
    CONSTRAINT uk_user_active_role 
        UNIQUE (user_id, is_active) 
        WHERE is_active = TRUE
);

-- Create indexes for performance
CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);
CREATE INDEX idx_user_roles_is_active ON user_roles(is_active);
CREATE INDEX idx_user_roles_user_active ON user_roles(user_id, is_active);

-- Migrate existing users to user_roles table
INSERT INTO user_roles (id, user_id, role, is_active, created_at, updated_at)
SELECT 
    CONCAT('ur_', id) as id,
    id as user_id,
    role as role,
    TRUE as is_active,
    created_at,
    updated_at
FROM users
WHERE role IS NOT NULL AND role != '';

-- Add active_role_id column to users table for quick access
ALTER TABLE users 
ADD COLUMN active_role_id VARCHAR(255) NULL,
ADD CONSTRAINT fk_users_active_role 
    FOREIGN KEY (active_role_id) REFERENCES user_roles(id) 
    ON DELETE SET NULL;

-- Update active_role_id for existing users
UPDATE users u
JOIN user_roles ur ON u.id = ur.user_id AND ur.is_active = TRUE
SET u.active_role_id = ur.id;