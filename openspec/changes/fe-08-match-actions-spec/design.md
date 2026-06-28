## Context

FE-07 implemented the match board structure with components for PokemonSlot, BenchZone, MatchHeader, GameLog, PrizeZone, PlayerArea, and OpponentArea. The MatchPage composes these components and handles initialization (WebSocket connection, state polling, card preloading).

FE-05 provides the state management foundation:
- `MatchStateService` — signal-based store with `publicState()`, `privateState()`, `isMyTurn()`, `currentPhase()`, `myActivePokemon()`, etc.
- `MatchInteractionService` — manages visual selection state (`selection()`, `isSelecting()`, `actionInProgress()`, `enterSelectBenchSlot()`, `enterSelectTargetPokemon()`, `cancelSelection()`)
- `GameActionDispatcherService` — sends actions via WebSocket with startAction/completeAction lifecycle

The existing `MatchPage` has an `onPokemonClicked` stub that currently does nothing. The board layout already reserves space for `HandZone` and `ActionPanel` (noted as FE-08 in the FE-07 spec layout).

This change adds the three remaining interaction components and wires the full action flow.

## Goals / Non-Goals

**Goals:**
- Implement `HandZoneComponent` — renders private hand cards with selection visual feedback
- Implement `ActionPanelComponent` — renders context-sensitive action buttons (MAIN/ATTACK/waiting states)
- Implement `VictoryOverlayComponent` — fullscreen overlay on match finish
- Wire `MatchPage` — integrate components, handle `onPokemonClicked`, `onHandCardClicked`, `onActionSelected` events
- Support all MVP action flows: PUT_BASIC_ON_BENCH, ATTACH_ENERGY, EVOLVE_POKEMON, PLAY_TRAINER, RETREAT_ACTIVE, DECLARE_ATTACK, END_TURN

**Non-Goals:**
- No backend changes (all action handlers already exist server-side)
- No changes to existing FE-05 services signatures (except `putBasicOnBench` adding `benchIndex` parameter)
- No changes to FE-07 board components (PokemonSlot, BenchZone, etc.)
- No animations or visual polish beyond MVP requirements
- No target selection for attacks (MVP targets opponent's active Pokemon directly)

## Decisions

| Decision | Rationale |
|---|---|
| `HandZoneComponent` receives data via `input()` signals rather than injecting services | Keeps component reusable and testable. The hand data, selection mode, and valid targets come from MatchPage which reads from MatchStateService and MatchInteractionService. |
| `ActionPanelComponent` injects `MatchStateService` and `MatchInteractionService` directly via DI | ActionPanel needs real-time access to `isMyTurn()`, `currentPhase()`, `actionInProgress()`, etc. Passing all as inputs would create too many input bindings. Consistent with existing codebase pattern. |
| `CardRepositoryService.getFromCache()` is called by MatchPage, not by HandZone | HandZone only shows `name` + `supertype` from `PrivateHandCardModel`. MatchPage needs cardDef to determine `stage`, `subtypes`, `evolvesFrom` for routing hand card clicks. Keeps responsibilities clean. |
| `VictoryOverlayComponent` receives `winnerPlayerId` and `myPlayerId` as inputs | Pure display component — no service dependencies needed. MatchPage controls visibility based on `publicState.status`. |
| `onPokemonClicked` stub is replaced with full selection routing logic | Follows the existing event wiring pattern. The handler checks current `selectionMode` and routes to the correct dispatcher method. |
| `putBasicOnBench` gains a 4th `benchIndex` parameter | The current signature only accepts `handIndex` but the action contract requires both `handIndex` and `benchIndex` in the payload. This is a targeted signature expansion, not a behavioral change. |
| Escape key (`document:keydown.escape`) triggers `cancelSelection()` | Standard UX pattern. Implemented via `@HostListener` in MatchPage. |

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| `CardRepositoryService.getFromCache()` may return `null` if card data not yet loaded | MatchPage already calls `CardRepositoryService.preload()` on init. Fallback: if `getFromCache` returns null, skip the click (no action). |
| User can click multiple cards rapidly | `actionInProgress()` flag in MatchInteractionService blocks concurrent dispatches. HandZone emits are ignored during active selection. |
| WebSocket disconnect during action | `GameActionDispatcherService` already handles this via the `MatchSocketService` connection state. Failed actions trigger `completeAction()` + `setError()`. |
| Bench slot may be taken between click and dispatch | MatchPage verifies slot emptiness (`myPlayerState()!.bench[benchIndex] === null`) before dispatching. Race conditions are handled server-side by the Game Engine. |
