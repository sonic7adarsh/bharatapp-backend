-- Enhanced Notification System for MVP
-- Replaces the dropped notification tables with a production-ready system

-- Notification Events Table
CREATE TABLE notification_events (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSON,
    status VARCHAR(50) DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'NORMAL',
    scheduled_at TIMESTAMP NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_notification_events_tenant (tenant_id),
    INDEX idx_notification_events_user (user_id),
    INDEX idx_notification_events_status (status),
    INDEX idx_notification_events_scheduled (scheduled_at),
    INDEX idx_notification_events_type (event_type)
);

-- Notification Logs Table
CREATE TABLE notification_logs (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    event_id VARCHAR(255) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    recipient VARCHAR(255) NOT NULL,
    message TEXT,
    status VARCHAR(50) DEFAULT 'SENT',
    error_message TEXT,
    response_data JSON,
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_notification_logs_tenant (tenant_id),
    INDEX idx_notification_logs_event (event_id),
    INDEX idx_notification_logs_provider (provider),
    INDEX idx_notification_logs_status (status),
    INDEX idx_notification_logs_sent (sent_at)
);

-- User Notification Preferences Table
CREATE TABLE user_notification_preferences (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    quiet_hours_start TIME,
    quiet_hours_end TIME,
    language VARCHAR(10) DEFAULT 'en',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_user_channel (tenant_id, user_id, channel),
    INDEX idx_preferences_user (user_id),
    INDEX idx_preferences_tenant_channel (tenant_id, channel)
);

-- Notification Templates Table
CREATE TABLE notification_templates (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    template_name VARCHAR(255) NOT NULL,
    subject VARCHAR(500),
    content TEXT NOT NULL,
    variables JSON,
    language VARCHAR(10) DEFAULT 'en',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE KEY uk_template_event_channel (tenant_id, event_type, channel, language),
    INDEX idx_templates_tenant (tenant_id),
    INDEX idx_templates_event (event_type),
    INDEX idx_templates_active (is_active)
);

-- Insert default notification templates
INSERT INTO notification_templates (id, tenant_id, event_type, channel, template_name, subject, content, variables) VALUES
-- Order notifications
(UUID(), 'default', 'ORDER_PLACED', 'WHATSAPP', 'order_placed_whatsapp', 'Order Confirmation', 'Your order #{orderId} has been placed successfully. Total: ₹{total}. Expected delivery: {deliveryTime}.', '["orderId", "total", "deliveryTime"]'),
(UUID(), 'default', 'ORDER_ACCEPTED', 'WHATSAPP', 'order_accepted_whatsapp', 'Order Accepted', 'Great news! Your order #{orderId} has been accepted by {storeName} and is being prepared.', '["orderId", "storeName"]'),
(UUID(), 'default', 'ORDER_READY', 'WHATSAPP', 'order_ready_whatsapp', 'Order Ready', 'Your order #{orderId} is ready for pickup/delivery!', '["orderId"]'),
(UUID(), 'default', 'ORDER_DELIVERED', 'WHATSAPP', 'order_delivered_whatsapp', 'Order Delivered', 'Your order #{orderId} has been delivered. Thank you for shopping with BharatShop!', '["orderId"]'),

-- OTP notifications
(UUID(), 'default', 'OTP_LOGIN', 'SMS', 'otp_login_sms', 'BharatShop Login', 'Your BharatShop login OTP is {otp}. Valid for 5 minutes. Do not share this code.', '["otp"]'),
(UUID(), 'default', 'OTP_ORDER', 'SMS', 'otp_order_sms', 'Order OTP', 'Your order delivery OTP is {otp}. Share this with the delivery partner.', '["otp"]'),

-- Seller notifications
(UUID(), 'default', 'NEW_ORDER', 'WHATSAPP', 'new_order_seller', 'New Order', 'New order #{orderId} for ₹{total}. Customer: {customerName}. Items: {itemCount}.', '["orderId", "total", "customerName", "itemCount"]'),
(UUID(), 'default', 'ORDER_CANCELLED', 'WHATSAPP', 'order_cancelled_seller', 'Order Cancelled', 'Order #{orderId} has been cancelled by customer. Reason: {reason}.', '["orderId", "reason"]');

-- Insert default user preferences (all channels enabled by default)
INSERT INTO user_notification_preferences (id, tenant_id, user_id, channel, enabled, quiet_hours_start, quiet_hours_end, language)
SELECT 
    UUID(), 
    'default', 
    u.id, 
    'WHATSAPP', 
    TRUE, 
    '22:00', 
    '08:00', 
    'en'
FROM users u;

INSERT INTO user_notification_preferences (id, tenant_id, user_id, channel, enabled, quiet_hours_start, quiet_hours_end, language)
SELECT 
    UUID(), 
    'default', 
    u.id, 
    'SMS', 
    TRUE, 
    '22:00', 
    '08:00', 
    'en'
FROM users u;

INSERT INTO user_notification_preferences (id, tenant_id, user_id, channel, enabled, quiet_hours_start, quiet_hours_end, language)
SELECT 
    UUID(), 
    'default', 
    u.id, 
    'EMAIL', 
    TRUE, 
    '22:00', 
    '08:00', 
    'en'
FROM users u;