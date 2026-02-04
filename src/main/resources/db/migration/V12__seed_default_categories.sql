INSERT INTO categories (id, name, icon, priority, is_global) VALUES
('cat-medicine', 'Medicine', 'medicine', 1, TRUE),
('cat-stationary', 'Stationary', 'stationary', 2, TRUE),
('cat-service', 'Service', 'service', 3, TRUE),
('cat-grocery', 'Groceries & Staples', 'grocery', 4, TRUE)
ON DUPLICATE KEY UPDATE
name = VALUES(name),
icon = VALUES(icon),
priority = VALUES(priority);
