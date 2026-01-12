-- Tenant Configuration System for MVP
-- Enables per-tenant business rules and feature flags

-- Tenant Configuration Table
CREATE TABLE tenant_configurations (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    config_key VARCHAR(255) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(50) DEFAULT 'STRING',
    description TEXT,
    is_encrypted BOOLEAN DEFAULT FALSE,
    is_feature_flag BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_tenant_config (tenant_id, config_key),
    INDEX idx_tenant_config (tenant_id),
    INDEX idx_feature_flags (is_feature_flag),
    INDEX idx_config_key (config_key)
);

-- Tenant Business Rules Configuration
CREATE TABLE tenant_business_rules (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    rule_type VARCHAR(100) NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_config JSON,
    is_active BOOLEAN DEFAULT TRUE,
    priority INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_tenant_rule (tenant_id, rule_type, rule_name),
    INDEX idx_tenant_rules (tenant_id),
    INDEX idx_rule_type (rule_type),
    INDEX idx_rule_active (is_active)
);

-- Tenant Whatsapp Configuration
CREATE TABLE tenant_whatsapp_config (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    api_key VARCHAR(500),
    sender_number VARCHAR(50),
    api_url VARCHAR(500),
    webhook_url VARCHAR(500),
    is_enabled BOOLEAN DEFAULT TRUE,
    rate_limit_per_minute INTEGER DEFAULT 60,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_tenant_whatsapp (tenant_id),
    INDEX idx_whatsapp_enabled (is_enabled)
);

-- Insert default tenant configurations
INSERT INTO tenant_configurations (id, tenant_id, config_key, config_value, config_type, description, is_feature_flag) VALUES
-- Feature Flags
(UUID(), 'default', 'feature.whatsapp.enabled', 'true', 'BOOLEAN', 'Enable WhatsApp notifications', TRUE),
(UUID(), 'default', 'feature.sms.enabled', 'true', 'BOOLEAN', 'Enable SMS notifications', TRUE),
(UUID(), 'default', 'feature.email.enabled', 'true', 'BOOLEAN', 'Enable Email notifications', TRUE),
(UUID(), 'default', 'feature.otp.enabled', 'true', 'BOOLEAN', 'Enable OTP verification', TRUE),
(UUID(), 'default', 'feature.rider.tracking', 'true', 'BOOLEAN', 'Enable rider tracking', TRUE),
(UUID(), 'default', 'feature.inventory.reservation', 'true', 'BOOLEAN', 'Enable inventory reservation', TRUE),
(UUID(), 'default', 'feature.seller.bulk.upload', 'true', 'BOOLEAN', 'Enable seller bulk upload', TRUE),
(UUID(), 'default', 'feature.customer.support', 'true', 'BOOLEAN', 'Enable customer support chat', TRUE),

-- Business Rules
(UUID(), 'default', 'order.acceptance.timeout', '15', 'INTEGER', 'Minutes to accept order before auto-rejection', FALSE),
(UUID(), 'default', 'order.preparation.timeout', '30', 'INTEGER', 'Minutes to prepare order', FALSE),
(UUID(), 'default', 'order.delivery.radius', '5', 'INTEGER', 'Delivery radius in kilometers', FALSE),
(UUID(), 'default', 'order.minimum.amount', '100', 'INTEGER', 'Minimum order amount in rupees', FALSE),
(UUID(), 'default', 'order.maximum.amount', '50000', 'INTEGER', 'Maximum order amount in rupees', FALSE),
(UUID(), 'default', 'inventory.reservation.timeout', '10', 'INTEGER', 'Minutes to reserve inventory', FALSE),
(UUID(), 'default', 'cart.abandonment.timeout', '30', 'INTEGER', 'Minutes before cart is considered abandoned', FALSE),
(UUID(), 'default', 'otp.expiry.minutes', '5', 'INTEGER', 'OTP expiry time in minutes', FALSE),
(UUID(), 'default', 'otp.max.attempts', '3', 'INTEGER', 'Maximum OTP attempts', FALSE),
(UUID(), 'default', 'notification.quiet.hours.start', '22:00', 'STRING', 'Start of quiet hours for notifications', FALSE),
(UUID(), 'default', 'notification.quiet.hours.end', '08:00', 'STRING', 'End of quiet hours for notifications', FALSE),
(UUID(), 'default', 'rate.limit.per.user', '100', 'INTEGER', 'API calls per minute per user', FALSE),
(UUID(), 'default', 'rate.limit.per.tenant', '1000', 'INTEGER', 'API calls per minute per tenant', FALSE),

-- WhatsApp Configuration
(UUID(), 'default', 'whatsapp.provider', 'default', 'STRING', 'WhatsApp provider name', FALSE),
(UUID(), 'default', 'whatsapp.sender.number', '+919876543210', 'STRING', 'WhatsApp sender number', FALSE),
(UUID(), 'default', 'whatsapp.api.url', 'https://api.whatsapp.com/v1', 'STRING', 'WhatsApp API URL', FALSE),
(UUID(), 'default', 'whatsapp.rate.limit', '60', 'INTEGER', 'WhatsApp messages per minute', FALSE);

-- Insert default business rules
INSERT INTO tenant_business_rules (id, tenant_id, rule_type, rule_name, rule_config, priority) VALUES
(UUID(), 'default', 'ORDER_VALIDATION', 'minimum_order_amount', '{"minAmount": 100, "currency": "INR"}', 1),
(UUID(), 'default', 'ORDER_VALIDATION', 'maximum_order_amount', '{"maxAmount": 50000, "currency": "INR"}', 2),
(UUID(), 'default', 'DELIVERY', 'delivery_radius_check', '{"maxRadius": 5, "unit": "km"}', 1),
(UUID(), 'default', 'SELLER', 'acceptance_timeout', '{"timeoutMinutes": 15, "autoReject": true}', 1),
(UUID(), 'default', 'INVENTORY', 'reservation_timeout', '{"timeoutMinutes": 10, "autoRelease": true}', 1),
(UUID(), 'default', 'NOTIFICATION', 'quiet_hours', '{"startTime": "22:00", "endTime": "08:00", "timezone": "IST"}', 1);