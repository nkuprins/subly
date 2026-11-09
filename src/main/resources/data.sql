INSERT IGNORE INTO categories (id, name) VALUES
    (1, 'Streaming'),
    (2, 'Music'),
    (3, 'Software'),
    (4, 'Gym'),
    (5, 'News'),
    (6, 'Other');

-- Demo logins: alice, bob, carol, dave, erin / subly-demo-password.
INSERT IGNORE INTO users (id, username, name, password_hash, email, role, time_zone) VALUES
    (2, 'alice', 'Alice', '$2a$10$G31NTfu5/gcQEbSRD0r6GucCJa6lccHZ7iDOfJQLeE2O96mBZpIiu', 'alice@example.com', 'USER',  'Europe/Riga'),
    (3, 'bob',   'Bob',   '$2a$10$G31NTfu5/gcQEbSRD0r6GucCJa6lccHZ7iDOfJQLeE2O96mBZpIiu', 'bob@example.com',   'USER',  'Europe/Riga'),
    (4, 'carol', 'Carol', '$2a$10$G31NTfu5/gcQEbSRD0r6GucCJa6lccHZ7iDOfJQLeE2O96mBZpIiu', 'carol@example.com', 'USER',  'Europe/Riga'),
    (5, 'dave',  'Dave',  '$2a$10$G31NTfu5/gcQEbSRD0r6GucCJa6lccHZ7iDOfJQLeE2O96mBZpIiu', 'dave@example.com',  'USER',  'Europe/Riga'),
    (6, 'erin',  'Erin',  '$2a$10$G31NTfu5/gcQEbSRD0r6GucCJa6lccHZ7iDOfJQLeE2O96mBZpIiu', 'erin@example.com',  'USER',  'Europe/Riga');

INSERT IGNORE INTO subscription_providers (id, name, category_id, created_by, provider_type) VALUES
    (1,  'Netflix',              1, NULL, 'CATALOG'),
    (2,  'Disney+',              1, NULL, 'CATALOG'),
    (3,  'HBO Max',              1, NULL, 'CATALOG'),
    (4,  'YouTube Premium',      1, NULL, 'CATALOG'),
    (5,  'Spotify',              2, NULL, 'CATALOG'),
    (6,  'Apple Music',          2, NULL, 'CATALOG'),
    (7,  'Microsoft 365',        3, NULL, 'CATALOG'),
    (8,  'Adobe Creative Cloud', 3, NULL, 'CATALOG'),
    (9,  'ChatGPT Plus',         3, NULL, 'CATALOG'),
    (10, 'The New York Times',   5, NULL, 'CATALOG'),
    (11, 'My gym',               4, 2,    'CUSTOM'),
    (12, 'My gym',               4, 3,    'CUSTOM'),
    (13, 'Local newspaper',      5, 4,    'CUSTOM');

