## ADDED Requirements

### Requirement: MatchPage — Integration of action components

`MatchPage` SHALL compose all action components into the board layout:
- `HandZoneComponent` below `PlayerAreaComponent` (debajo del área de juego)
- `ActionPanelComponent` fixed at bottom
- `VictoryOverlayComponent` as overlay when `publicState.status === 'FINISHED'`

**Nuevos imports en MatchPage:**
```typescript
import { HandZoneComponent } from '../../components/hand-zone/hand-zone.component';
import { ActionPanelComponent } from '../../components/action-panel/action-panel.component';
import { VictoryOverlayComponent } from '../../components/victory-overlay/victory-overlay.component';
import { GameActionDispatcherService } from '../../services/game-action-dispatcher.service';
```

**Nuevas dependencias inyectadas:**
- `GameActionDispatcherService` — para llamar `dispatchAction()`
- `CardRepositoryService` — para resolver cardDefs de hand cards (stage, subtypes)

**Template additions:**
- `<app-hand-zone>` after `</app-player-area>` — with inputs `hand`, `selectionMode`, `validTargets`, `selectedHandIndex`
- `<app-action-panel>` at end of main content — listens to `(actionSelected)` event
- `<app-victory-overlay>` conditional when `publicState().status === 'FINISHED'`

**Event wiring (replacing FE-07 stubs):**

`onPokemonClicked` wiring:
- When event comes from active slot: `event` is `PublicPokemonSlotModel` with `instanceId`
- When event comes from bench (occupied or empty): `event` is `{ benchIndex: number }`
- MatchPage SHALL resolve `instanceId` from player state when the event doesn't carry it

- If `selectionMode === 'SELECT_BENCH_SLOT'`:
  - Read `benchIndex = event.benchIndex`
  - Verify `myPlayerState()!.bench[benchIndex] === null` (empty slot). If occupied, ignore.
  - Dispatch with `handIndex = selectionState.selectedHandIndex`, `benchIndex` in payload
- If `selectionMode === 'SELECT_RETREAT_TARGET'`:
  - Read `benchIndex = event.benchIndex`
  - Verify `myPlayerState()!.bench[benchIndex] !== null` (occupied slot)
  - Dispatch with `benchIndex` in payload
- If `selectionMode === 'SELECT_TARGET_POKEMON'`:
  - Resolve `targetPokemonInstanceId`:
    - If `'instanceId' in event`: use `event.instanceId` (active slot)
    - If not: resolve from `myPlayerState()!.bench[event.benchIndex]!.instanceId` (bench)
  - Dispatch according to selected hand card supertype (ATTACH_ENERGY, EVOLVE_POKEMON, or PLAY_TRAINER)

`onHandCardClicked` wiring:
- If `selectionMode !== 'NONE'`: ignore (selection already active)
- If `supertype === 'POKEMON'`: call `interactionService.enterSelectBenchSlot(handIndex, [])` (validTargets empty — BenchZone highlights empty slots by mode)
- If `supertype === 'ENERGY'`: call `interactionService.enterSelectTargetPokemon(handIndex, allPokemonInstanceIds)`
- If `supertype === 'TRAINER'`:
  - Resolve cardDef via CardRepositoryService
  - If `subtypes` includes `POKEMON_TOOL`: call `enterSelectTargetPokemon`
  - If not: dispatch `PLAY_TRAINER` directly

`onActionSelected` wiring:
- Receives `{ type: GameActionType; payload?: Record<string, unknown> }`
- Calls `dispatcher.dispatchAction(matchId, playerId, type, payload)`
- `cancelSelection()` is handled internally by `GameActionDispatcherService.dispatchAction()` via `completeAction()`

**ESC key support:**
- `@HostListener('document:keydown.escape')` SHALL call `interactionService.cancelSelection()`

**Contract references:** `06-game-state-contract.md` (MatchStatus, TurnPhase, game state model), `08-game-action-contract.md` (GameActionType, payload formats), `15-frontend-state-contract.md` (frontend state model, SelectionState)

#### Scenario: MatchPage routes Pokemon card click to bench slot selection
- WHEN user clicks a card with `supertype === 'POKEMON'` in HandZone
- THEN `interactionService.enterSelectBenchSlot(handIndex, [])` SHALL be called

#### Scenario: MatchPage routes Energy card click to target selection
- WHEN user clicks a card with `supertype === 'ENERGY'` in HandZone
- THEN `interactionService.enterSelectTargetPokemon(handIndex, allPokemonInstanceIds)` SHALL be called

#### Scenario: MatchPage routes Trainer card click to direct dispatch
- WHEN user clicks a card with `supertype === 'TRAINER'` and no `POKEMON_TOOL` subtype
- THEN `dispatcher.playTrainer(matchId, playerId, handIndex)` SHALL be called

#### Scenario: MatchPage routes Trainer with POKEMON_TOOL to target selection
- WHEN user clicks a card with `supertype === 'TRAINER'` and `subtypes` includes `POKEMON_TOOL`
- THEN `interactionService.enterSelectTargetPokemon(handIndex, eligibleInstanceIds)` SHALL be called

#### Scenario: MatchPage ignores hand card click during active selection
- WHEN user clicks a card in HandZone while `selectionMode !== 'NONE'`
- THEN no action SHALL be taken

#### Scenario: MatchPage wires bench click to PUT_BASIC_ON_BENCH
- WHEN user clicks an empty bench slot while `selectionMode === 'SELECT_BENCH_SLOT'`
- THEN `dispatcher.putBasicOnBench(matchId, playerId, selectedHandIndex, benchIndex)` SHALL be called
- AND payload SHALL include both `handIndex` and `benchIndex`

#### Scenario: MatchPage ignores occupied bench slot during bench selection
- WHEN user clicks an occupied bench slot while `selectionMode === 'SELECT_BENCH_SLOT'`
- THEN no action SHALL be dispatched

#### Scenario: MatchPage wires bench click to RETREAT_ACTIVE
- WHEN user clicks an occupied bench slot while `selectionMode === 'SELECT_RETREAT_TARGET'`
- THEN `dispatcher.retreatActive(matchId, playerId, benchIndex)` SHALL be called

#### Scenario: MatchPage wires pokemon click to ATTACH_ENERGY
- WHEN user clicks a Pokemon slot while `selectionMode === 'SELECT_TARGET_POKEMON'` and selected hand card has `supertype === 'ENERGY'`
- THEN `dispatcher.attachEnergy(matchId, playerId, selectedHandIndex, targetInstanceId)` SHALL be called

#### Scenario: MatchPage wires pokemon click to EVOLVE_POKEMON
- WHEN user clicks a Pokemon slot while `selectionMode === 'SELECT_TARGET_POKEMON'` and selected hand card has `stage !== 'BASIC'`
- THEN `dispatcher.evolvePokemon(matchId, playerId, selectedHandIndex, targetInstanceId)` SHALL be called

#### Scenario: MatchPage shows hand zone
- WHEN `matchState.privateState()` has hand cards
- THEN `<app-hand-zone>` SHALL be rendered with those cards

#### Scenario: MatchPage shows victory overlay
- WHEN `matchState.publicState().status === 'FINISHED'`
- THEN `<app-victory-overlay>` SHALL be visible

#### Scenario: MatchPage handles victory return to lobby
- WHEN victory overlay emits `returnToLobby`
- THEN `matchState.reset()` SHALL be called
- THEN router SHALL navigate to `/lobby`

#### Scenario: MatchPage handles ESC key to cancel selection
- WHEN user presses Escape key while `isSelecting()` is `true`
- THEN `interactionService.cancelSelection()` SHALL be called
