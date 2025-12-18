-- =====================================================
-- V3: Add client status and expiry
-- =====================================================
-- Adds support for deactivating clients and setting API key expiration

ALTER TABLE clients
ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE,
ADD COLUMN expires_at TIMESTAMP;

-- Index for cleanup jobs or expiration checks
CREATE INDEX idx_clients_expires_at ON clients(expires_at);
