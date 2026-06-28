## ADDED Requirements

### Requirement: ActionPanelComponent

`ActionPanelComponent` SHALL render context-sensitive action buttons based on turn, phase, and selection state.

**Injected dependencies (via DI):**
- `MatchStateService` — for `publicState()`, `isMyTurn()`, `currentPhase()`, `myActivePokemon()`
- `MatchInteractionService` — for `selection()`, `actionInProgress()`, `canInteract()`, `isSelecting()`, `cancelSelection()`
- `CardRepositoryService` — for resolving `cardDef` of active Pokemon (to get attacks)

**Inputs:** none (todo via DI, consistente con existing codebase)

**Outputs:**
- `actionSelected: { type: GameActionType; payload?: Record<string, unknown> }`

**Layout variations by state:**

```
─── If !isMyTurn ────────────────────────────────────
  ⏳ Esperando al oponente...

─── If isMyTurn, phase = 'MAIN' ─────────────────────
  [Colocar en banca]  [Adjuntar energía]
  [Evolucionar]       [Jugar Entrenador]
  [Retirar activo]    [Fin del turno]

─── If isMyTurn, phase = 'ATTACK' ───────────────────
  [Frenzy Plant — 150]       ← attack names + damage
  [Tackle — 30]
  [Fin del turno sin atacar]

─── If actionInProgress ─────────────────────────────
  Todos botones deshabilitados + LoadingSpinner
```

**Behavior:**
- Attack buttons SHALL read `myActivePokemon()` → resolve `cardDef` via `CardRepositoryService.getFromCache()` → `cardDef.attacks` for name and damage display
- `[Cancelar]` SHALL be visible when `interactionService.isSelecting()` is `true`, and SHALL call `interactionService.cancelSelection()` + emit null
- MAIN buttons SHALL be disabled during active selection (user must cancel first)
- `[Fin del turno sin atacar]` SHALL only be visible during ATTACK phase
- SHALL use `standalone: true`, `ChangeDetectionStrategy.OnPush`, inline template, DI instead of inputs for services
- `actionSelected` emit SHALL allow MatchPage to decide which dispatcher to call

**Contract references:** `06-game-state-contract.md` (TurnPhase enum), `08-game-action-contract.md` (GameActionType), `15-frontend-state-contract.md` (UI state model for SelectionMode)

#### Scenario: ActionPanel shows MAIN buttons during my turn
- WHEN `isMyTurn()` is `true` and `currentPhase()` is `'MAIN'`
- THEN SHALL render buttons: [Colocar en banca], [Adjuntar energía], [Evolucionar], [Jugar Entrenador], [Retirar activo], [Fin del turno]
- AND none SHALL render attack buttons

#### Scenario: ActionPanel shows ATTACK buttons during attack phase
- WHEN `isMyTurn()` is `true` and `currentPhase()` is `'ATTACK'`
- THEN SHALL render one button per attack from `myActivePokemon()` → cardDef
- AND each attack button SHALL display attack name and damage
- AND SHALL render [Fin del turno sin atacar]

#### Scenario: ActionPanel shows waiting message when not my turn
- WHEN `isMyTurn()` is `false`
- THEN SHALL render waiting message
- AND no action buttons SHALL be rendered

#### Scenario: ActionPanel shows Cancelar button during selection
- WHEN `interactionService.isSelecting()` is `true`
- THEN a [Cancelar] button SHALL be rendered
- WHEN clicked SHALL call `interactionService.cancelSelection()`

#### Scenario: ActionPanel disables all buttons during action in progress
- WHEN `interactionService.actionInProgress()` is `true`
- THEN all buttons SHALL be disabled
- AND a `LoadingSpinner` SHALL be shown
