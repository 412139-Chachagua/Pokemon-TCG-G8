## ADDED Requirements

### Requirement: TakePrizeCardHandler validates KO ownership

`TakePrizeCardHandler` SHALL verify that the player requesting the prize is the one who made the KO, using the `pendingPrizeOwnerPlayerId` field in `GameState`. If the player ID does not match, the handler SHALL reject the action silently (no state mutation, no event).

#### Scenario: Player who made KO takes prize

- **GIVEN** `DeclareAttackHandler` has set `pendingPrizeOwnerPlayerId` to player A after a KO
- **WHEN** player A sends `TAKE_PRIZE_CARD` action
- **THEN** `TakePrizeCardHandler` allows the action, moves the prize card to hand, and clears `pendingPrizeOwnerPlayerId`

#### Scenario: Other player tries to take prize

- **GIVEN** `pendingPrizeOwnerPlayerId` is set to player A after a KO
- **WHEN** player B sends `TAKE_PRIZE_CARD` action
- **THEN** `TakePrizeCardHandler` rejects the action without mutating state

### Requirement: DeclareAttackHandler sets pendingPrizeOwnerPlayerId

When `DeclareAttackHandler` detects a KO, it SHALL set `state.setPendingPrizeOwnerPlayerId(player.getPlayerId())` on the `GameState` before emitting events.

#### Scenario: KO sets pending prize owner

- **GIVEN** player A's attack KOs the defending Pokémon
- **WHEN** `DeclareAttackHandler.handle()` processes the KO
- **THEN** `GameState.pendingPrizeOwnerPlayerId` is set to player A's ID

### Requirement: pendingPrizeOwnerPlayerId resets on new turn

`TurnManager.startTurn()` or `TurnManager.resetTurnFlags()` SHALL reset `pendingPrizeOwnerPlayerId` to `null` at the start of each turn.

#### Scenario: Pending prize owner resets

- **GIVEN** `pendingPrizeOwnerPlayerId` is set to player A
- **WHEN** a new turn starts
- **THEN** `pendingPrizeOwnerPlayerId` is `null`

### Requirement: RetreatActiveHandler sets enteredTurnNumber

When `RetreatActiveHandler` moves the active Pokémon to the bench, it SHALL set `enteredTurnNumber = state.getTurnNumber()` on that Pokémon.

#### Scenario: Retreat sets enteredTurnNumber on benched Pokémon

- **GIVEN** a Pokémon with `enteredTurnNumber = 0` is the active Pokémon
- **WHEN** a retreat action is executed
- **THEN** after the retreat, that Pokémon's `enteredTurnNumber` equals the current turn number

### Requirement: GameState includes pendingPrizeOwnerPlayerId

`GameState` SHALL include a new field `pendingPrizeOwnerPlayerId` of type `UUID`.

#### Scenario: Field exists and compiles

- **WHEN** the project compiles
- **THEN** `GameState.getPendingPrizeOwnerPlayerId()` and `GameState.setPendingPrizeOwnerPlayerId(UUID)` are available
