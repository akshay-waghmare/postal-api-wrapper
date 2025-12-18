-- =====================================================
-- V2: Create trackings table
-- =====================================================
-- Stores shipment tracking records with wrapper-to-upstream ID mapping
-- Supports soft delete for audit trail

CREATE TABLE trackings (
    id BIGSERIAL PRIMARY KEY,
    tracking_id VARCHAR(32) NOT NULL,
    client_id BIGINT NOT NULL,
    tracking_number VARCHAR(255) NOT NULL,
    courier_code VARCHAR(100) NOT NULL,
    trackingmore_id VARCHAR(255),
    origin_country VARCHAR(2),
    destination_country VARCHAR(2),
    status VARCHAR(50),
    order_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP,

    -- Foreign key to clients table
    CONSTRAINT fk_trackings_client 
        FOREIGN KEY (client_id) 
        REFERENCES clients(id) 
        ON DELETE CASCADE
);

-- Unique constraint on wrapper tracking ID (client-facing)
CREATE UNIQUE INDEX idx_trackings_tracking_id ON trackings(tracking_id);

-- Index for TrackingMore ID (upstream reference) - not partial for H2 compatibility
CREATE INDEX idx_trackings_trackingmore_id ON trackings(trackingmore_id);

-- Index for client-scoped queries (most common access pattern)
CREATE INDEX idx_trackings_client_id ON trackings(client_id);

-- Composite index for active trackings per client (soft delete filter)
CREATE INDEX idx_trackings_client_deleted ON trackings(client_id, deleted_at);

-- Index for lookups by tracking number
CREATE INDEX idx_trackings_tracking_number ON trackings(tracking_number);

-- Index for filtering by status
CREATE INDEX idx_trackings_status ON trackings(status);

-- Composite index for common query pattern: client + status
CREATE INDEX idx_trackings_client_status ON trackings(client_id, status);

COMMENT ON TABLE trackings IS 'Shipment tracking records with wrapper-to-upstream ID mapping';
COMMENT ON COLUMN trackings.tracking_id IS 'Wrapper-generated ID (e.g., trk_9f3a2b8c) exposed to clients';
COMMENT ON COLUMN trackings.trackingmore_id IS 'TrackingMore internal ID from upstream API response';
COMMENT ON COLUMN trackings.status IS 'Normalized status: PENDING, NOT_FOUND, IN_TRANSIT, OUT_FOR_DELIVERY, DELIVERED, EXCEPTION, EXPIRED, RETURNED';
COMMENT ON COLUMN trackings.deleted_at IS 'Soft delete timestamp - NULL if active';
