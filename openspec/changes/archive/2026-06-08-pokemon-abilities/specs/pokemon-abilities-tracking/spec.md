## ADDED Requirements

### Requirement: PokemonInPlay abilities tracking
The system SHALL add `Set<String> abilitiesUsedThisTurn` to `PokemonInPlay` to track which abilities each Pokemon instance has used during the current turn.

#### Scenario: New Pokemon starts with empty set
- **WHEN** a Pokemon enters play (placed on bench, becomes active, or evolves)
- **THEN** `abilitiesUsedThisTurn` SHALL be an empty set

#### Scenario: Ability usage registered
- **WHEN** a Pokemon successfully uses an ability
- **THEN** the ability name SHALL be added to `abilitiesUsedThisTurn`

#### Scenario: Ability usage checked
- **WHEN** a USE_ABILITY action is validated
- **THEN** the system SHALL check if abilityName is in `abilitiesUsedThisTurn` and reject if present

### Requirement: TurnManager resets abilities
The system SHALL modify `TurnManager.startTurn()` to clear `abilitiesUsedThisTurn` for all Pokemon (active + bench) of the current player.

#### Scenario: Abilities reset at turn start
- **WHEN** `TurnManager.startTurn()` is called
- **THEN** all Pokemon belonging to the current player SHALL have their `abilitiesUsedThisTurn` cleared

#### Scenario: Active Pokemon reset
- **WHEN** a new turn starts
- **THEN** the active Pokemon's `abilitiesUsedThisTurn` SHALL be cleared

#### Scenario: Bench Pokemon reset
- **WHEN** a new turn starts
- **THEN** all bench Pokemon's `abilitiesUsedThisTurn` SHALL be cleared

### Requirement: Multiple copy independence
The system SHALL correctly handle multiple copies of the same Pokemon card with independent ability tracking.

#### Scenario: Two Greninja instances
- **WHEN** Greninja A uses Water Shuriken and Greninja B has not
- **THEN** Greninja A SHALL NOT be able to use Water Shuriken again this turn, but Greninja B SHALL be able to use it

### Requirement: Cleanup on leaving play
When a Pokemon leaves play (KO, retreat to bench, discard), its `abilitiesUsedThisTurn` SHALL be discarded with the instance.

#### Scenario: KO removes tracking
- **WHEN** a Pokemon is Knocked Out
- **THEN** its `abilitiesUsedThisTurn` data SHALL be removed from the game state
