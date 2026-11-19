-- The business details shown in Settings (and available to receipts and messages later). One row.
CREATE TABLE business_info (
  id TINYINT PRIMARY KEY,
  name VARCHAR(150) NOT NULL,
  description VARCHAR(255) NULL,
  email VARCHAR(150) NULL,
  phone VARCHAR(30) NULL,
  address VARCHAR(255) NULL,
  updated_by CHAR(36) NULL,
  updated_at TIMESTAMP NULL
);
INSERT INTO business_info (id, name, description, email, phone, address)
VALUES (1, 'LUTIANO Coffee', 'Premium coffee and beverage solutions', 'support@lutiano.cm', '+237 696 12 78 04', 'Douala, Cameroon');
