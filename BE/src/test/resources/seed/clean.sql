-- Clean all test data before each integration test
DELETE FROM match_states;
DELETE FROM match_players;
DELETE FROM matches;
DELETE FROM deck_cards;
DELETE FROM decks;
DELETE FROM players;
DELETE FROM users;
DELETE FROM card_weaknesses;
DELETE FROM card_resistances;
DELETE FROM card_attacks;
DELETE FROM cards;
