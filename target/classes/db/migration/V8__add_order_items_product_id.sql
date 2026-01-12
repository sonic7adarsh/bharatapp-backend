-- Add product_id column to order_items to track inventory linkage
ALTER TABLE order_items ADD COLUMN product_id VARCHAR(255);