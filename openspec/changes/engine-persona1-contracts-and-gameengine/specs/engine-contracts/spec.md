## ADDED Requirements

### Requirement: Engine package is pure Java 21

The `engine/` package SHALL NOT import any Spring annotation (`@Service`, `@Repository`, `@Autowired`, `@Entity`, `@RestController`), JPA classes, REST controllers, WebSocket classes, or repository classes.

All classes in `engine/` SHALL reside under `ar.edu.utn.frc.tup.piii.engine.*`.

#### Scenario: Engine has no Spring imports

- **WHEN** compiling `engine/` package
- **THEN** no class in `engine/` SHALL contain imports from `org.springframework`, `javax.persistence`, `jakarta.persistence`, `ar.edu.utn.frc.tup.piii.repositories`, `ar.edu.utn.frc.tup.piii.services`, or `ar.edu.utn.frc.tup.piii.controllers`

### Requirement: Game state models match contract 06

`GameState`, `PlayerState`, `PokemonInPlay`, `TurnFlags` SHALL contain exactly the fields defined in contract 06.

#### Scenario: GameState has required fields

- **WHEN** inspecting `GameState` class
- **THEN** it SHALL have fields: `matchId` (UUID), `status` (MatchStatus), `phase` (TurnPhase), `turnNumber` (int), `currentPlayerId` (UUID), `firstPlayerId` (UUID), `players` (List of PlayerState, size 2), `stadiumCardInstanceId` (UUID, nullable), `turnFlags` (TurnFlags), `pendingDecision` (PendingDecision or similar, nullable), `winnerPlayerId` (UUID, nullable), `finishReason` (FinishReason, nullable), `createdAt` (Instant), `updatedAt` (Instant)

#### Scenario: PlayerState has required fields

- **WHEN** inspecting `PlayerState` class
- **THEN** it SHALL have fields: `playerId` (UUID), `side` (PlayerSide), `deck` (List of CardInstance), `hand` (List of CardInstance), `prizes` (List of CardInstance), `discard` (List of CardInstance), `activePokemon` (PokemonInPlay, nullable), `bench` (List of PokemonInPlay), `mulliganCount` (int)

#### Scenario: PokemonInPlay has required fields

- **WHEN** inspecting `PokemonInPlay` class
- **THEN** it SHALL have fields: `instanceId` (UUID), `cardDefinitionId` (String), `ownerPlayerId` (UUID), `enteredTurnNumber` (int), `evolvedThisTurn` (boolean), `damageCounters` (int), `specialConditions` (List of SpecialCondition), `attachedEnergies` (List of CardInstance), `toolCardInstanceId` (UUID, nullable)

#### Scenario: TurnFlags has required fields

- **WHEN** inspecting `TurnFlags` class
- **THEN** it SHALL have fields: `hasDrawnForTurn` (boolean), `hasAttachedEnergy` (boolean), `hasRetreated` (boolean), `hasPlayedSupporter` (boolean), `hasPlayedStadium` (boolean), `hasAttacked` (boolean)

### Requirement: CardInstance represents a physical card copy

`CardInstance` SHALL have exactly two fields: `instanceId` (UUID, unique per match) and `cardDefinitionId` (String, references card catalog).

#### Scenario: CardInstance is constructable

- **WHEN** creating a `new CardInstance(UUID.randomUUID(), "xy1-1")`
- **THEN** it SHALL store both values in the corresponding fields

### Requirement: GameMetadata and GamePhase exist

`GameMetadata` SHALL exist as a class in `engine/model/`. `GamePhase` SHALL exist as an enum (alias of `TurnPhase`) in `engine/model/`.

#### Scenario: GameMetadata compiles

- **WHEN** compiling the engine package
- **THEN** `GameMetadata` SHALL compile without errors

#### Scenario: GamePhase is an enum

- **WHEN** inspecting `GamePhase`
- **THEN** it SHALL be an enum with values matching `TurnPhase`: `DRAW`, `MAIN`, `ATTACK`, `BETWEEN_TURNS`

### Requirement: Action types match contract 03

