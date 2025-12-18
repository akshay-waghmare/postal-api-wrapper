-- =====================================================
-- V1: Create clients table
-- =====================================================
-- Stores API client accounts with Stripe-style API key storage
-- (prefix for log correlation + bcrypt hash for authentication)

CREATE TABLE clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    api_key_prefix VARCHAR(16) NOT NULL,
    api_key_hash VARCHAR(60) NOT NULL,
    plan VARCHAR(50) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Unique constraint on api_key_hash for authentication lookups
CREATE UNIQUE INDEX idx_clients_api_key_hash ON clients(api_key_hash);

-- Index on api_key_prefix for log correlation lookups
CREATE INDEX idx_clients_api_key_prefix ON clients(api_key_prefix);

-- Index on plan for filtering clients by tier
CREATE INDEX idx_clients_plan ON clients(plan);

COMMENT ON TABLE clients IS 'API client accounts with rate-limited access';
COMMENT ON COLUMN clients.api_key_prefix IS 'First 8-16 chars of API key (e.g., sk_live_ab12cd) for log correlation';
COMMENT ON COLUMN clients.api_key_hash IS 'bcrypt hash of full API key for secure authentication';
COMMENT ON COLUMN clients.plan IS 'Rate limit tier: FREE, STARTER, PRO, ENTERPRISE';
