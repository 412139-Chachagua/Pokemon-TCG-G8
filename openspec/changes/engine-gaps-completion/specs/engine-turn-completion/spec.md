## ADDED Requirements

### Requirement: TurnManager provides advancePhase method
The system SHALL provide an `advancePhase(GameState)` method in TurnManager that advances the game phase sequentially.

#### Scenario: Advance from DRAW to MAIN
- **WHEN** `advancePhase` is called with phase set to DRAW
- **THEN** the phase SHALL change to MAIN

#### Scenario: Advance from MAIN to ATTACK
- **WHEN** `advancePhase` is called with phase set to MAIN
- **THEN** the phase SHALL change to ATTACK

#### Scenario: Advance from ATTACK to BETWEEN_TURNS
- **WHEN** `advancePhase` is called with phase set to ATTACK
- **THEN** the phase SHALL change to BETWEEN_TURNS

#### Scenario: No-op at BETWEEN_TURNS
- **WHEN** `advancePhase` is called with phase set to BETWEEN_TURNS
- **THEN** the phase SHALL remain BETWEEN_TURNS

### Requirement: TurnManager resets evolvedThisTurn on startTurn
The system SHALL reset `evolvedThisTurn` flag on all Pokémon in the current player's active and bench when a new turn starts.

#### Scenario: Reset after evolution
- **WHEN** `startTurn` is called
- **THEN** all PokémonInPlay in the current player's active and bench SHALL have `evolvedThisTurn = false`

### Requirement: TurnManager auto-resolves DRAW phase
The system SHALL automatically draw a card when the turn enters DRAW phase, without requiring a DRAW_CARD action from the player.

#### Scenario: Draw card from non-empty deck
- **WHEN** `startTurn` sets phase to DRAW and the player's deck is not empty
- **THEN** the top card SHALL move from deck to hand and `hasDrawnForTurn` SHALL be set to true

#### Scenario: Deck-out on empty deck
- **WHEN** `startTurn` sets phase to DRAW and the player's deck is empty
- **THEN** a deck-out event SHALL be generated and victory SHALL be checked
