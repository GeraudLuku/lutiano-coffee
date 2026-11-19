-- Images for product categories, add-ons and users (sellers). Served through /media/{kind}/{id}/{file}.
ALTER TABLE product_category ADD COLUMN image_url VARCHAR(500) NULL;
ALTER TABLE addon ADD COLUMN image_url VARCHAR(500) NULL;
ALTER TABLE staff_user ADD COLUMN image_url VARCHAR(500) NULL;
