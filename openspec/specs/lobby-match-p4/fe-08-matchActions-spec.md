# Spec: FE-08 — Match Actions & Interactions

## Dependencias
- FE-07 board components (PokemonSlot, BenchZone, MatchHeader, GameLog, PrizeZone, PlayerArea, OpponentArea)
- FE-05 stores (MatchStateService, MatchInteractionService, GameActionDispatcherService)
- AuthService, CardRepositoryService

## Archivos a crear
- `features/match/components/hand-zone/hand-zone.component.ts`
- `features/match/components/action-panel/action-panel.component.ts`
- `features/match/components/victory-overlay/victory-overlay.component.ts`

## Archivos a modificar
- `features/match/pages/match-page/match-page.ts` (agregar hand-zone, action-panel, victory-overlay en template + event wiring)

---

### Requirement: HandZoneComponent

**Inputs:**
- `hand: PrivateHandCardModel[]`
- `selectionMode: SelectionMode`
- `validTargets: string[]` (hand indices como strings: "0", "1", "2"...)
- `selectedHandIndex: number | null`

**Outputs:** `cardClicked: { card: PrivateHandCardModel; handIndex: number }`

**Display:**
```
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Charmander│ │  Fire E  │ │  Potion  │ │  ...     │
│  POKEMON  │ │  ENERGY  │ │  TRAINER │ │          │
└──────────┘ └──────────┘ └──────────┘ └──────────┘
```

**Behavior:**
- Each card shows `name` + `supertype` badge
- Cards whose index NOT in `validTargets` during selection: opacity 50%, pointer-events none
- Card whose index matches `selectedHandIndex`: golden border
- Click emits `cardClicked` with `handIndex`
- Responsive: horizontal scroll en mobile, wrap en desktop

**Pattern notes:**
- Usa `standalone: true`, `ChangeDetectionStrategy.OnPush`, `input()/output()`, inline template, Tailwind-compatible styles
- `validTargets` son string[] porque así lo define `SelectionState`

#### Scenario: HandZone shows cards
- WHEN receiving 4 `PrivateHandCardModel` items
- THEN 4 card items SHALL be rendered with name and supertype badge

#### Scenario: HandZone dims invalid targets
- WHEN `selectionMode` is not `'NONE'` and a card's handIndex is NOT in `validTargets`
- THEN that card SHALL have reduced opacity and `pointer-events: none`

#### Scenario: HandZone highlights selected card
- WHEN `selectedHandIndex` equals a card's handIndex
- THEN that card SHALL have a golden border

#### Scenario: HandZone click emits cardClicked
- WHEN user clicks a card in the hand zone
- THEN `cardClicked` SHALL emit with that card's data and its `handIndex`
- AND `handIndex` SHALL be the 0-based position in the hand array

---

### Requirement: ActionPanelComponent

**Inyecta:**
- `MatchStateService` — para `publicState()`, `isMyTurn()`, `currentPhase()`, `myActivePokemon()`
- `MatchInteractionService` — para `selection()`, `actionInProgress()`, `canInteract()`, `isSelecting()`, `cancelSelection()`
- `CardRepositoryService` — para resolver `cardDef` del activo (ataques)

**Inputs:** none (todo vía DI para mantener patrón consistente con existing codebase)

**Outputs:**
- `actionSelected: { type: GameActionType; payload?: Record<string, unknown> }` (para que el padre dispatchee)

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
- Attack buttons leen `matchState.myActivePokemon()` → resolve `cardDef` via `CardRepositoryService.getFromCache()` → `cardDef.attacks`
- `[Cancelar]` visible cuando `interactionService.isSelecting()`, llama a `cancelSelection()` + emite null
- Botones MAIN deshabilitados durante selección activa (cancelar primero)
- `[Fin del turno sin atacar]` solo visible en phase ATTACK

**Pattern notes:**
- Sigue mismo patrón que otros componentes: standalone, OnPush, inline template, DI en lugar de inputs para servicios
- `actionSelected` emit permite al MatchPage decidir qué dispatcher llamar

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

---

### Requirement: Action flows

Nota sobre resolución de cardDef: `PrivateHandCardModel` solo tiene `instanceId`, `cardId`, `name`, `supertype`. Para determinar `stage`, `evolvesFrom`, `subtypes`, el MatchPage DEBE resolver vía `CardRepositoryService.getFromCache(cardId)`. El HandZone NO resuelve cardDefs — solo muestra name + supertype.

Nota sobre instanceIds de Pokémon: los `PublicPokemonSlotModel` activos tienen `instanceId` directo. Los Pokémon en banca se identifican por `benchIndex` (0-4). Para resolver `instanceId` de un Pokémon en banca: `myPlayerState().bench[benchIndex].instanceId`. El MatchPage DEBE computar `allPokemonInstanceIds` como `[myPlayerState().activePokemon.instanceId, ...myPlayerState().bench.filter(Boolean).map(p => p.instanceId)]`, y `eligiblePokemonInstanceIds` filtrando aquellos cuyo `evolvesFrom` coincida con la carta seleccionada.

