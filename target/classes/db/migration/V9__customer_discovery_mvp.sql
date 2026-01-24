CREATE TABLE categories (
    id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    icon VARCHAR(255),
    priority INT DEFAULT 0,
    is_global BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_categories_name (name)
);

ALTER TABLE products ADD COLUMN category_id VARCHAR(36);
CREATE INDEX idx_products_category_id ON products(category_id);
CREATE INDEX idx_products_name ON products(name);
