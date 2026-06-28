## Why

FE-07 implemented the match board structure (PokemonSlot, BenchZone, MatchHeader, GameLog, PlayerArea, OpponentArea) but the MatchPage still lacks any interaction layer. Players cannot play cards from their hand, execute game actions (attach energy, evolve, retreat, attack), or see a victory screen when the match ends. FE-05 already provides `MatchStateService`, `MatchInteractionService`, and `GameActionDispatcherService` — but no UI components consume them for action flows.

## What Changes

- Create `HandZoneComponent` — renders the player's hand cards with selection support (valid targets, highlighted selection, click emission)
- Create `ActionPanelComponent` — renders context-sensitive action buttons based on turn, phase, and selection state
- Create `VictoryOverlayComponent` — shows win/loss result when match finishes
- Modify `MatchPage` — integrate HandZone, ActionPanel, VictoryOverlay in the board layout; wire action flows (PUT_BASIC_ON_BENCH, ATTACH_ENERGY, EVOLVE_POKEMON, PLAY_TRAINER, RETREAT_ACTIVE, DECLARE_ATTACK, END_TURN)
- Modify `GameActionDispatcherService` — update `putBasicOnBench()` to accept `benchIndex` as 4th parameter

## Capabilities

### New Capabilities

- `hand-zone`: HandZoneComponent — renders private hand cards with `name` + `supertype` badge, dims invalid targets during selection, highlights selected card, emits `cardClicked` with card data and hand index
- `action-panel`: ActionPanelComponent — renders context-sensitive buttons (MAIN phase buttons, ATTACK phase buttons with per-attack name+damage, waiting message when not your turn, Cancelar button during selection, disabled state during action in progress)
- `victory-overlay`: VictoryOverlayComponent — fullscreen overlay showing "¡Ganaste!" or "El oponente ganó." with a "Volver al lobby" button, displayed when `publicState.status === 'FINISHED'`
- `match-actions-interactions`: MatchPage integration — composes all three components in the board layout, wires event handlers for hand card clicks (routing by supertype), Pokemon slot clicks (routing by selection mode), and action panel button clicks (dispatching via GameActionDispatcherService)

### Modified Capabilities

- `fe-05-match-state` (`openspec/specs/lobby-match-p4/fe-05-matchState-spec.md`): `GameActionDispatcherService.putBasicOnBench()` must accept `benchIndex: number` as a 4th parameter and include it in the action payload as `{ handIndex, benchIndex }`

## Impact

- `FE/src/app/features/match/components/hand-zone/hand-zone.component.ts` — new file (standalone, OnPush)
- `FE/src/app/features/match/components/action-panel/action-panel.component.ts` — new file (standalone, OnPush)
- `FE/src/app/features/match/components/victory-overlay/victory-overlay.component.ts` — new file (standalone, OnPush)
- `FE/src/app/features/match/pages/match-page/match-page.ts` — modified (import + compose components, wire event handlers)
- `FE/src/app/features/match/services/game-action-dispatcher.service.ts` — modified (putBasicOnBench signature)
- New dependencies: `CardRepositoryService` in MatchPage (for cardDef resolution), `GameActionDispatcherService` in MatchPage (for dispatching)