#### Flow: PUT_BASIC_ON_BENCH
1. User clicks a Pokemon card in hand (`supertype === 'POKEMON'`)
2. HandZone emite `cardClicked` → MatchPage llama `interactionService.enterSelectBenchSlot(handIndex, [])` (validTargets vacío porque BenchZone resalta vacíos por modo, no por targets)
3. Empty bench slots se marcan como clickables (`class.clickable` cuando `selectionMode === 'SELECT_BENCH_SLOT'`)
4. MatchPage DEBE verificar que `myPlayerState().bench[benchIndex] === null` antes de continuar (slots ocupados se ignoran)
5. User clicks empty bench slot → dispatch `PUT_BASIC_ON_BENCH` con payload `{ handIndex, benchIndex }`

#### Flow: ATTACH_ENERGY
1. User clicks energy card in hand (`supertype === 'ENERGY'`)
2. → `interactionService.enterSelectTargetPokemon(handIndex, allPokemonInstanceIds)`
3. Pokemon slots con `instanceId` en `validTargets` se resaltan via `isHighlighted`
4. User clicks target → MatchPage obtiene `targetPokemonInstanceId`: si el evento tiene `instanceId` (active) lo usa directo; si tiene `benchIndex` (banca) lo resuelve desde `myPlayerState().bench[benchIndex].instanceId`
5. Dispatch `ATTACH_ENERGY` con payload `{ handIndex, targetPokemonInstanceId }`

#### Flow: EVOLVE_POKEMON
1. User clicks Pokemon in hand donde `cardDef.stage !== 'BASIC'` (resuelto por MatchPage)
2. → `interactionService.enterSelectTargetPokemon(handIndex, eligiblePokemonInstanceIds)`
3. Solo Pokemon que pueden evolucionar (matching `evolvesFrom`) se resaltan
4. User clicks target → MatchPage resuelve `targetPokemonInstanceId` igual que en ATTACH_ENERGY
5. Dispatch `EVOLVE_POKEMON` con payload `{ handIndex, targetPokemonInstanceId }`

#### Flow: PLAY_TRAINER
1. User clicks Trainer card in hand (`supertype === 'TRAINER'`)
2. If `cardDef.subtypes` incluye `POKEMON_TOOL`: enter `SELECT_TARGET_POKEMON` mode
3. Otherwise: dispatch directo `PLAY_TRAINER` con payload `{ handIndex }`

#### Flow: RETREAT_ACTIVE
1. User clicks `[Retirar activo]` button en ActionPanel
2. → `interactionService.enterSelectRetreatTarget(benchInstanceIds)` — `benchInstanceIds` son los `instanceId` de Pokémon en banca (`myPlayerState().bench.filter(Boolean).map(p => p.instanceId)`)
3. Bench slots con `instanceId` en `validTargets` se resaltan
4. User clicks target bench slot → dispatch `RETREAT_ACTIVE` con payload `{ benchIndex }`

#### Flow: DECLARE_ATTACK
1. User clicks attack button en ActionPanel
2. → emite `{ type: 'DECLARE_ATTACK', payload: { attackIndex, targetPokemonInstanceId: opponentActive.instanceId } }`
3. Target siempre es el activo del oponente (no target selection para MVP)

#### Flow: END_TURN
1. User clicks `[Fin del turno]` button
2. → emite `{ type: 'END_TURN', payload: {} }`

#### Flow: Cancel selection
1. User clicks `[Cancelar]` button (visible when `interactionService.isSelecting()`)
2. → `interactionService.cancelSelection()`
3. All highlights removed, selection mode back to NONE
4. ESC key via `@HostListener('document:keydown.escape')` también llama `cancelSelection()`

---

### Requirement: VictoryOverlayComponent

**Inputs:** `winnerPlayerId: string | null`, `myPlayerId: string | null`

**Visibility:** Mostrado por el MatchPage cuando `matchState.publicState()?.status === 'FINISHED'`

**Display:**
```
┌──────────────────────────────────────┐
│          Fin de partida              │
│   ¡Ganaste! / El oponente ganó.      │
│                                      │
│         [Volver al lobby]            │
└──────────────────────────────────────┘
```

**Behavior:**
- Show "¡Ganaste!" si `winnerPlayerId === myPlayerId`
- Show "El oponente ganó." otherwise
- `[Volver al lobby]` emite `returnToLobby` (el MatchPage llama `matchState.reset()` + navega)

**Outputs:** `returnToLobby: void`

**Pattern notes:**
- No usa emojis en el template (consistente con existing codebase)
- Overlay con position: fixed, backdrop semi-transparente

