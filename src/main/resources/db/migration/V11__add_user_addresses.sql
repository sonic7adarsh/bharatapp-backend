-- Add alternate_phone to users table
ALTER TABLE users ADD COLUMN alternate_phone VARCHAR(255);

-- Create user_addresses table
CREATE TABLE user_addresses (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    phone VARCHAR(255),
    alternate_phone VARCHAR(255),
    line1 VARCHAR(255),
    line2 VARCHAR(255),
    city VARCHAR(255),
    state VARCHAR(255),
    zip VARCHAR(255),
    type VARCHAR(50),
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Index for user_id for faster lookups
CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);

-- Add customer details to orders table
ALTER TABLE orders ADD COLUMN customer_name VARCHAR(255);
ALTER TABLE orders ADD COLUMN customer_phone VARCHAR(255);
ALTER TABLE orders ADD COLUMN customer_alternate_phone VARCHAR(255);