INSERT IGNORE INTO subscriptions
    (id, provider_id, price, billing_cycle, next_payment_date, billing_anchor_date,
     free_trial, status, cancelled_at, currency, max_participants) VALUES
    (1,  1,  17.99,  'MONTHLY', CURDATE() + INTERVAL 3 DAY,  CURDATE() + INTERVAL 3 DAY - INTERVAL 8 MONTH,   FALSE, 'ACTIVE',    NULL,                       'EUR', 4),
    (2,  5,  19.99,  'MONTHLY', CURDATE() + INTERVAL 12 DAY, CURDATE() + INTERVAL 12 DAY - INTERVAL 5 MONTH,  FALSE, 'ACTIVE',    NULL,                       'EUR', 6),
    (3,  7,  99.00,  'YEARLY',  CURDATE() + INTERVAL 45 DAY, CURDATE() + INTERVAL 45 DAY - INTERVAL 1 YEAR,   FALSE, 'ACTIVE',    NULL,                       'EUR', 1),
    (4,  11, 35.00,  'MONTHLY', CURDATE() + INTERVAL 20 DAY, CURDATE() + INTERVAL 20 DAY - INTERVAL 14 MONTH, FALSE, 'ACTIVE',    NULL,                       'EUR', 1),
    (5,  2,  9.99,   'MONTHLY', CURDATE() + INTERVAL 5 DAY,  CURDATE() + INTERVAL 5 DAY,                      TRUE,  'ACTIVE',    NULL,                       'EUR', 1),
    (6,  3,  10.99,  'MONTHLY', CURDATE() + INTERVAL 8 DAY,  CURDATE() + INTERVAL 8 DAY - INTERVAL 6 MONTH,   FALSE, 'CANCELLED', NOW() - INTERVAL 10 DAY,    'EUR', 1),
    (7,  4,  12.99,  'MONTHLY', CURDATE() - INTERVAL 40 DAY, CURDATE() - INTERVAL 40 DAY - INTERVAL 6 MONTH,  FALSE, 'CANCELLED', NOW() - INTERVAL 55 DAY,    'EUR', 1),
    (8,  12, 29.00,  'MONTHLY', CURDATE() + INTERVAL 1 DAY,  CURDATE() + INTERVAL 1 DAY - INTERVAL 3 MONTH,   FALSE, 'ACTIVE',    NULL,                       'EUR', 1),
    (9,  9,  20.00,  'MONTHLY', CURDATE() + INTERVAL 15 DAY, CURDATE() + INTERVAL 15 DAY - INTERVAL 2 MONTH,  FALSE, 'ACTIVE',    NULL,                       'USD', 1),
    (10, 7,  129.00, 'YEARLY',  CURDATE() + INTERVAL 60 DAY, CURDATE() + INTERVAL 60 DAY - INTERVAL 2 YEAR,   FALSE, 'ACTIVE',    NULL,                       'EUR', 6),
    (11, 6,  10.99,  'MONTHLY', CURDATE() + INTERVAL 9 DAY,  CURDATE() + INTERVAL 9 DAY - INTERVAL 4 MONTH,   FALSE, 'ACTIVE',    NULL,                       'GBP', 1),
    (12, 13, 8.50,   'MONTHLY', CURDATE() + INTERVAL 28 DAY, CURDATE() + INTERVAL 28 DAY - INTERVAL 1 MONTH,  FALSE, 'ACTIVE',    NULL,                       'EUR', 1);

INSERT IGNORE INTO subscription_membership (id, user_id, subscription_id, role, joined_at) VALUES
    (1,  2, 1,  'OWNER',  CURDATE() + INTERVAL 3 DAY - INTERVAL 8 MONTH),
    (2,  3, 1,  'MEMBER', CURDATE() - INTERVAL 95 DAY),
    (3,  4, 1,  'MEMBER', CURDATE() - INTERVAL 95 DAY),
    (4,  2, 2,  'OWNER',  CURDATE() + INTERVAL 12 DAY - INTERVAL 5 MONTH),
    (5,  5, 2,  'MEMBER', CURDATE() - INTERVAL 55 DAY),
    (6,  2, 3,  'OWNER',  CURDATE() + INTERVAL 45 DAY - INTERVAL 1 YEAR),
    (7,  2, 4,  'OWNER',  CURDATE() + INTERVAL 20 DAY - INTERVAL 14 MONTH),
    (8,  2, 5,  'OWNER',  CURDATE() - INTERVAL 25 DAY),
    (9,  2, 6,  'OWNER',  CURDATE() + INTERVAL 8 DAY - INTERVAL 6 MONTH),
    (10, 3, 7,  'OWNER',  CURDATE() - INTERVAL 40 DAY - INTERVAL 6 MONTH),
    (11, 3, 8,  'OWNER',  CURDATE() + INTERVAL 1 DAY - INTERVAL 3 MONTH),
    (12, 3, 9,  'OWNER',  CURDATE() + INTERVAL 15 DAY - INTERVAL 2 MONTH),
    (13, 3, 10, 'OWNER',  CURDATE() + INTERVAL 60 DAY - INTERVAL 2 YEAR),
    (14, 4, 10, 'MEMBER', CURDATE() - INTERVAL 320 DAY),
    (15, 4, 11, 'OWNER',  CURDATE() + INTERVAL 9 DAY - INTERVAL 4 MONTH),
    (16, 4, 12, 'OWNER',  CURDATE() + INTERVAL 28 DAY - INTERVAL 1 MONTH);

