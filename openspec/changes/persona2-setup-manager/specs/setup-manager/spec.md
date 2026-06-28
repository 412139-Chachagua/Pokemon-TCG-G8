## ADDED Requirements

### Requirement: SetupManager constructor accepts ports
The `SetupManager` SHALL accept its dependencies via a single public constructor with three parameters: `DeckLoadPort`, `CardLookupPort`, `RandomizerPort`. No Spring annotations, no `@Autowired`, no default constructor.

Source of truth: `engine-persona2-master-spec.md` (constructor injection rule), `setup-manager-spec.md` (Constructor section).

#### Scenario: SetupManager instantiated with ports
- **WHEN** `new SetupManager(deckLoadPort, cardLookupPort, randomizerPort)` is called
- **THEN** the instance is created without exceptions

#### Scenario: SetupManager has no no-arg constructor
- **WHEN** checking `SetupManager.class.getDeclaredConstructors()`
- **THEN** there is exactly one constructor, requiring all three port parameters

### Requirement: SetupManager.setup() returns initialized GameState
The `SetupManager` SHALL provide a method `GameState setup(UUID matchId, UUID playerOneId, UUID playerTwoId, UUID deckOneId, UUID deckTwoId)` that performs the complete setup flow and returns a fully initialized `GameState`.

#### Scenario: setup() returns valid GameState
- **WHEN** `setup(matchId, p1Id, p2Id, deck1Id, deck2Id)` is called with valid decks having Basic Pokémon
- **THEN** the returned `GameState` has `status=ACTIVE`, `phase=DRAW`, `turnNumber=1`, non-null `currentPlayerId` and `firstPlayerId`, both `PlayerState` entries populated

### Requirement: Deck loading via DeckLoadPort
The setup SHALL load both player decks via `DeckLoadPort.loadDeck(deckId)` before any shuffling or dealing.

#### Scenario: Both decks loaded successfully
- **WHEN** setup begins
- **THEN** `DeckLoadPort.loadDeck(deckOneId)` and `DeckLoadPort.loadDeck(deckTwoId)` are called exactly once each

### Requirement: DeckCard expansion to CardInstance list
Each `DeckCard` in `Deck.getCards()` SHALL be expanded into `CardInstance` objects: one `CardInstance` per unit of `DeckCard.quantity`, each with `instanceId=UUID.randomUUID()` and `cardDefinitionId=DeckCard.getCardId()`.

#### Scenario: Deck with multiple copies produces correct CardInstance list
- **WHEN** a deck has a DeckCard with `cardId="xy1-1"` and `quantity=3`
- **THEN** the resulting list contains exactly 3 `CardInstance` entries, each with `cardDefinitionId="xy1-1"` and unique `instanceId` values

### Requirement: Shuffle via RandomizerPort
Deck shuffling SHALL use `RandomizerPort` exclusively. Direct use of `java.util.Random`, `ThreadLocalRandom`, or `Collections.shuffle` is forbidden.

#### Scenario: Deck is shuffled before dealing
- **WHEN** the expanded deck list is prepared
- **THEN** `RandomizerPort.shuffleAndPick(deckList, deckList.size())` is called, and the shuffled result replaces the original deck list

### Requirement: Initial hand of 7 cards
Each player SHALL receive exactly 7 cards from the top of their shuffled deck as their initial hand.

#### Scenario: Both players have 7-card hands
- **WHEN** setup completes successfully
- **THEN** `playerState.getHand().size() == 7` for both players and those cards are removed from `playerState.getDeck()`

### Requirement: Mulligan loop
If a player's initial hand contains no Basic Pokémon (detected via `CardLookupPort.getCardById()` returning a `PokemonCardDefinition` with `stage=="BASIC"`), mulligan SHALL trigger: return 7 cards to deck, reshuffle, redeal 7 cards, increment `mulliganCount`. Loop until hand contains at least one Basic Pokémon. The opponent's `mulliganCount` SHALL determine how many extra cards the opponent draws after all mulligans resolve.

Mulligan logic SHALL be implemented as a private method within `SetupManager` (no separate class per contract 07).

#### Scenario: Single mulligan
- **WHEN** player one's initial hand has no Basic Pokémon, and their second hand has at least one Basic
- **THEN** after setup: player one has `mulliganCount=1`, player two has 8 cards in hand (7 initial + 1 extra)

#### Scenario: Both players need mulligan
- **WHEN** both players' initial hands lack Basic Pokémon, and both second hands have Basic
- **THEN** both have `mulliganCount=1`, and both draw 1 extra card (total 8 each)

### Requirement: Active Pokémon auto-selection
The first `CardInstance` in the player's hand whose card definition is a `PokemonCardDefinition` with `stage=="BASIC"` SHALL be moved from the hand to `activePokemon` as a new `PokemonInPlay` instance. The `PokemonInPlay.instanceId` SHALL match the `CardInstance.instanceId`, `cardDefinitionId` SHALL match, and `ownerPlayerId` SHALL be set to the player's ID.

#### Scenario: Active selected from hand
- **WHEN** both players have at least one Basic Pokémon in hand
- **THEN** `playerState.getActivePokemon()` is non-null for both, `activePokemon.getOwnerPlayerId()` matches the player ID, and the corresponding `CardInstance` is removed from `playerState.getHand()`

### Requirement: Prize assignment
After Active selection, the top 6 cards from each player's remaining deck SHALL be assigned to `playerState.getPrizes()`. Both players SHALL have exactly 6 prizes.

#### Scenario: Both players have 6 prizes
- **WHEN** setup completes successfully
- **THEN** `playerState.getPrizes().size() == 6` for both, and those 6 cards are removed from `playerState.getDeck()`

### Requirement: First player coin flip
The first player SHALL be determined by `RandomizerPort.nextInt(2)`: 0 → `playerOneId` goes first, 1 → `playerTwoId` goes first. Both `currentPlayerId` and `firstPlayerId` in `GameState` SHALL be set to the winner's ID.

#### Scenario: First player determined
- **WHEN** setup completes
- **THEN** `gameState.getCurrentPlayerId()` equals `gameState.getFirstPlayerId()`, and equals either `playerOneId` or `playerTwoId` (not null)

### Requirement: No Spring/JPA dependencies
`SetupManager` SHALL NOT import or use any Spring annotation (`@Component`, `@Service`, `@Autowired`, etc.) or JPA class. It SHALL be pure Java 21.

#### Scenario: No Spring imports
- **WHEN** grepping for `org.springframework` or `jakarta.persistence` in `engine/setup/SetupManager.java`
- **THEN** no matches are found

### Requirement: MulliganService stub removal
The empty `MulliganService.java` class at `engine/setup/MulliganService.java` SHALL be removed, as contract 07 specifies mulligan is a private method within `SetupManager`.

#### Scenario: MulliganService deleted
- **WHEN** checking for `engine/setup/MulliganService.java`
- **THEN** the file does not exist

### Requirement: Bench starts empty
The `bench` field of both `PlayerState` entries SHALL start as an empty list. No bench auto-fill occurs during setup.

#### Scenario: Empty bench after setup
- **WHEN** setup completes
- **THEN** `playerState.getBench()` is an empty list (not null) for both players

### Requirement: TurnFlags initialized false
The `GameState.turnFlags` SHALL be a new `TurnFlags` instance with all boolean fields set to `false`.

#### Scenario: TurnFlags all false
- **WHEN** checking `gameState.getTurnFlags()`
- **THEN** all flags (`hasDrawnForTurn`, `hasAttachedEnergy`, `hasRetreated`, `hasPlayedSupporter`, `hasPlayedStadium`, `hasAttacked`) are `false`
