-- Database indexes for BharatShop performance optimization
-- Run these indexes based on your database (MySQL/PostgreSQL syntax)

-- Zone and serviceability indexes
CREATE INDEX idx_zones_tenant_id ON zones(tenant_id);
CREATE INDEX idx_store_zones_tenant_store ON store_zones(tenant_id, store_id);
CREATE INDEX idx_rider_zones_tenant_rider ON rider_zones(tenant_id, rider_id);
CREATE INDEX idx_zones_center_lat_lng ON zones(center_lat, center_lng);

-- Order and delivery indexes
CREATE INDEX idx_orders_tenant_user ON orders(tenant_id, user_id);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_order_deliveries_tenant_order ON order_deliveries(tenant_id, order_id);
CREATE INDEX idx_order_deliveries_status ON order_deliveries(status);
CREATE INDEX idx_order_deliveries_rider ON order_deliveries(rider_id);

-- Delivery attempt indexes
CREATE INDEX idx_delivery_attempts_tenant_delivery ON delivery_attempts(tenant_id, delivery_id);
CREATE INDEX idx_delivery_attempts_ts ON delivery_attempts(ts DESC);

-- Rider and location indexes
CREATE INDEX idx_riders_tenant_status ON riders(tenant_id, status);
CREATE INDEX idx_rider_locations_rider ON rider_locations(rider_id);

-- Inventory indexes
CREATE INDEX idx_inventory_tenant_product ON inventory(tenant_id, product_id);
CREATE INDEX idx_inventory_reserved ON inventory(reserved);

-- Store indexes
CREATE INDEX idx_stores_tenant ON stores(tenant_id);
CREATE INDEX idx_stores_city ON stores(city);

-- Product indexes
CREATE INDEX idx_products_store ON products(store_id);
CREATE INDEX idx_products_category ON products(category);