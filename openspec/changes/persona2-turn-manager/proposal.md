proposal.md:
## Why

`TurnManager` at `engine/turn/TurnManager.java` is an empty stub. The turn flow — start-of-turn draw, phase transitions, turn flag resets, end-turn player switching, and turn counting — must be implemented before `GameEngine` can process `END_TURN` actions and before the match loop can function. This change implements the real `TurnManager` under Persona 2 ownership, strictly following `turn-manager-spec.md`, `06-game-state-contract.md`, and `07-setup-flow-contract.md`.

## What Changes

- Replace empty `TurnManager` stub with a fully implemented class
- Add `RandomizerPort` as constructor dependency (injected but unused in V1 deterministic turn logic)
- Implement `startTurn(EngineContext ctx)`: reset `TurnFlags`, execute draw (skip for first player on turn 1 per `07-setup-flow-contract.md`), transition phase `DRAW → MAIN`
- Implement `endTurn(EngineContext ctx)`: transition phase to `BETWEEN_TURNS`, switch `currentPlayerId`, increment `turnNumber`, chain into `startTurn` for the new player
- No new test files, no modifications outside `engine/turn/`

## Capabilities

### New Capabilities
- `turn-manager`: Turn flow implementation — start-of-turn draw with first-turn skip rule, `TurnFlags` reset, phase transitions (`DRAW → MAIN`, `MAIN → BETWEEN_TURNS`), end-turn player switching, and turn counting. Spec already exists at `openspec/specs/engine-p2/specs-engine-persona2/turn-manager-spec.md`.

### Modified Capabilities
- (none)

## Impact

- `engine/turn/TurnManager.java` — fully implemented (currently empty stub)
- No new files of any kind
- No Spring/JPA dependencies introduced
- No model classes, enums, or any class outside `engine/turn/` modified
- No changes to `GameEngine`, `GameState`, `PlayerState`, `TurnFlags`, `EngineContext`, or any other existing class
- No test files created — acceptance criteria are verified by implementation review and existing project tests via `mvn test`
