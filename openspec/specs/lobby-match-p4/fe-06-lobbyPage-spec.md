# Spec: FE-06 — Lobby Page

## Dependencias
- FE-05 stores (MatchState, MatchInteraction)
- `deck-api.service.ts`, `match-api.service.ts`
- `auth.service.ts` (para playerId)
- `match-facade.service.ts`
- Shared: `ButtonComponent`, `ModalComponent`, `LoadingSpinnerComponent`, `NotificationService`

## Archivos a crear
- `features/lobby/components/match-create/match-create.component.ts`
- `features/lobby/components/match-create/match-create.component.html`
- `features/lobby/components/match-join/match-join.component.ts`
- `features/lobby/components/match-join/match-join.component.html`
- `features/lobby/components/match-list/match-list.component.ts`
- `features/lobby/components/match-list/match-list.component.html`

## Archivos a modificar
- `features/lobby/pages/lobby-page/lobby-page.ts`
- `features/match/services/match-facade.service.ts` (ya implementado, verificar señales y delegar getMatchState)

---

### Requirement: MatchFacadeService (review — minimal changes)

`MatchFacadeService` already has:
- `playerId` from `AuthService` (injectado internamente)
- `matchId`, `playerId`, `side`, `status` signals persistidos tras create/join
- `createMatch(player1Name, player1DeckId)` → `Observable<MatchResponse>`
- `joinMatch(matchId, playerName, deckId)` → `Observable<MatchResponse>`

Cambio requerido:
- `getMatchState()` SHALL delegate a `MatchStateService.initialize()` en vez de llamar a `MatchApiService` directamente

---

### Requirement: MatchCreateComponent

`MatchCreateComponent` SHALL provide a form to create a match.

**Inputs:** `playerId: string` (from auth)
**Outputs:** `created: EventEmitter<MatchResponse>`

**Layout:**
```
┌─────────────────────────────────────────┐
│  Crear partida                          │
│  Tu nombre: [_________________________] │
│  Mazo:      [▼ Selector de mazos ▼]    │
│                         [Crear partida] │
└─────────────────────────────────────────┘
```

**Behavior:**
- On init: load player's decks via `DeckApiService.listByPlayer(playerId)`
- Select dropdown shows deck names, value is deck id
- `[Crear partida]` disabled if name or deck not selected, or while loading
- On submit: call `MatchFacadeService.createMatch(name, deckId)`
- On success: emit `created` with the response
- On error: show notification with error message
- Local signals: `loading`, `error`, `decks`

#### Scenario: MatchCreateComponent loads decks on init
- WHEN component initializes with a valid `playerId`
- THEN `DeckApiService.listByPlayer(playerId)` SHALL be called
- THEN the select dropdown SHALL be populated with deck names

#### Scenario: MatchCreateComponent creates match
- WHEN user fills name and selects a deck, then clicks "Crear partida"
- THEN `MatchFacadeService.createMatch(name, deckId)` SHALL be called
- THEN on success, `created` SHALL emit

#### Scenario: MatchCreateComponent shows error
- WHEN API call fails during create
- THEN `error` signal SHALL be set
- THEN `NotificationService.show()` SHALL be called with error message

---

### Requirement: MatchJoinComponent

`MatchJoinComponent` SHALL provide a form to join an existing match.

**Inputs:** `playerId: string`
**Outputs:** `joined: EventEmitter<MatchResponse>`

**Layout:**
```
┌─────────────────────────────────────────┐
│  Unirse a partida                       │
│  ID de la partida: [__________________] │
│  Tu nombre:        [__________________] │
│  Mazo:             [▼ Selector de mazos]│
│                          [Unirse]       │
└─────────────────────────────────────────┘
```

**Behavior:**
- `matchId` field can be pre-filled via a setter method (called from MatchListComponent)
- Loads decks the same way as MatchCreateComponent
- On submit: call `MatchFacadeService.joinMatch(matchId, name, deckId)`
- On success: emit `joined`

#### Scenario: MatchJoinComponent pre-fills matchId
- WHEN `setMatchId(id)` is called externally
- THEN the matchId input field SHALL be updated

---

### Requirement: MatchListComponent

`MatchListComponent` SHALL display a list of waiting matches.

**Outputs:** `matchSelected: EventEmitter<string>` (emits matchId)

**Layout:**
```
┌──────────────────────────────────────────────────────┐
│  Partidas disponibles            [↻ Actualizar]      │
├──────────────────────────────────────────────────────┤
│  match-abc123   |  Esperando jugador   [Usar este]   │
│  match-def456   |  Esperando jugador   [Usar este]   │
├──────────────────────────────────────────────────────┤
│  No hay partidas disponibles.                        │
└──────────────────────────────────────────────────────┘
```

**Behavior:**
- `[Usar este]` emits the matchId via `matchSelected`
- `[↻ Actualizar]` re-fetches (stub: no endpoint exists yet, show placeholder)
- Empty state message when no matches

---

### Requirement: LobbyPage

`LobbyPage` SHALL compose the full lobby view.

**Layout:**
```
┌────────────────────────────────────────────────────────┐
│  ┌───────────────────────┐  ┌────────────────────────┐ │
│  │  MatchCreateComponent │  │  MatchListComponent    │ │
│  └───────────────────────┘  └────────────────────────┘ │
│  ┌──────────────────────────────────────────────────┐   │
│  │  MatchJoinComponent                              │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────┘
```

**Flow when `created` or `joined` is emitted:**
1. Call `matchState.reset()` to clear previous state
2. Store `matchId` and `playerId` in `MatchFacadeService`
3. Navigate to `/match/{matchId}`

**Flow on init:**
1. Read `AuthService.playerId()` and pass to child components
2. If URL has `?deckId=`: pass to `MatchCreateComponent` as pre-selected deck

#### Scenario: LobbyPage navigates after match creation
- WHEN `MatchCreateComponent` emits `created`
- THEN `MatchStateService.reset()` SHALL be called
- THEN the router SHALL navigate to `/match/{matchId}`

#### Scenario: LobbyPage handles missing auth
- WHEN `AuthService.isAuthenticated()` is `false`
- THEN the page SHALL show a message "Iniciá sesión para jugar" with a link to `/auth/register`
