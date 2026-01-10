-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    total DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

-- Sample data
INSERT INTO orders (id, customer_id, total, status, created_at) VALUES
    ('ORD-001', 'CUST-001', 150.00, 'COMPLETED', '2026-01-10 10:00:00'),
    ('ORD-002', 'CUST-001', 75.50, 'PENDING', '2026-01-10 11:00:00'),
    ('ORD-003', 'CUST-002', 200.00, 'SHIPPED', '2026-01-10 12:00:00');
