## ADDED Requirements

### Requirement: validateDrawCard enforces hasDrawnForTurn
The system SHALL reject DRAW_CARD if the player has already drawn cards this turn.

#### Scenario: First draw succeeds
- WHEN a player executes DRAW_CARD
- AND `TurnFlags.hasDrawnForTurn` is false
- THEN the validator SHALL return true

#### Scenario: Second draw rejected
- WHEN a player executes DRAW_CARD
- AND `TurnFlags.hasDrawnForTurn` is true
- THEN the validator SHALL return false

### Requirement: validateTakePrizeCard verifies pending KO
The system SHALL reject TAKE_PRIZE_CARD if no pending prize is owed.

#### Scenario: Take prize with pending KO
- WHEN a player executes TAKE_PRIZE_CARD
- AND `GameState.pendingPrizeOwnerPlayerId` matches the requesting player
- THEN the validator SHALL return true

#### Scenario: Take prize without pending KO
- WHEN a player executes TAKE_PRIZE_CARD
- AND `GameState.pendingPrizeOwnerPlayerId` is null
- THEN the validator SHALL return false

### Requirement: validateChooseKnockoutReplacement verifies pending KO
The system SHALL reject CHOOSE_KNOCKOUT_REPLACEMENT if no Pokemon needs to be replaced.

#### Scenario: Choose replacement with pending KO
- WHEN a player executes CHOOSE_KNOCKOUT_REPLACEMENT
- AND the opponent's active Pokemon is null and bench is not empty
- THEN the validator SHALL return true

#### Scenario: Choose replacement without pending KO
- WHEN a player executes CHOOSE_KNOCKOUT_REPLACEMENT
- AND the opponent's active Pokemon is not null
- THEN the validator SHALL return false

### Requirement: validateRetreat validates energy types
The system SHALL validate that the discarded energy cards match the required types in the retreat cost.

#### Scenario: Correct energy types for retreat
- WHEN a Pokemon with retreat cost `[Water, Colorless]` retreats
- AND player discards energies matching the required types
- THEN the validator SHALL return true

#### Scenario: Wrong energy types for retreat
- WHEN a Pokemon with retreat cost `[Water, Colorless]` retreats
- AND player discards energies that do not include a Water type
- THEN the validator SHALL return false

## MODIFIED Requirements

### Requirement: DeclareAttackHandler KO targets correct defender
The system SHALL only nullify the opponent's active Pokemon when the defender IS the active Pokemon.

#### Scenario: Attack on bench Pokemon KOs only the bench
- WHEN a player attacks an opponent's bench Pokemon
- AND the attack would KO that bench Pokemon
- THEN only the bench Pokemon SHALL be removed from play
- AND `opponent.setActivePokemon(null)` SHALL NOT be called
- AND the opponent's active Pokemon SHALL remain unchanged

#### Scenario: Attack on active Pokemon KOs normally
- WHEN a player attacks the opponent's active Pokemon
- AND the attack would KO the active Pokemon
- THEN the active Pokemon SHALL be removed from play
- AND energies attached to it SHALL move to discard

### Requirement: VictoryConditionChecker detects deck-out for both players
The system SHALL check if either player's deck is empty, not just the current player's.

#### Scenario: Opponent deck-out triggers victory
- WHEN a player draws the last card from their deck
- AND the opponent has no cards left in their deck
- THEN the victory condition SHALL be detected
- AND the opponent SHALL be declared the winner

### Requirement: ActionResult returns populated game state
The system SHALL return non-null publicState and privateState in every action response.

#### Scenario: Action response includes full state
- WHEN any game action is executed via `POST /api/matches/{id}/actions`
- THEN the response SHALL include `publicState` with public game state
- AND `privateState` with the acting player's hidden information

### Requirement: PutBasicOnBenchHandler sets enteredTurnNumber
When a Pokemon is placed on the bench, the system SHALL set its `enteredTurnNumber` to the current turn number.

#### Scenario: Placed Pokemon records current turn
- WHEN a player places a Basic Pokemon on the bench via PUT_BASIC_ON_BENCH
- THEN the new `PokemonInPlay.enteredTurnNumber` SHALL equal `state.getTurnNumber()`
- AND `specialConditions` SHALL be initialized as empty list
- AND `attachedEnergies` SHALL be initialized as empty list

### Requirement: DeclareAttackHandler returns error on insufficient energy
When `AttackResolver` detects insufficient energy, the handler SHALL return an error rather than silently succeeding.

#### Scenario: Insufficient energy returns error
- WHEN a player declares an attack with a Pokemon that lacks the required energy
- THEN the handler SHALL NOT apply any damage
- AND the ActionResult SHALL indicate failure with error message "INSUFFICIENT_ENERGY"
