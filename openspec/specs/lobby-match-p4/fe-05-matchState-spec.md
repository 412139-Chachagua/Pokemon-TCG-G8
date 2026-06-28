# Spec: FE-05 — MatchState, MatchInteraction y GameActionDispatcher

## Dependencias
- `game-state.models.ts`, `game-action.models.ts`, `ui-state.models.ts` (ya existen)
- `match-api.service.ts`, `match-socket.service.ts` (ya existen)
- `auth.service.ts` (ya existe, para obtener `playerId`)

## Archivos a crear
- `features/match/services/match-state.service.ts`
- `features/match/services/match-interaction.service.ts`

## Archivos a modificar
- `features/match/services/game-action-dispatcher.service.ts` (agregar WebSocket + validar auth)

---

### Requirement: MatchStateService

`MatchStateService` SHALL be an `@Injectable({ providedIn: 'root' })` service with signal-based state management for match game state.

**State signals (readonly public):**
- `matchId: Signal<string | null>`
- `publicState: Signal<PublicGameStateModel | null>`
- `privateState: Signal<PrivatePlayerStateModel | null>`
- `events: Signal<GameEventDto[]>`
- `lastError: Signal<GameErrorModel | null>`
- `connectionStatus: Signal<'DISCONNECTED' | 'CONNECTED' | 'RECONNECTING'>`

**Computed signals:**
- `isMyTurn: Signal<boolean>` — true cuando `publicState()?.currentPlayerId === playerId()`
- `currentPhase: Signal<TurnPhase | null>` — alias de `publicState()?.phase`
- `myPlayerId: Signal<string | null>` — del `AuthService.playerId`
- `myActivePokemon: Signal<PublicPokemonSlotModel | null>` — el active del jugador local
- `opponentActivePokemon: Signal<PublicPokemonSlotModel | null>` — el active del oponente

**Methods:**
- `initialize(matchId: string)`: conecta WebSocket + hace GET state inicial
- `updatePublicState(state: PublicGameStateModel)`: actualiza `_publicState`
- `updatePrivateState(state: PrivatePlayerStateModel)`: actualiza `_privateState`
- `addEvent(event: GameEventDto)`: agrega a `_events`
- `setError(error: GameErrorModel | null)`: actualiza `_lastError`
- `reset()`: limpia todos los signals

#### Scenario: initialize connects WebSocket and fetches state
- WHEN calling `initialize(matchId)` with a valid matchId and authenticated player
- THEN `MatchSocketService.connect()` SHALL be called
- THEN `MatchApiService.getMatchState(matchId, playerId)` SHALL be called
- THEN `publicState` SHALL be set from response
- THEN `privateState` SHALL be set from response
- THEN WebSocket subscriptions SHALL be established

#### Scenario: WebSocket public event updates publicState
- WHEN a `STATE_UPDATED` event is received via WebSocket
- THEN `updatePublicState` SHALL be called with the new state
- THEN the event SHALL be added to `events`

#### Scenario: WebSocket private event updates privateState
- WHEN a private state message is received via WebSocket `/queue/matches/{id}/{playerId}`
- THEN `updatePrivateState` SHALL be called

#### Scenario: isMyTurn computed correctly
- WHEN `publicState.currentPlayerId` equals `myPlayerId`
- THEN `isMyTurn()` SHALL return `true`
- WHEN they differ
- THEN `isMyTurn()` SHALL return `false`

#### Scenario: reset clears all state
- WHEN `reset()` is called
- THEN all signals SHALL return to initial values
- THEN `MatchSocketService.disconnect()` SHALL be called

---

### Requirement: MatchInteractionService

`MatchInteractionService` SHALL be an `@Injectable({ providedIn: 'root' })` service managing visual interaction state.

**State signals (private):**
- `_selection: Signal<SelectionState>` — mode, selectedHandIndex, validTargets
- `_hoveredCardInstanceId: Signal<string | null>`
- `_actionInProgress: Signal<boolean>`
- `_modalSignal: Signal<{ open: boolean; content: unknown }>`

