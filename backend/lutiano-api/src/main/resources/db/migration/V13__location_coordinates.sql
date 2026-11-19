-- Map position of a sales location (WGS84 decimal degrees), required by the API on create and edit. For a mobile
-- barista it is the usual base. Nullable so older rows can be positioned on their next edit; unpositioned ones are not mapped.
ALTER TABLE operation
  ADD COLUMN latitude DECIMAL(9,6) NULL,
  ADD COLUMN longitude DECIMAL(9,6) NULL;
UPDATE operation SET latitude = 4.027000, longitude = 9.693000 WHERE id = '10000000-0000-4000-8000-000000000001';
UPDATE operation SET latitude = 4.048000, longitude = 9.701000 WHERE id = '10000000-0000-4000-8000-000000000002';
