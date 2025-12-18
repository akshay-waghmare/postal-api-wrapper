-- Sample data for local development
-- This script inserts test clients and trackings for development purposes
-- 
-- Usage: Enable with spring.sql.init.mode=always in application-dev.yml
-- Or run manually: mvn flyway:migrate && mvn sql:execute

-- Test Client 1: Free tier
-- API Key: sk_live_testfree_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
-- API Key Hash is for: sk_live_testfree_demo1234567890abcdef12345678
INSERT INTO clients (id, name, api_key_prefix, api_key_hash, plan, created_at, updated_at)
VALUES (1, 'Test Free Client', 'testfree', 
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X7/l5p.dVm5JyqPNe', 
        'FREE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Test Client 2: Pro tier
-- API Key: sk_live_testpro0_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
INSERT INTO clients (id, name, api_key_prefix, api_key_hash, plan, created_at, updated_at)
VALUES (2, 'Test Pro Client', 'testpro0', 
        '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
        'PRO', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Test Client 3: Enterprise tier
-- API Key: sk_live_testent0_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
INSERT INTO clients (id, name, api_key_prefix, api_key_hash, plan, created_at, updated_at)
VALUES (3, 'Test Enterprise Client', 'testent0', 
        '$2a$12$Y4mDXBq6z5Xz3vYN8z8g3.eH5U6tU.jK3Rl5Nv8M2XL4pQmTkVOYi', 
        'ENTERPRISE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Sample Trackings for Test Client 1
INSERT INTO trackings (id, tracking_id, client_id, tracking_number, courier_code, trackingmore_id, 
                       origin_country, destination_country, status, order_id, created_at, updated_at)
VALUES 
    (1, 'trk_sample001', 1, '9400111899223456789012', 'usps', 'tm_001', 
     'US', 'US', 'PENDING', 'ORD-001', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (2, 'trk_sample002', 1, 'EE123456789IN', 'india-post', 'tm_002', 
     'IN', 'US', 'IN_TRANSIT', 'ORD-002', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (3, 'trk_sample003', 1, '1Z999AA10123456784', 'ups', 'tm_003', 
     'US', 'CA', 'DELIVERED', 'ORD-003', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Sample Trackings for Test Client 2
INSERT INTO trackings (id, tracking_id, client_id, tracking_number, courier_code, trackingmore_id, 
                       origin_country, destination_country, status, order_id, created_at, updated_at)
VALUES 
    (4, 'trk_sample004', 2, 'JD014600004033594714', 'jd-worldwide', 'tm_004', 
     'CN', 'US', 'IN_TRANSIT', 'ORD-004', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (5, 'trk_sample005', 2, 'SF1234567890123', 'sf-express', 'tm_005', 
     'CN', 'GB', 'OUT_FOR_DELIVERY', 'ORD-005', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Reset sequences (PostgreSQL)
-- SELECT setval('clients_id_seq', (SELECT MAX(id) FROM clients));
-- SELECT setval('trackings_id_seq', (SELECT MAX(id) FROM trackings));
