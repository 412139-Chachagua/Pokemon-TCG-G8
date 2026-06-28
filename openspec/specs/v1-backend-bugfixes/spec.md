## ADDED Requirements

### Requirement: Game state returned with every action response
The system SHALL return the updated public and private game state in every action response, so the client can render the board without a separate state fetch.

#### Scenario: Action response includes public state
- WHEN a game action is executed via `POST /api/matches/{id}/actions`
- THEN the response SHALL include a non-null `publicState` with the full game state visible to both players
- AND `privateState` SHALL include the acting player's hidden information (hand, prizes)

#### Scenario: After DRAW_CARD, hand size is reflected
- WHEN a player executes DRAW_CARD
- THEN the response `privateState.hand` SHALL contain the drawn card
- AND `privateState.handCount` SHALL reflect the new hand size

### Requirement: State locking for concurrent access
The system SHALL prevent concurrent modification of the same match state by two simultaneous requests.

#### Scenario: Sequential requests are safe
- WHEN two concurrent `POST /api/matches/{id}/actions` requests arrive for the same match
- THEN one SHALL succeed and the other SHALL fail with HTTP 409 Conflict
- AND the match state SHALL NOT be corrupted

### Requirement: Entered turn number set for bench Pokemon
When a Pokemon is placed on the bench, the system SHALL set its `enteredTurnNumber` to the current turn number.

#### Scenario: Basic placed on bench records turn number
- WHEN a player places a Basic Pokemon on the bench via PUT_BASIC_ON_BENCH
- THEN the new `PokemonInPlay.enteredTurnNumber` SHALL equal `state.getTurnNumber()`

#### Scenario: Evolution blocked on same-turn bench
- WHEN a Pokemon placed on the bench this turn is targeted for evolution
- THEN the validator SHALL reject the evolution because `enteredTurnNumber == turnNumber`

### Requirement: DrawCard rate limited per turn
The system SHALL enforce that a player can only draw cards once per turn via the DRAW_CARD action.

#### Scenario: First draw succeeds
- WHEN a player executes DRAW_CARD
- THEN the player draws the top card(s) of their deck
- AND `TurnFlags.hasDrawnForTurn` SHALL be set to true

#### Scenario: Second draw blocked
- WHEN a player who has already drawn this turn tries to DRAW_CARD again
- THEN the RuleValidator SHALL reject the action

### Requirement: RuleValidator validates pending decisions
The system SHALL validate that an action's preconditions exist before allowing it, specifically `TAKE_PRIZE_CARD` and `CHOOSE_KNOCKOUT_REPLACEMENT`.

#### Scenario: Take prize blocked without pending KO
- WHEN a player executes TAKE_PRIZE_CARD with no pending KO
- THEN the RuleValidator SHALL reject the action

#### Scenario: Choose knockout replacement blocked without KO
- WHEN a player executes CHOOSE_KNOCKOUT_REPLACEMENT with no Pokemon to replace
- THEN the RuleValidator SHALL reject the action

### Requirement: VictoryConditionChecker detects opponent deck-out
The system SHALL check if either player's deck is empty, not just the current player's.

#### Scenario: Opponent deck-out triggers victory
- WHEN a player draws the last card from their deck
- AND the opponent has no cards left in their deck
- THEN the victory condition SHALL be detected
- AND the opponent SHALL be declared the winner

### Requirement: Card sync runs asynchronously
The system SHALL NOT block application startup while synchronizing the card catalog from the external API.

#### Scenario: Startup completes even if API is unreachable
- WHEN the application starts
- AND the Pokemon TCG API is unreachable
- THEN the application SHALL finish starting and accept HTTP requests
- AND the sync SHALL be retried or logged as a warning

### Requirement: Card cache invalidated on resync
The system SHALL clear the card lookup cache when the catalog is re-synchronized.

#### Scenario: After sync, new cards are visible
- WHEN a manual sync is triggered via `POST /api/cards/sync`
- THEN any subsequent `getCardById()` call SHALL return the updated data
- AND stale cached entries SHALL NOT be served

### Requirement: Energy type validation for retreat cost
The system SHALL validate that the discarded energy cards match the required types specified in the retreat cost.

#### Scenario: Correct energy types discarded
- WHEN a Pokemon with retreat cost `[Water, Colorless]` retreats
- AND the player discards one Water energy and one Colorless energy
- THEN the retreat SHALL succeed

#### Scenario: Wrong energy types discarded
- WHEN a Pokemon with retreat cost `[Water, Colorless]` retreats
- AND the player discards two Colorless energies (no Water)
- THEN the RuleValidator SHALL reject the retreat

### Requirement: Handler returns error when attack energy is insufficient
When `DeclareAttackHandler` detects insufficient energy for the declared attack, it SHALL return an error rather than silently succeeding.

#### Scenario: Attack with insufficient energy returns error
- WHEN a player declares an attack with a Pokemon that lacks the required energy
- THEN the handler SHALL NOT apply any damage
- AND the action result SHALL indicate failure with an appropriate error message

