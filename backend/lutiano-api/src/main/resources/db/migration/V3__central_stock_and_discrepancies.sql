CREATE TABLE central_inventory (product_id CHAR(36) PRIMARY KEY, quantity INT NOT NULL DEFAULT 0);
CREATE TABLE stock_discrepancy (id CHAR(36) PRIMARY KEY, allocation_id CHAR(36) NOT NULL, sent_quantity INT NOT NULL, received_quantity INT NOT NULL, created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP);
INSERT INTO central_inventory(product_id,quantity) SELECT id,100 FROM product;
