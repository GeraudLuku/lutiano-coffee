-- Changes waiting to be copied into the search index. The auto-increment id is the change's revision: the index only
-- accepts a document whose revision is not older than the one it holds, so replays and late retries are harmless.
CREATE TABLE search_outbox (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  type VARCHAR(30) NOT NULL,
  source_id VARCHAR(64) NOT NULL,
  status VARCHAR(10) NOT NULL DEFAULT 'PENDING',  -- PENDING, DONE, FAILED
  attempts INT NOT NULL DEFAULT 0,
  next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  error VARCHAR(500) NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_outbox_due (status, next_attempt_at, id),
  INDEX idx_outbox_source (type, source_id, status)
);
