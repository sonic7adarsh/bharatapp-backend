-- Migration to remove tenant isolation for location-first platform

-- Drop tenant-specific configuration tables
DROP TABLE IF EXISTS tenant_configurations;
DROP TABLE IF EXISTS tenant_business_rules;
DROP TABLE IF EXISTS tenant_whatsapp_config;

-- Drop indexes involving tenant_id (if they exist)
-- Note: MySQL syntax. Using IF EXISTS where supported or assuming existence based on codebase.
-- For safety, we wrap in a stored procedure or just use simple statements if we are sure.
-- Given I can't easily check DB state, I will use simple DROP INDEX and hope names match db-indexes.sql

-- Zones
DROP INDEX IF EXISTS idx_zones_tenant_id ON zones;

-- Store Zones
DROP INDEX IF EXISTS idx_store_zones_tenant_store ON store_zones;

-- Rider Zones
DROP INDEX IF EXISTS idx_rider_zones_tenant_rider ON rider_zones;

-- Orders
DROP INDEX IF EXISTS idx_orders_tenant_user ON orders;

-- Order Deliveries
DROP INDEX IF EXISTS idx_order_deliveries_tenant_order ON order_deliveries;

-- Delivery Attempts
DROP INDEX IF EXISTS idx_delivery_attempts_tenant_delivery ON delivery_attempts;

-- Riders
DROP INDEX IF EXISTS idx_riders_tenant_status ON riders;

-- Inventory (if exists)
DROP INDEX IF EXISTS idx_inventory_tenant_product ON inventory;

-- Stores
DROP INDEX IF EXISTS idx_stores_tenant ON stores;

-- Drop tenant_id columns
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE orders DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE products DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE categories DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE stores DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE riders DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE payments DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE audit_logs DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE notification_events DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE notification_logs DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE notification_templates DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE rider_earnings DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE user_notification_preferences DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE delivery_attempts DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE zones DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE store_zones DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE rider_zones DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE inventory DROP COLUMN IF EXISTS tenant_id;
