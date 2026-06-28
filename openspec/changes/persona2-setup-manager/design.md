## Context


`SetupManager` at `engine/setup/SetupManager.java` is an empty stub. The master spec (`engine-p2/engine-persona2-master-spec.md`) and subsystem spec (`engine-p2/specs-engine-persona2/setup-manager-spec.md`) define the full behavior, but no implementation exists. Contract 07 (setup-flow-contract) defines the canonical flow. `CardLookupPort` and `RandomizerPort` already exist as interfaces.
`DeckLoadPort` may already exist; if not, Persona 2 may define the minimal interface contract required by `SetupManager`. The engine model classes (`GameState`, `PlayerState`, `CardInstance`, `PokemonInPlay`, `TurnFlags`, `GamePhase`, `PlayerSide`, `MatchStatus`) are all defined and compiling.

## Goals / Non-Goals

**Goals:**
- Implement `SetupManager.setup()` with: deck loading → shuffle → deal 7 cards → mulligan loop → auto-select Active → assign 6 prizes → coin flip → build `GameState`
- Constructor injection of `DeckLoadPort`, `CardLookupPort`, `RandomizerPort`
- Mulligan logic as a private method inside `SetupManager` (no separate class per contract 07)
- Returned `GameState`: `status=ACTIVE`, `phase=DRAW`, `turnNumber=1`, `turnFlags` all false, both players have Active + 6 prizes + valid hands
- No Spring/JPA imports or annotations
- Remain compatible with existing engine model classes

**Non-Goals:**
- No changes to `GameState`, `PlayerState`, `PokemonInPlay`, or any model class
- No changes to `GameEngine`, `EngineContext`, or handlers
- No redesign of existing ports or adapters beyond the minimal `DeckLoadPort` contract if it does not yet exist
- No frontend changes
- No persistence changes
- No bench auto-fill (bench starts empty; only Active is placed during setup per V1 simplification in spec)
- No manual Active selection (V1 auto-selects first Basic in hand per spec)

## Decisions

1. **Mulligan as private method, not separate class** — Contract 07 explicitly states: "No separate `MulliganService` class exists. Mulligan is a private method within `SetupManager`." The existing `MulliganService.java` stub will be removed to align with the contract.

2. **Deck cards expanded to CardInstance list via flatMap** — `Deck.getCards()` returns `List<DeckCard>` where each `DeckCard` has `quantity` and `cardId`. The setup expands this into individual `CardInstance` objects with `UUID.randomUUID()` instanceId and cardDefinitionId from `DeckCard.getCardId()`. This produces the full deck list for shuffling.

3. **Shuffle via existing `RandomizerPort.shuffle()` method** — `SetupManager` uses the already existing `<T> void shuffle(List<T> items)` method from `RandomizerPort`. `RandomizerAdapter.shuffle()` performs Fisher-Yates shuffling: loop from last index down to 1, swapping each element with a randomly chosen element at or before it via `nextInt(i + 1)`. `SetupManager` calls this method exclusively through the port — no direct `Collections.shuffle`, `Random`, or `ThreadLocalRandom`. This keeps randomness encapsulated and fully mockable in tests.

4. **Active auto-selection: first Basic in hand** — Scan hand left-to-right (deck order after shuffle). For each `CardInstance`, call `CardLookupPort.getCardById()` and check `instanceof PokemonCardDefinition && "BASIC".equals(stage)`. First match becomes Active. This is the V1 simplification from contract 07.

5. **Prize assignment: top 6 cards after all draws** — After mulligan resolution and Active selection, the remaining deck cards (in order) have their first 6 assigned to `playerState.setPrizes()`. Invariant: exactly 6 prizes per player.

6. **Opponent extra cards for mulligan** — Each time a player declares a mulligan, the opponent's extra card count increments by 1. After both players' mulligan loops complete, the opponent draws that many cards from the top of their deck into their hand. This matches the published TCG rules.

7. **Coin flip: RandomizerPort.nextInt(2)** — 0 → first player = playerOne (PLAYER_ONE), 1 → first player = playerTwo (PLAYER_TWO). Both `currentPlayerId` and `firstPlayerId` are set to the winner.

## Risks / Trade-offs

- **[Risk] `RandomizerAdapter.shuffle()` implementation correctness — The Fisher-Yates loop in the adapter must swap correctly on every index. Mitigation: SetupManager unit tests use a mock RandomizerPort so they are independent of the adapter. Adapter correctness is verified separately in RandomizerAdapterTest.
- **[Risk] `Deck.getCards()` returns unexpanded DeckCard list** — The `DeckCard` has `quantity`, so a deck with 4 copies of the same card produced 4 `CardInstance` entries. Mitigation: this is correct behavior.
- **[Trade-off] No bench auto-fill during setup** — The V1 spec says bench starts empty. Players place bench Pokémon via `PUT_BASIC_ON_BENCH` actions during their turn. This simplifies setup but means the initial game state has no bench.
