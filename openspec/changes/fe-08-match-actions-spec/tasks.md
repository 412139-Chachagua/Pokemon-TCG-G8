## 1. GameActionDispatcherService (modify)

- [x] 1.1 Add `benchIndex: number` as 4th parameter to `putBasicOnBench()` signature
- [x] 1.2 Update `putBasicOnBench()` to include `benchIndex` in the dispatch payload as `{ handIndex, benchIndex }`

## 2. HandZoneComponent

- [x] 2.1 Create `features/match/components/hand-zone/hand-zone.component.ts` with `ChangeDetectionStrategy.OnPush`
- [x] 2.2 Add inputs: `hand: PrivateHandCardModel[]`, `selectionMode: SelectionMode`, `validTargets: string[]`, `selectedHandIndex: number | null`
- [x] 2.3 Add output: `cardClicked = output<{ card: PrivateHandCardModel; handIndex: number }>()`
- [x] 2.4 Implement inline template: render hand cards with name + supertype badge in horizontal layout
- [x] 2.5 Implement selection visual feedback: dim invalid targets (opacity 50%, pointer-events none) and golden border on selected card
- [x] 2.6 Implement click handler that emits `cardClicked` with card data and 0-based handIndex
- [x] 2.7 Add responsive styles: horizontal scroll on mobile, wrap on desktop

## 3. ActionPanelComponent

- [x] 3.1 Create `features/match/components/action-panel/action-panel.component.ts` with `ChangeDetectionStrategy.OnPush`
- [x] 3.2 Inject `MatchStateService`, `MatchInteractionService`, `CardRepositoryService` via DI
- [x] 3.3 Add output: `actionSelected = output<{ type: GameActionType; payload?: Record<string, unknown> }>()`
- [x] 3.4 Implement `isMyTurn` view: show waiting message when not player's turn
- [x] 3.5 Implement MAIN phase buttons: [Colocar en banca], [Adjuntar energía], [Evolucionar], [Jugar Entrenador], [Retirar activo], [Fin del turno]
- [x] 3.6 Implement ATTACK phase buttons: read `myActivePokemon()` → resolve `cardDef` → render per-attack buttons with name + damage, plus [Fin del turno sin atacar]
- [x] 3.7 Implement [Cancelar] button visible during `isSelecting()`, calls `cancelSelection()` on click
- [x] 3.8 Implement disabled state: disable all buttons when `actionInProgress()` is true, show `LoadingSpinner`
- [x] 3.9 Wire button clicks to emit `actionSelected` with appropriate `GameActionType` and payload

## 4. VictoryOverlayComponent

- [x] 4.1 Create `features/match/components/victory-overlay/victory-overlay.component.ts` with `ChangeDetectionStrategy.OnPush`
- [x] 4.2 Add inputs: `winnerPlayerId: string | null`, `myPlayerId: string | null`
- [x] 4.3 Add output: `returnToLobby = output<void>()`
- [x] 4.4 Implement template: fullscreen fixed overlay with semi-transparent backdrop, winner text ("¡Ganaste!" / "El oponente ganó."), [Volver al lobby] button

## 5. MatchPage — Integration and event wiring

- [x] 5.1 Import and add `HandZoneComponent`, `ActionPanelComponent`, `VictoryOverlayComponent` to MatchPage `imports`
- [x] 5.2 Inject `GameActionDispatcherService` and `CardRepositoryService` into MatchPage
- [x] 5.3 Add `<app-hand-zone>` to template with inputs `[hand]`, `[selectionMode]`, `[validTargets]`, `[selectedHandIndex]` after PlayerArea
- [x] 5.4 Add `<app-action-panel>` to template with `(actionSelected)` event listener at bottom of main content
- [x] 5.5 Add conditional `<app-victory-overlay>` to template when `publicState().status === 'FINISHED'`
- [x] 5.6 Implement `onHandCardClicked(handIndex)` — route by supertype: POKEMON → enterSelectBenchSlot, ENERGY → enterSelectTargetPokemon, TRAINER → check POKEMON_TOOL subtype
- [x] 5.7 Implement `onPokemonClicked` (replace stub) — route by selectionMode: SELECT_BENCH_SLOT → verify empty + dispatch, SELECT_RETREAT_TARGET → verify occupied + dispatch, SELECT_TARGET_POKEMON → resolve instanceId + dispatch
- [x] 5.8 Implement `onActionSelected` handler — dispatch directly for END_TURN/DECLARE_ATTACK, enter selection mode for RETREAT_ACTIVE, ignore card-based actions (start from hand click)
- [x] 5.9 Add `@HostListener('document:keydown.escape')` via `host` to call `interactionService.cancelSelection()`
- [x] 5.10 Wire `returnToLobby` from VictoryOverlay: call `matchState.reset()` + navigate to `/lobby`

## 6. Build verification

- [x] 6.1 Run `ng build` and fix any compilation errors
- [x] 6.2 Run `tsc --noEmit` to verify type correctness