### Requirement: Remove dead code
Classes that are no longer used or are empty stubs SHALL be removed to reduce maintenance burden.

#### Scenario: Empty/stub classes removed
- WHEN the codebase is compiled after cleanup
- THEN the following classes SHALL NOT exist: `GamePhase.java`, `StatusEffectManager.java`, `AttackStep.java`, `GameMetadata.java`, `VictoryResult.java`
- AND the Payload DTO classes (`AttachEnergyPayload`, `DeclareAttackPayload`, etc.) SHALL be removed

#### Scenario: USE_ABILITY action removed or handled
- WHEN a client sends a USE_ABILITY action
- THEN the system SHALL return "UNKNOWN_ACTION" error (handler not implemented)
- OR the `USE_ABILITY` enum value SHALL be removed from `GameActionType`

### Requirement: PlayTrainerHandler applies basic trainer effects
Trainer cards SHALL have basic effects implemented for Items and Supporters, even if complex card-specific effects remain unimplemented.

#### Scenario: Item trainer is discarded without effect
- WHEN a player plays an Item-type Trainer card
- THEN the card SHALL move from hand to discard pile
- AND `hasPlayedSupporter` flag SHALL NOT be set (Item is not Supporter)

#### Scenario: Supporter trainer is limited to one per turn
- WHEN a player plays a Supporter-type Trainer card
- THEN `hasPlayedSupporter` SHALL be set to true
- AND a second Supporter played this turn SHALL be rejected by RuleValidator

## MODIFIED Requirements

### Requirement: PutBasicOnBenchHandler compiles and works (was BROKEN)
The handler SHALL compile and execute correctly, referencing the declared `payload` variable instead of an undefined one.

#### Scenario: Basic Pokemon placed on bench
- WHEN a player executes PUT_BASIC_ON_BENCH with a valid Basic Pokemon in hand
- THEN the card SHALL move from hand to the player's bench
- AND the new `PokemonInPlay` SHALL have `enteredTurnNumber` set to `state.getTurnNumber()`
- AND `specialConditions` SHALL be initialized as empty list
- AND `attachedEnergies` SHALL be initialized as empty list
- AND `evolvedThisTurn` SHALL be false

### Requirement: DeclareAttackHandler KO only applies to active defender (was BROKEN)
The system SHALL only nullify the opponent's active Pokemon when the defender IS the active Pokemon. Attacking a bench Pokemon SHALL NOT remove the active.

#### Scenario: Attack on bench Pokemon does not remove active
- WHEN a player attacks an opponent's bench Pokemon
- AND the attack would KO that bench Pokemon
- THEN only the bench Pokemon SHALL be KO'd
- AND `opponent.setActivePokemon(null)` SHALL NOT be called
- AND the opponent's active Pokemon SHALL remain unchanged

### Requirement: SeedDeckService uses valid card IDs (was BROKEN)
The seed decks SHALL reference card IDs that actually exist in the synced catalog.

#### Scenario: Seed Fire Deck references valid cards
- WHEN SeedDeckService runs after catalog sync
- THEN the card IDs in seed decks SHALL match real card IDs from the API (e.g., `xy1-10`, `xy1-1`)
- AND seed decks SHALL be created successfully without FK constraint violations

### Requirement: DeckMapper uses setters/constructors instead of reflection (was RISKY)
The DeckMapper SHALL use standard Java constructors, setters, or builders instead of `getDeclaredField` + `setAccessible`.

#### Scenario: Deck domain object constructed via setters
- WHEN `DeckMapper.toDomain()` is called
- THEN it SHALL use public setters or constructors
- AND it SHALL NOT use `Field.setAccessible(true)`

### Requirement: FE/BE contract alignment (was MISMATCHED)
The backend API response formats SHALL be updated to match what the frontend services expect, to ensure integration works when the FE is implemented.

#### Scenario: Card search returns expected format
- WHEN `GET /api/cards` returns results
- THEN the response format SHALL include `items`, `page`, `size`, `totalItems` (not Spring's `content`, `totalElements`)

#### Scenario: Match creation accepts expected payload
- WHEN `POST /api/matches` receives `{ playerName, deckId }`
- THEN the backend SHALL accept it and create a match with one player (WAITING status)
- AND the response SHALL include the generated playerId

#### Scenario: Match creation with direct second player
- WHEN `POST /api/matches` receives `{ player1Name, player1DeckId, player2Name, player2DeckId }`
- THEN the backend SHALL accept the legacy format as well (backward compatible)

### Requirement: WebSocket publishes private state to /queue (was MISSING)
The system SHALL publish private player state to `/queue/matches/{matchId}/{playerId}` so that each player receives their own hidden information.

#### Scenario: Player receives private events
- WHEN an action modifies a player's hand
- THEN an event SHALL be published to `/queue/matches/{matchId}/{playerId}` with the updated `PrivatePlayerState`
- AND events on `/topic/matches/{matchId}/events` SHALL NOT include private data