INSERT IGNORE INTO subscription_pending (id, user_id, subscription_id, invited_at) VALUES
    (1, 5, 1,  NOW() - INTERVAL 2 DAY),
    (2, 6, 2,  NOW() - INTERVAL 1 DAY),
    (3, 6, 10, NOW() - INTERVAL 6 HOUR);

INSERT IGNORE INTO subscription_payments (id, membership_id, amount, paid_on) VALUES
    (1,  1,  6.01,  CURDATE() + INTERVAL 3 DAY - INTERVAL 3 MONTH),
    (2,  2,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 3 MONTH),
    (3,  3,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 3 MONTH),
    (4,  1,  6.01,  CURDATE() + INTERVAL 3 DAY - INTERVAL 2 MONTH),
    (5,  2,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 2 MONTH),
    (6,  3,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 2 MONTH),
    (7,  1,  6.01,  CURDATE() + INTERVAL 3 DAY - INTERVAL 1 MONTH),
    (8,  2,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 1 MONTH),
    (9,  3,  5.99,  CURDATE() + INTERVAL 3 DAY - INTERVAL 1 MONTH),
    (10, 4,  10.00, CURDATE() + INTERVAL 12 DAY - INTERVAL 2 MONTH),
    (11, 5,  9.99,  CURDATE() + INTERVAL 12 DAY - INTERVAL 2 MONTH),
    (12, 4,  10.00, CURDATE() + INTERVAL 12 DAY - INTERVAL 1 MONTH),
    (13, 5,  9.99,  CURDATE() + INTERVAL 12 DAY - INTERVAL 1 MONTH),
    (14, 6,  99.00, CURDATE() + INTERVAL 45 DAY - INTERVAL 1 YEAR),
    (15, 7,  35.00, CURDATE() + INTERVAL 20 DAY - INTERVAL 3 MONTH),
    (16, 7,  35.00, CURDATE() + INTERVAL 20 DAY - INTERVAL 2 MONTH),
    (17, 7,  35.00, CURDATE() + INTERVAL 20 DAY - INTERVAL 1 MONTH),
    (18, 9,  10.99, CURDATE() + INTERVAL 8 DAY - INTERVAL 3 MONTH),
    (19, 9,  10.99, CURDATE() + INTERVAL 8 DAY - INTERVAL 2 MONTH),
    (20, 9,  10.99, CURDATE() + INTERVAL 8 DAY - INTERVAL 1 MONTH),
    (21, 10, 12.99, CURDATE() - INTERVAL 40 DAY - INTERVAL 3 MONTH),
    (22, 10, 12.99, CURDATE() - INTERVAL 40 DAY - INTERVAL 2 MONTH),
    (23, 10, 12.99, CURDATE() - INTERVAL 40 DAY - INTERVAL 1 MONTH),
    (24, 11, 29.00, CURDATE() + INTERVAL 1 DAY - INTERVAL 2 MONTH),
    (25, 11, 29.00, CURDATE() + INTERVAL 1 DAY - INTERVAL 1 MONTH),
    (26, 12, 20.00, CURDATE() + INTERVAL 15 DAY - INTERVAL 1 MONTH),
    (27, 13, 64.50, CURDATE() + INTERVAL 60 DAY - INTERVAL 1 YEAR),
    (28, 14, 64.50, CURDATE() + INTERVAL 60 DAY - INTERVAL 1 YEAR),
    (29, 15, 10.99, CURDATE() + INTERVAL 9 DAY - INTERVAL 2 MONTH),
    (30, 15, 10.99, CURDATE() + INTERVAL 9 DAY - INTERVAL 1 MONTH),
    (31, 16, 8.50,  CURDATE() + INTERVAL 28 DAY - INTERVAL 1 MONTH);
