INSERT INTO users (id, username, email, password, role, status, created_at, updated_at)
VALUES
    (gen_random_uuid(), 'ash@pokemon.com', 'ash@pokemon.com', 'password123', 'PLAYER', 'ACTIVE', now(), now()),
    (gen_random_uuid(), 'misty@pokemon.com', 'misty@pokemon.com', 'password456', 'PLAYER', 'ACTIVE', now(), now());

INSERT INTO players (id, user_id, display_name, created_at)
SELECT gen_random_uuid(), id, 'Ash Ketchum', now() FROM users WHERE email = 'ash@pokemon.com'
UNION ALL
SELECT gen_random_uuid(), id, 'Misty', now() FROM users WHERE email = 'misty@pokemon.com';
