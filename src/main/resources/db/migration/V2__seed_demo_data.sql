INSERT INTO users (id, email, full_name, password_hash, enabled, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'customer@bankflow.dev', 'Demo Customer', crypt('Customer123!', gen_salt('bf', 10)), TRUE, now()),
    ('22222222-2222-2222-2222-222222222222', 'admin@bankflow.dev', 'Demo Admin', crypt('Admin123!', gen_salt('bf', 10)), TRUE, now());

INSERT INTO user_roles (user_id, role)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'CUSTOMER'),
    ('22222222-2222-2222-2222-222222222222', 'ADMIN');

INSERT INTO accounts (id, user_id, account_number, type, status, balance, version, created_at)
VALUES
    ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'BF1000000001', 'CHECKING', 'ACTIVE', 1000.00, 0, now()),
    ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'BF1000000002', 'SAVINGS', 'ACTIVE', 500.00, 0, now());

INSERT INTO transfers (id, source_account_id, destination_account_id, amount, description, status, created_at)
VALUES
    ('cccccccc-cccc-cccc-cccc-cccccccccccc', NULL, 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 1000.00, 'Seed opening balance', 'COMPLETED', now()),
    ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', NULL, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 500.00, 'Seed opening balance', 'COMPLETED', now());

INSERT INTO ledger_entries (id, account_id, transfer_id, entry_type, amount, balance_after, reference, created_at)
VALUES
    ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'CREDIT', 1000.00, 1000.00, 'SEED_OPENING_BALANCE', now()),
    ('ffffffff-ffff-ffff-ffff-ffffffffffff', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'CREDIT', 500.00, 500.00, 'SEED_OPENING_BALANCE', now());

INSERT INTO audit_logs (actor_user_id, action, resource_type, resource_id, details, created_at)
VALUES ('22222222-2222-2222-2222-222222222222', 'USER_REGISTERED', 'SYSTEM', NULL, 'Demo data seeded by Flyway', now());