#### Scenario: VictoryOverlay shows winner
- WHEN `winnerPlayerId` equals `myPlayerId`
- THEN displayed text SHALL be "¡Ganaste!"
- WHEN they differ
- THEN displayed text SHALL be "El oponente ganó."

#### Scenario: VictoryOverlay emits returnToLobby
- WHEN user clicks "[Volver al lobby]"
- THEN `returnToLobby` SHALL emit

---

### Requirement: MatchPage — Integration of action components

`MatchPage` SHALL compose all action components:
- `HandZoneComponent` below `PlayerAreaComponent` (debajo del área de juego)
- `ActionPanelComponent` fixed en parte inferior
- `VictoryOverlayComponent` como overlay cuando `publicState.status === 'FINISHED'`

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
- `<app-hand-zone>` after `</app-player-area>` — con inputs hand (privateState().hand), selectionMode, validTargets, selectedHandIndex
- `<app-action-panel>` al final del main — escucha `(actionSelected)` event
- `<app-victory-overlay>` condicional cuando `publicState().status === 'FINISHED'`

**Event wiring existente (FE-07) que se mantiene:**
- `matchState`, `interactionService`, `myPlayerState`, `opponentPlayerState`, `selectionState` ya existen
- `onPokemonClicked` ya es un stub → se completa con la lógica de selección

**onPokemonClicked wiring (reemplazar stub):**

Nota: el evento recibido es `PublicPokemonSlotModel | { benchIndex: number }`.
- Cuando viene del active slot: `event` es `PublicPokemonSlotModel` con `instanceId`.
- Cuando viene de banca (occupied o empty): `event` es `{ benchIndex: number }`.
MatchPage DEBE resolver `instanceId` desde el player state cuando el evento no lo trae.

- Si `selectionMode === 'SELECT_BENCH_SLOT'`:
  - `benchIndex = event.benchIndex`
  - Verificar que `myPlayerState()!.bench[benchIndex] === null` (slot vacío). Si está ocupado, ignorar.
  - Dispatch con `handIndex = selectionState.selectedHandIndex`, `benchIndex` en payload
- Si `selectionMode === 'SELECT_RETREAT_TARGET'`:
  - `benchIndex = event.benchIndex`
  - Verificar que `myPlayerState()!.bench[benchIndex] !== null` (slot ocupado)
  - Dispatch con `benchIndex` en payload
- Si `selectionMode === 'SELECT_TARGET_POKEMON'`:
  - Resolver `targetPokemonInstanceId`:
    - Si `'instanceId' in event`: usar `event.instanceId` (active slot)
    - Si no: resolver de `myPlayerState()!.bench[event.benchIndex]!.instanceId` (banca)
  - Dispatch según supertype de la carta seleccionada (ATTACH_ENERGY, EVOLVE_POKEMON, o PLAY_TRAINER)

**Nota:** `GameActionDispatcherService.putBasicOnBench()` debe actualizarse para aceptar `benchIndex: number` como 4to parámetro e incluirlo en el payload `{ handIndex, benchIndex }`.

**onHandCardClicked wiring:**
- Si `selectionMode !== 'NONE'`: ignorar (ya hay selección activa)
- Si `supertype === 'POKEMON'`: llamar `interactionService.enterSelectBenchSlot(handIndex, [])` (validTargets vacío, BenchZone usa el modo para resaltar slots vacíos)
- Si `supertype === 'ENERGY'`: llamar `interactionService.enterSelectTargetPokemon(handIndex, allPokemonInstanceIds)`
- Si `supertype === 'TRAINER'`:
  - Resolver cardDef via CardRepositoryService
  - Si `subtypes` incluye `POKEMON_TOOL`: `enterSelectTargetPokemon`
  - Si no: dispatch `PLAY_TRAINER` directamente

**onActionSelected wiring:**
- Recibe `{ type: GameActionType; payload?: Record<string, unknown> }`
- Llama `dispatcher.dispatchAction(matchId, playerId, type, payload)`
- `cancelSelection()` ya lo maneja internamente `GameActionDispatcherService.dispatchAction()` vía `completeAction()`

#### Scenario: MatchPage routes Pokemon card click to bench slot selection
- WHEN user clicks a card with `supertype === 'POKEMON'` in HandZone
- THEN `interactionService.enterSelectBenchSlot(handIndex, [])` SHALL be called

#### Scenario: MatchPage routes Energy card click to target selection
- WHEN user clicks a card with `supertype === 'ENERGY'` in HandZone
- THEN `interactionService.enterSelectTargetPokemon(handIndex, allPokemonInstanceIds)` SHALL be called

#### Scenario: MatchPage routes Trainer card click to direct dispatch
- WHEN user clicks a card with `supertype === 'TRAINER'` and no `POKEMON_TOOL` subtype
- THEN `dispatcher.playTrainer(matchId, playerId, handIndex)` SHALL be called
- AND no selection mode SHALL be entered

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