**Computed signals (public readonly):**
- `selection: Signal<SelectionState>`
- `isSelecting: Signal<boolean>` — `mode !== 'NONE'`
- `canInteract: Signal<boolean>` — `!actionInProgress() && !modalOpen`
- `actionInProgress: Signal<boolean>`
- `hoveredCardInstanceId: Signal<string | null>`
- `modal: Signal<{ open: boolean; content: unknown }>`

**Methods:**
- `enterSelectBenchSlot(handIndex: number, validTargets: string[])`: setea `mode='SELECT_BENCH_SLOT'`
- `enterSelectTargetPokemon(handIndex: number, targets: string[])`: setea `mode='SELECT_TARGET_POKEMON'`
- `enterSelectRetreatTarget(validTargets: string[])`: setea `mode='SELECT_RETREAT_TARGET'`
- `cancelSelection()`: resetea `_selection` a `{ mode: 'NONE', ... }`
- `startAction(requestId: string)`: setea `_actionInProgress = true`
- `completeAction()`: setea `_actionInProgress = false` + `cancelSelection()`
- `setHoveredCard(instanceId: string | null)`: actualiza `_hoveredCardInstanceId`
- `openModal(content: unknown)`: abre modal
- `closeModal()`: cierra modal

#### Scenario: enterSelectBenchSlot sets correct mode
- WHEN calling `enterSelectBenchSlot(2, ['slot-5'])` with handIndex=2
- THEN `selection()` SHALL return `{ mode: 'SELECT_BENCH_SLOT', selectedHandIndex: 2, selectedInstanceId: null, validTargets: ['slot-5'] }`
- THEN `isSelecting()` SHALL return `true`

#### Scenario: cancelSelection resets to NONE
- WHEN calling `cancelSelection()` after entering a selection mode
- THEN `selection()` SHALL return `{ mode: 'NONE', selectedHandIndex: null, selectedInstanceId: null, validTargets: [] }`

#### Scenario: canInteract blocks during action
- WHEN `actionInProgress()` is `false` and `modal()` is `{ open: false }`
- THEN `canInteract()` SHALL return `true`
- WHEN `actionInProgress()` is `true`
- THEN `canInteract()` SHALL return `false`
- WHEN `modal()` has `open: true`
- THEN `canInteract()` SHALL return `false`

---

### Requirement: GameActionDispatcherService (modified)

`GameActionDispatcherService` SHALL use WebSocket (`MatchSocketService.sendAction`) as primary transport, falling back to HTTP POST.

**Modified behavior:**
- `dispatchAction()` SHALL check `auth.service.playerId()` for the playerId if not provided
- `dispatchAction()` SHALL first verify `interactionService.actionInProgress()` is `false`; if true, log warning and return
- `dispatchAction()` SHALL call `interactionService.startAction()` before sending
- On success response: call `interactionService.completeAction()` + update `matchState` with public/private state
- On error response: call `interactionService.completeAction()` + call `matchState.setError(error)`
- `dispatchAction()` SHALL primarily use `MatchSocketService.sendAction()` (STOMP), with HTTP POST as fallback

#### Scenario: dispatchAction uses WebSocket
- WHEN calling `dispatchAction(matchId, playerId, actionType, payload)`
- THEN `interactionService.startAction()` SHALL be called
- THEN `MatchSocketService.sendAction()` SHALL be called with the `GameActionRequest`
- THEN `interactionService.completeAction()` SHALL be called after response

#### Scenario: dispatchAction blocks when actionInProgress
- WHEN `interactionService.actionInProgress()` is `true`
- THEN calling any dispatcher method SHALL log a warning and return without sending

#### Scenario: dispatchAction obtains playerId from AuthService
- WHEN calling `endTurn(matchId)` without explicit playerId
- THEN the dispatcher SHALL use `auth.playerId()` as the playerId parameter
