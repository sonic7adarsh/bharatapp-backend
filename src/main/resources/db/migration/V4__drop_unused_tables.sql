-- Drop unused tables for MVP pruning
-- This migration removes hospitality and platform catalog features

-- Drop notification system tables (already removed via V3 deletion)
-- These would have been created by V3__add_notification_system.sql

-- Drop hospitality tables
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS rooms;

-- Drop platform catalog table
DROP TABLE IF EXISTS platform_products;

-- Drop notification system tables (if they exist from previous deployments)
DROP TABLE IF EXISTS notification_events;
DROP TABLE IF EXISTS notification_logs;
DROP TABLE IF EXISTS notification_providers;
DROP TABLE IF EXISTS user_notification_preferences;