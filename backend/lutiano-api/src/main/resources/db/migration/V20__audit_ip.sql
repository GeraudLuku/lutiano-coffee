-- The network address a logged action came from (for sign-ins and changes made through the portal or app).
ALTER TABLE audit_event ADD COLUMN ip VARCHAR(45) NULL;