`GameActionType` SHALL contain exactly: `PUT_BASIC_ON_BENCH`, `ATTACH_ENERGY`, `EVOLVE_POKEMON`, `PLAY_TRAINER`, `RETREAT_ACTIVE`, `DECLARE_ATTACK`, `END_TURN`. Values `DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, `USE_ABILITY` SHALL be removed or marked `@Deprecated`.

#### Scenario: GameActionType has canonical values

- **WHEN** inspecting `GameActionType.values()`
- **THEN** the set SHALL contain exactly the 7 canonical values
- **AND** any non-canonical value SHALL be removed or annotated with `@Deprecated`

### Requirement: GameAction and GameActionPayload follow contract 08

`GameAction` SHALL contain `type` (GameActionType), `playerId` (UUID), `payload` (GameActionPayload), and `clientRequestId` (String). `GameActionPayload` SHALL be a marker interface.

#### Scenario: GameAction is constructable

- **WHEN** creating a `GameAction`
- **THEN** all four fields SHALL be accessible via getters

#### Scenario: Each action type has its own payload class

- **WHEN** inspecting action payload classes
- **THEN** `AttachEnergyPayload`, `DeclareAttackPayload`, `RetreatPayload`, `PlayTrainerPayload`, `EvolvePokemonPayload`, `PutBasicOnBenchPayload` SHALL each implement `GameActionPayload`
- **AND** each SHALL contain fields matching the JSON payload in contract 08

### Requirement: ActionResult matches contract 08

`ActionResult` SHALL contain `success` (boolean), `events` (List of String), `error` (GameError, nullable).

#### Scenario: ActionResult success path

- **WHEN** creating a successful `ActionResult`
- **THEN** `isSuccess()` SHALL return true, `events` SHALL contain the event list, `error` SHALL be null

#### Scenario: ActionResult error path

- **WHEN** creating a failed `ActionResult`
- **THEN** `isSuccess()` SHALL return false, `error` SHALL contain a `GameError` with code and message

### Requirement: GameError has code and message

`GameError` SHALL contain `code` (String) and `message` (String) and `details` (Map of String to Object, nullable).

#### Scenario: GameError is constructable

- **WHEN** creating a `new GameError("NOT_YOUR_TURN", "No es tu turno.")`
- **THEN** both fields SHALL be accessible via getters

### Requirement: ErrorCode enum matches contract 09

`ErrorCode` SHALL be an enum with at least: `NOT_YOUR_TURN`, `WRONG_PHASE`, `MATCH_NOT_ACTIVE`, `ENERGY_ALREADY_ATTACHED`, `BENCH_FULL`, `INSUFFICIENT_ENERGY`, `CANNOT_ATTACK_FIRST_TURN`, `POKEMON_ASLEEP`, `POKEMON_PARALYZED`, `RETREAT_ALREADY_USED`, `SUPPORTER_ALREADY_PLAYED`, `EVOLVE_NOT_ALLOWED`, `CARD_NOT_IN_HAND`, `INVALID_TARGET`, `KNOCKOUT_REPLACEMENT_REQUIRED`.

#### Scenario: ErrorCode contains all required values

- **WHEN** inspecting `ErrorCode.values()`
- **THEN** it SHALL contain all the enumerated error codes

### Requirement: EngineContext wraps GameState and ports

`EngineContext` SHALL provide access to mutable `GameState`, accumulated `events` (List of String), `CardLookupPort`, and `RandomizerPort`. Events SHALL be added via `addEvent(String)` and retrieved as an unmodifiable list via `getEvents()`.

#### Scenario: EngineContext is constructable

- **WHEN** creating an `EngineContext` with a `GameState`, `CardLookupPort`, and `RandomizerPort`
- **THEN** `getState()` SHALL return the same state instance
- **AND** `getEvents()` SHALL return an empty list
- **AND** events added via `addEvent()` SHALL appear in `getEvents()`

### Requirement: ActionHandler interface

`ActionHandler` SHALL be an interface in `engine/handlers/` with method `void handle(EngineContext ctx)`.

#### Scenario: ActionHandler compiles

- **WHEN** compiling the handler interface
- **THEN** it SHALL be a valid Java interface

### Requirement: GameEngine.applyAction() follows 11-step flow

`GameEngine.applyAction(matchId, playerId, action)` SHALL execute in this exact order:

1. Load `GameState` via `StatePersisterPort.loadState(matchId)`
2. Verify match status is `ACTIVE`; else return error
3. Verify `playerId` equals `currentPlayerId`; else return error
4. Create `EngineContext` with loaded state and ports
5. Validate via `RuleValidator.validate(ctx, action)`; if invalid return `ActionResult` with error
6. Dispatch to the correct `ActionHandler` based on `action.getType()`
7. Check victory via `VictoryConditionChecker.check(ctx)`
8. If winner found: set `winnerPlayerId`, `finishReason`, change status to `FINISHED`
9. Persist via `StatePersisterPort.saveState(matchId, state)`
10. Publish events via `EventPublisherPort`
11. Return `ActionResult` with `success=true`, events, and public/private state

#### Scenario: applyAction returns error for inactive match

- **WHEN** calling `applyAction(matchId, playerId, action)` with a match whose status is not `ACTIVE`
- **THEN** the result SHALL have `success=false` and `error.code` equal to `MATCH_NOT_ACTIVE`

#### Scenario: applyAction returns error for wrong turn

- **WHEN** calling `applyAction(matchId, playerId, action)` where `playerId` is not the current player
- **THEN** the result SHALL have `success=false` and `error.code` equal to `NOT_YOUR_TURN`

### Requirement: Ports exist with correct signatures

`CardLookupPort`, `RandomizerPort`, `StatePersisterPort`, `EventPublisherPort` SHALL exist as interfaces in `engine/ports/`.

#### Scenario: StatePersisterPort has loadState and saveState

- **WHEN** inspecting `StatePersisterPort`
- **THEN** it SHALL declare `Optional<GameState> loadState(UUID matchId)` and `void saveState(UUID matchId, GameState state)`

### Requirement: Stub classes compile

`RuleValidator`, `SetupManager`, `VictoryConditionChecker`, `TurnManager` SHALL exist as stubs that compile without errors. `RuleValidator.validate()` SHALL always return valid. `VictoryConditionChecker.check()` SHALL always return empty.

#### Scenario: Stubs compile

- **WHEN** running `mvn compile` in `BE/`
- **THEN** all stub classes SHALL compile without errors

### Requirement: PlayerSide and SpecialCondition match contract 03

`PlayerSide` SHALL have `PLAYER_ONE` and `PLAYER_TWO`. `SpecialCondition` SHALL have `ASLEEP`, `BURNED`, `CONFUSED`, `PARALYZED`, `POISONED`.

#### Scenario: PlayerSide has canonical values

- **WHEN** inspecting `PlayerSide.values()`
- **THEN** it SHALL contain exactly `PLAYER_ONE` and `PLAYER_TWO`

#### Scenario: SpecialCondition has canonical values

- **WHEN** inspecting `SpecialCondition.values()`
- **THEN** it SHALL contain exactly `ASLEEP`, `BURNED`, `CONFUSED`, `PARALYZED`, `POISONED`
