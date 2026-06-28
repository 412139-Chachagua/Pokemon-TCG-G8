design.md:
## Context

`TurnManager` is an empty stub at `engine/turn/TurnManager.java`. The engine already provides:

- `GameState` with `phase` (`TurnPhase`), `turnNumber`, `currentPlayerId`, `firstPlayerId`, `players[]`, `turnFlags`
- `TurnFlags` with 6 boolean fields and individual setters
- `EngineContext` wrapping `GameState` plus ports, with `addEvent(String)`
- `TurnPhase` enum: `DRAW`, `MAIN`, `ATTACK`, `BETWEEN_TURNS`
- `PlayerState` with `deck` (`List<CardInstance>`), `hand`, `playerId`
- `RandomizerPort` (to inject per spec, unused in V1)

`SetupManager` initialises `GameState` with `phase=DRAW`, `turnNumber=1`, `currentPlayerId=firstPlayerId`, and empty `TurnFlags`. `TurnManager` assumes these invariants.

## Goals / Non-Goals

**Goals:**
- Implement `startTurn`: reset flags, draw (skip for first player on turn 1), phase `DRAW → MAIN`
- Implement `endTurn`: phase `BETWEEN_TURNS`, switch current player, increment turn, chain `startTurn`
- Keep `TurnManager` purely in `engine/turn/`, injectable via constructor, framework-agnostic

**Non-Goals:**
- Combat, damage, attack effects, KO resolution, victory checking, status conditions, coin flips, handlers, persistence — all explicitly excluded per spec
- No changes to `TurnPhase`, `TurnFlags`, `GameState`, `PlayerState`, `EngineContext`, or any class outside `engine/turn/`
- No new test files or unit tests created

## Decisions

### Why inject `RandomizerPort` if unused in V1?
Per spec, the constructor must receive `RandomizerPort`. This keeps the constructor signature consistent with `SetupManager`'s pattern and allows future turn phases (e.g., coin flips for status conditions between turns) without breaking the API.

### Why does `endTurn` call `startTurn` internally?
Per spec: `endTurn` switches player and increments turn, then calls `startTurn(ctx)` for the new player. This ensures the new turn begins atomically within the same action. `GameEngine.applyAction` runs victory checks after the handler returns, so deck-out detection fires after the draw attempt.

### How to find the other player?
Search `state.getPlayers()` for the `PlayerState` whose `playerId` differs from `currentPlayerId`. Do not assume fixed array indices.

### How to reset `TurnFlags`?
Since `TurnFlags` is in `engine/model/` (non-owned), call each setter individually. No `reset()` method is added.

### First-turn draw skip?
If `state.getCurrentPlayerId().equals(state.getFirstPlayerId()) && state.getTurnNumber() == 1`, skip the draw. This matches the TCG rule: the first player does not draw on their first turn (`07-setup-flow-contract.md`).

### Empty deck — why not set `winnerPlayerId`?
Per spec: "NO modificar `winnerPlayerId` directamente." Victory checking belongs to Persona 3. `GameEngine.applyAction` calls `VictoryConditionChecker.check(ctx)` after handler dispatch, which detects the deck-out condition. `TurnManager` only logs the event.

## Risks / Trade-offs

- **Integration gap:** `GameEngineConfig` has no `TurnManager` bean and no `END_TURN` handler yet. `TurnManager` cannot be exercised end-to-end until the handler and wiring are created (Persona 1/3 concern). Mitigation: the class is a plain POJO with no Spring dependencies, trivially instantiable.
- **Plain string events:** `ctx.addEvent(String)` uses plain strings. Future typed event systems may require migration, but V1 event strings are compatible with `GameEngine`'s existing event publishing.
- **Atomicity of `endTurn → startTurn`:** If `startTurn` throws, the turn is partially advanced. Mitigation: all operations are in-memory mutations on `GameState`; no I/O or validation happens in these methods.
