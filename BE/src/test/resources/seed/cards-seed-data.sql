-- ============================================================
-- SEED DATA FOR INTEGRATION TESTS
-- Cards con IDs fijos para tests deterministas
-- ============================================================

-- 1. BASIC ENERGIES
INSERT INTO cards (id, name, supertype, set_code, number, hp, pokemon_stage, energy_card_type, provides_energy_types, pokemon_types, retreat_cost, is_ex, is_mega, is_ace_spec, created_at, updated_at)
VALUES
('seed-fire-energy',    'Fire Energy',     'Energy', 'test', '01', 0, NULL, 'BASIC', 'FIRE',     NULL, NULL, false, false, false, NOW(), NOW()),
('seed-lightning-energy', 'Lightning Energy', 'Energy', 'test', '02', 0, NULL, 'BASIC', 'LIGHTNING', NULL, NULL, false, false, false, NOW(), NOW()),
('seed-water-energy',   'Water Energy',    'Energy', 'test', '03', 0, NULL, 'BASIC', 'WATER',    NULL, NULL, false, false, false, NOW(), NOW()),
('seed-fighting-energy','Fighting Energy',  'Energy', 'test', '04', 0, NULL, 'BASIC', 'FIGHTING', NULL, NULL, false, false, false, NOW(), NOW());

-- 2. BASIC POKEMON
INSERT INTO cards (id, name, supertype, set_code, number, hp, pokemon_stage, evolves_from, pokemon_types, retreat_cost, is_ex, is_mega, is_ace_spec, created_at, updated_at)
VALUES
('seed-pikachu',    'Pikachu',    'Pokemon', 'test', '10', 60,  'BASIC', NULL, 'LIGHTNING', 'COLORLESS', false, false, false, NOW(), NOW()),
('seed-charmander', 'Charmander', 'Pokemon', 'test', '11', 50,  'BASIC', NULL, 'FIRE',      'COLORLESS', false, false, false, NOW(), NOW()),
('seed-squirtle',   'Squirtle',   'Pokemon', 'test', '12', 50,  'BASIC', NULL, 'WATER',     'COLORLESS', false, false, false, NOW(), NOW()),
('seed-pikachu-ex', 'Pikachu-EX', 'Pokemon', 'test', '20', 120, 'BASIC', NULL, 'LIGHTNING', 'COLORLESS', true,  false, false, NOW(), NOW());

-- 3. STAGE 1 POKEMON
INSERT INTO cards (id, name, supertype, set_code, number, hp, pokemon_stage, evolves_from, pokemon_types, retreat_cost, is_ex, is_mega, is_ace_spec, created_at, updated_at)
VALUES
('seed-charmeleon', 'Charmeleon', 'Pokemon', 'test', '30', 90, 'STAGE_1', 'Charmander', 'FIRE', 'COLORLESS,COLORLESS', false, false, false, NOW(), NOW());

-- 4. STAGE 2 POKEMON
INSERT INTO cards (id, name, supertype, set_code, number, hp, pokemon_stage, evolves_from, pokemon_types, retreat_cost, is_ex, is_mega, is_ace_spec, created_at, updated_at)
VALUES
('seed-charizard', 'Charizard', 'Pokemon', 'test', '40', 150, 'STAGE_2', 'Charmeleon', 'FIRE', 'COLORLESS,COLORLESS,COLORLESS', false, false, false, NOW(), NOW());

-- 5. ATTACKS
-- Pikachu: Thunderbolt (30 dmg, 1 Lightning)
INSERT INTO card_attacks (id, card_id, attack_index, name, printed_cost, converted_energy_cost, damage_text, base_damage, effect_text, created_at)
SELECT RANDOM_UUID(), 'seed-pikachu', 0, 'Thunderbolt', 'LIGHTNING', 1, '30', 30, '', NOW() UNION ALL
SELECT RANDOM_UUID(), 'seed-charmander', 0, 'Ember', 'FIRE', 1, '30', 30, '', NOW() UNION ALL
SELECT RANDOM_UUID(), 'seed-squirtle', 0, 'Water Gun', 'WATER', 1, '20', 20, '', NOW() UNION ALL
SELECT RANDOM_UUID(), 'seed-pikachu-ex', 0, 'Thunderbolt', 'LIGHTNING', 1, '50', 50, '', NOW() UNION ALL
SELECT RANDOM_UUID(), 'seed-charmeleon', 0, 'Flamethrower', 'FIRE', 1, '60', 60, '', NOW() UNION ALL
SELECT RANDOM_UUID(), 'seed-charizard', 0, 'Fire Spin', 'FIRE,FIRE', 2, '100', 100, '', NOW();

-- 6. WEAKNESSES
INSERT INTO card_weaknesses (id, card_id, energy_type, multiplier)
SELECT RANDOM_UUID(), 'seed-pikachu', 'FIGHTING', 2 UNION ALL
SELECT RANDOM_UUID(), 'seed-charmander', 'WATER', 2 UNION ALL
SELECT RANDOM_UUID(), 'seed-squirtle', 'LIGHTNING', 2 UNION ALL
SELECT RANDOM_UUID(), 'seed-charmeleon', 'WATER', 2 UNION ALL
SELECT RANDOM_UUID(), 'seed-charizard', 'WATER', 2 UNION ALL
SELECT RANDOM_UUID(), 'seed-pikachu-ex', 'FIGHTING', 2;
