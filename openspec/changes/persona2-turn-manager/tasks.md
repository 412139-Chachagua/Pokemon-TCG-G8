tasks.md:

## 1. Implement TurnManager

- [x] 1.1 Add constructor receiving `RandomizerPort` (store as field, unused in V1)
- [x] 1.2 Implement `startTurn(EngineContext ctx)`:
    - [x] 1.2.1 Obtain `GameState` from `ctx.getState()` and `PlayerState` for `currentPlayerId`
    - [x] 1.2.2 Reset all 6 `TurnFlags` to `false` via individual setters
    - [x] 1.2.3 If deck is empty: emit `GameEvent` with `GameEventType.STATE_UPDATED` — do NOT modify `winnerPlayerId`
    - [x] 1.2.4 Auto-draw removed (handler-driven model): draw is handled by `DrawCardHandler` on `DRAW_CARD` action
    - [x] 1.2.5 Set `state.setPhase(TurnPhase.MAIN)`
- [x] 1.3 Implement `endTurn(EngineContext ctx)`:
    - [x] 1.3.1 Set `state.setPhase(TurnPhase.BETWEEN_TURNS)`
    - [x] 1.3.2 Emit `GameEvent(PHASE_CHANGED)` for end-of-turn
    - [x] 1.3.3 Find the other player by iterating `state.getPlayers()` matching `playerId != currentPlayerId`
    - [x] 1.3.4 Set `state.setCurrentPlayerId(otherPlayerId)`
    - [x] 1.3.5 Increment `state.setTurnNumber(state.getTurnNumber() + 1)`
    - [x] 1.3.6 Set phase to `DRAW` (instead of calling `startTurn`, to let client send `DRAW_CARD`)

## 2. Integrate TurnManager with engine

- [x] 2.1 Add `TurnManager` bean in `GameEngineConfig`
- [x] 2.2 Inject `TurnManager` into `EndTurnHandler` via constructor
- [x] 2.3 `EndTurnHandler.handle()` delegates entirely to `turnManager.endTurn(ctx)`
- [x] 2.4 Pass `TurnManager` from `GameEngineConfig` → `GameEngine` → `EndTurnHandler`

## 3. Fix DrawCardHandler

- [x] 3.1 Draw from index 0 (top of deck) instead of `size()-1`
- [x] 3.2 Add first-turn draw skip: if `currentPlayerId == firstPlayerId && turnNumber == 1`, emit `STATE_UPDATED` event and set phase to `MAIN` without drawing
- [x] 3.3 Set `state.setPhase(TurnPhase.MAIN)` after successful draw

## 4. Verify

- [ ] 4.1 Run `mvn compile` — compilation passes
- [ ] 4.2 Run `mvn test` — all existing tests pass (including `ApplicationTests.contextLoads`)
- [ ] 4.3 Confirm no classes outside `engine/turn/` were modified (handlers wired via constructor, no Spring annotations on engine classes)
- [ ] 4.4 Confirm no Spring/JPA annotations or imports introduced into engine logic
