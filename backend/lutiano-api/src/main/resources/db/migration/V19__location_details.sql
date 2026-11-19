-- Sales location extras from the design: a short description, a photo, and the payment methods each location accepts.
-- A location can only take a method that is also usable platform-wide (Payments -> Payment Methods); until Mobile Money
-- is enabled there, only Cash works. Existing locations accept every method, so enabling a method later works everywhere.
ALTER TABLE operation ADD COLUMN description VARCHAR(200) NULL, ADD COLUMN image_url VARCHAR(500) NULL;
CREATE TABLE operation_payment_method (
  operation_id CHAR(36) NOT NULL,
  method VARCHAR(20) NOT NULL,
  PRIMARY KEY (operation_id, method)
);
INSERT INTO operation_payment_method (operation_id, method) SELECT o.id, m.code FROM operation o CROSS JOIN payment_method m;
