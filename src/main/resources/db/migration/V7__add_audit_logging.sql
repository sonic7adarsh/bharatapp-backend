-- Audit Logging System for MVP
-- Comprehensive audit trail for compliance and debugging

CREATE TABLE audit_logs (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(255),
    user_email VARCHAR(255),
    user_role VARCHAR(100),
    action VARCHAR(255) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    action_status VARCHAR(50) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    request_id VARCHAR(255),
    session_id VARCHAR(255),
    before_state TEXT,
    after_state TEXT,
    changes TEXT,
    error_message TEXT,
    execution_time_ms BIGINT,
    additional_data TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_audit_tenant (tenant_id),
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_action (action),
    INDEX idx_audit_status (action_status),
    INDEX idx_audit_resource (resource_type, resource_id),
    INDEX idx_audit_created (created_at),
    INDEX idx_audit_ip (ip_address),
    INDEX idx_audit_session (session_id),
    INDEX idx_audit_request (request_id)
);

-- Partitioning for better performance (optional for large scale)
-- This would require MySQL 8.0+ and additional setup

-- Create indexes for common query patterns
CREATE INDEX idx_audit_composite_user_time ON audit_logs (tenant_id, user_id, created_at);
CREATE INDEX idx_audit_composite_action_time ON audit_logs (tenant_id, action, created_at);
CREATE INDEX idx_audit_composite_resource_time ON audit_logs (tenant_id, resource_type, resource_id, created_at);
CREATE INDEX idx_audit_composite_status_time ON audit_logs (tenant_id, action_status, created_at);

-- Create index for security events
CREATE INDEX idx_audit_security_events ON audit_logs (tenant_id, action, created_at) 
WHERE action IN ('SECURITY_VIOLATION', 'RATE_LIMIT_EXCEEDED', 'UNAUTHORIZED_ACCESS');

-- Create index for performance tracking
CREATE INDEX idx_audit_performance ON audit_logs (tenant_id, execution_time_ms, created_at) 
WHERE execution_time_ms > 5000;

-- Insert sample audit log entries for testing
INSERT INTO audit_logs (id, tenant_id, user_id, user_email, user_role, action, resource_type, resource_id, action_status, ip_address, user_agent, request_id, session_id, additional_data) VALUES
(UUID(), 'default', 'user123', 'customer@example.com', 'CUSTOMER', 'USER_LOGIN', 'USER', 'user123', 'SUCCESS', '192.168.1.100', 'Mozilla/5.0...', 'req123', 'sess123', '{"loginMethod": "password"}'),
(UUID(), 'default', 'seller123', 'seller@example.com', 'SELLER', 'ORDER_ACCEPT', 'ORDER', 'order123', 'SUCCESS', '192.168.1.101', 'Mozilla/5.0...', 'req124', 'sess124', '{"storeId": "store123"}'),
(UUID(), 'default', 'admin123', 'admin@example.com', 'ADMIN', 'CONFIG_UPDATE', 'TENANT_CONFIG', 'config123', 'SUCCESS', '192.168.1.102', 'Mozilla/5.0...', 'req125', 'sess125', '{"configKey": "order.acceptance.timeout"}');