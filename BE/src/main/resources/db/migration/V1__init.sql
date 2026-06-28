CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(120) UNIQUE,
    password VARCHAR(255),
    role VARCHAR(30) NOT NULL DEFAULT 'PLAYER',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE players (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE REFERENCES users(id),
    display_name VARCHAR(80) NOT NULL,
    avatar_url VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE player_stats (
    player_id UUID PRIMARY KEY REFERENCES players(id),
    total_wins INTEGER NOT NULL DEFAULT 0,
    total_losses INTEGER NOT NULL DEFAULT 0,
    current_win_streak INTEGER NOT NULL DEFAULT 0,
    max_win_streak INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE cards (
    id VARCHAR(80) PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    supertype VARCHAR(30) NOT NULL,
    subtypes TEXT,
    set_code VARCHAR(30) NOT NULL,
    number VARCHAR(30),
    rarity VARCHAR(80),
    image_small_url TEXT,
    image_large_url TEXT,
    hp INTEGER,
    pokemon_stage VARCHAR(30),
    evolves_from VARCHAR(160),
    evolves_to TEXT,
    abilities TEXT,
    pokemon_types TEXT,
    retreat_cost TEXT,
    converted_retreat_cost INTEGER,
    is_ex BOOLEAN NOT NULL DEFAULT FALSE,
    is_mega BOOLEAN NOT NULL DEFAULT FALSE,
    energy_card_type VARCHAR(30),
    provides_energy_types TEXT,
    trainer_subtype VARCHAR(30),
    effect_code VARCHAR(60),
    is_ace_spec BOOLEAN NOT NULL DEFAULT FALSE,
    strategy_key VARCHAR(30),
    rules_text TEXT,
    raw_json TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE card_attacks (
    id UUID PRIMARY KEY,
    card_id VARCHAR(80) NOT NULL REFERENCES cards(id),
    attack_index INTEGER NOT NULL,
    name VARCHAR(160) NOT NULL,
    printed_cost TEXT,
    converted_energy_cost INTEGER NOT NULL DEFAULT 0,
    damage_text VARCHAR(40),
    base_damage INTEGER,
    effect_text TEXT,
    effect_code VARCHAR(500),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE card_weaknesses (
    id UUID PRIMARY KEY,
    card_id VARCHAR(80) NOT NULL REFERENCES cards(id),
    energy_type VARCHAR(30) NOT NULL,
    multiplier INTEGER NOT NULL DEFAULT 2
);

CREATE TABLE card_resistances (
    id UUID PRIMARY KEY,
    card_id VARCHAR(80) NOT NULL REFERENCES cards(id),
    energy_type VARCHAR(30) NOT NULL,
    "value" INTEGER NOT NULL DEFAULT -20
);

CREATE TABLE decks (
    id UUID PRIMARY KEY,
    owner_player_id UUID REFERENCES players(id),
    name VARCHAR(120) NOT NULL,
    source VARCHAR(30) DEFAULT 'USER',
    valid BOOLEAN NOT NULL DEFAULT FALSE,
    main_card_id VARCHAR(80),
    validation_errors TEXT DEFAULT '[]',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE deck_cards (
    id UUID PRIMARY KEY,
    deck_id UUID NOT NULL REFERENCES decks(id),
    card_id VARCHAR(80) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE matches (
    id UUID PRIMARY KEY,
    status VARCHAR(30) NOT NULL,
    current_phase VARCHAR(30),
    turn_number INTEGER NOT NULL DEFAULT 0,
    current_player_id UUID,
    first_player_id UUID,
    winner_player_id UUID,
    finish_reason VARCHAR(60),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    hand_size INTEGER DEFAULT 7,
    latest_state_version BIGINT,
    last_resumed_player_id UUID
);

CREATE TABLE match_players (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id),
    player_id UUID NOT NULL,
    player_kind VARCHAR(20) NOT NULL,
    side VARCHAR(30) NOT NULL,
    deck_id UUID,
    display_name VARCHAR(80) NOT NULL,
    joined_at TIMESTAMP NOT NULL
);

CREATE TABLE match_states (
    id UUID PRIMARY KEY,
    match_id UUID NOT NULL REFERENCES matches(id),
    version BIGINT NOT NULL,
    serialized_state TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
