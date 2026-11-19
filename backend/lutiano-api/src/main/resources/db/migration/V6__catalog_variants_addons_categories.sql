-- A03 Products & Pricing: managed categories, units, stock-tracking and eligibility flags,
-- variants (parent product + sellable variant SKUs), add-ons, and dated price history.

-- Managed categories. product.category keeps the category name; renaming a category cascades to its products.
CREATE TABLE product_category (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(60) NOT NULL UNIQUE,
  description VARCHAR(255) NULL,
  sort_order INT NOT NULL DEFAULT 0,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
INSERT INTO product_category (id, name) SELECT UUID(), c.category FROM (SELECT DISTINCT category FROM product) c;
ALTER TABLE product ADD CONSTRAINT fk_product_category FOREIGN KEY (category) REFERENCES product_category (name) ON UPDATE CASCADE;

-- kind: SIMPLE (sold directly), PARENT (groups variants; never sold or stocked), VARIANT (sellable SKU under a parent).
ALTER TABLE product
  ADD COLUMN kind VARCHAR(10) NOT NULL DEFAULT 'SIMPLE',
  ADD COLUMN parent_id CHAR(36) NULL,
  ADD COLUMN variant_name VARCHAR(80) NULL,
  ADD COLUMN unit VARCHAR(30) NULL,
  ADD COLUMN track_stock BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN loyalty_eligible BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN promotion_eligible BOOLEAN NOT NULL DEFAULT TRUE,
  ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
  ADD COLUMN updated_at TIMESTAMP NULL;
CREATE INDEX idx_product_parent ON product (parent_id);
UPDATE product SET unit = 'cup' WHERE id IN ('30000000-0000-4000-8000-000000000001', '30000000-0000-4000-8000-000000000002');
UPDATE product SET unit = '250 g pack' WHERE id = '30000000-0000-4000-8000-000000000003';

-- Dated price history (prices apply to new sales only; sale lines keep their own snapshot).
CREATE TABLE product_price_history (
  id CHAR(36) PRIMARY KEY,
  product_id CHAR(36) NOT NULL,
  price_xaf BIGINT NOT NULL,
  previous_price_xaf BIGINT NULL,
  changed_by CHAR(36) NULL,
  changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_price_history_product (product_id, changed_at)
);
INSERT INTO product_price_history (id, product_id, price_xaf, changed_at) SELECT UUID(), id, price_xaf, created_at FROM product;

-- Add-ons (extra shot, syrups…) with a price added per unit. Allowed per SIMPLE or PARENT product; variants inherit.
CREATE TABLE addon (
  id CHAR(36) PRIMARY KEY,
  name VARCHAR(80) NOT NULL UNIQUE,
  price_xaf BIGINT NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE,
  sort_order INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE product_addon (
  product_id CHAR(36) NOT NULL,
  addon_id CHAR(36) NOT NULL,
  PRIMARY KEY (product_id, addon_id)
);

-- Sale lines: the same product can appear twice with different add-ons, so lines are numbered.
ALTER TABLE sale_line
  ADD COLUMN line_no INT NOT NULL DEFAULT 0,
  ADD COLUMN addons_unit_xaf BIGINT NOT NULL DEFAULT 0;
UPDATE sale_line l
  JOIN (SELECT sale_id, product_id, ROW_NUMBER() OVER (PARTITION BY sale_id ORDER BY product_name, product_id) rn FROM sale_line) x
    ON x.sale_id = l.sale_id AND x.product_id = l.product_id
   SET l.line_no = x.rn;
ALTER TABLE sale_line DROP PRIMARY KEY, ADD PRIMARY KEY (sale_id, line_no);
CREATE INDEX idx_sale_line_product ON sale_line (product_id);

-- Add-ons chosen on a sale line, with name and price snapshots.
CREATE TABLE sale_line_addon (
  sale_id CHAR(36) NOT NULL,
  line_no INT NOT NULL,
  addon_id CHAR(36) NOT NULL,
  addon_name VARCHAR(80) NOT NULL,
  unit_price_xaf BIGINT NOT NULL,
  PRIMARY KEY (sale_id, line_no, addon_id)
);
