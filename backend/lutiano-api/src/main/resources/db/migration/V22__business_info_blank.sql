-- V21 seeded sample contact details copied from a design board. Clear them unless someone has already changed them.
UPDATE business_info SET description = NULL, email = NULL, phone = NULL, address = NULL
 WHERE id = 1 AND updated_at IS NULL;
