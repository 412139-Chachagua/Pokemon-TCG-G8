## ADDED Requirements

### Requirement: TurnManager implements turn flow
`TurnManager` SHALL implement `startTurn(EngineContext)` and `endTurn(EngineContext)` covering draw, phase transitions, flag resets, and player switching, in strict compliance with `06-game-state-contract.md`, `07-setup-flow-contract.md`, and the existing `turn-manager-spec.md`.

#### Scenario: startTurn draws one card on a normal turn
- **GIVEN** a `GameState` where `currentPlayerId != firstPlayerId || turnNumber > 1`
- **WHEN** `startTurn(ctx)` is called
- **THEN** the top card of the player's deck SHALL be removed and added to their hand
- **AND** `hasDrawnForTurn` SHALL be set to `true`
- **AND** `phase` SHALL change to `TurnPhase.MAIN`

#### Scenario: first player does not draw on turn 1
- **GIVEN** a `GameState` where `currentPlayerId == firstPlayerId && turnNumber == 1`
- **WHEN** `startTurn(ctx)` is called
- **THEN** no card SHALL be drawn
- **AND** `hasDrawnForTurn` SHALL remain `false`
- **AND** `phase` SHALL change to `TurnPhase.MAIN`

#### Scenario: startTurn resets all TurnFlags
- **GIVEN** any `GameState` with some `TurnFlags` set to `true`
- **WHEN** `startTurn(ctx)` is called
- **THEN** all six `TurnFlags` (`hasDrawnForTurn`, `hasAttachedEnergy`, `hasRetreated`, `hasPlayedSupporter`, `hasPlayedStadium`, `hasAttacked`) SHALL be `false`

#### Scenario: empty deck logs an event without setting winnerPlayerId
- **GIVEN** a `GameState` where the active player's deck is empty
- **WHEN** `startTurn(ctx)` is called
- **THEN** an event SHALL be registered via `ctx.addEvent()` indicating the player cannot draw
- **AND** `state.winnerPlayerId` SHALL NOT be modified

#### Scenario: endTurn switches player, increments turn, and chains to startTurn
- **GIVEN** any `GameState`
- **WHEN** `endTurn(ctx)` is called
- **THEN** `phase` SHALL change to `TurnPhase.BETWEEN_TURNS`
- **AND** `currentPlayerId` SHALL be set to the other player's ID (searched by `playerId`, not by index)
- **AND** `turnNumber` SHALL be incremented by 1
- **AND** `startTurn(ctx)` SHALL be called for the new player
