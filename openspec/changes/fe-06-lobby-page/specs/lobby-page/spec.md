## MODIFIED Requirements

### Requirement: LobbyPage

`LobbyPage` SHALL compose the full lobby view with all child components.

**Dependencies (new injections):**
- `AuthService` — get `playerId()` + check `isAuthenticated()`
- `MatchStateService` — call `reset()` before navigation
- `MatchFacadeService` — create/join operations, `reset()` 
- `Router` — navigate to `/match/{id}`
- `DestroyRef` — cleanup subscriptions

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
1. Call `matchState.reset()` to clear previous match state
2. Call `matchFacade.reset()` to clear facade state
3. Navigate to `/match/{matchResponse.id}` via `Router.navigate()`

**Flow on init:**
1. Read `AuthService.playerId()` and pass to child component inputs
2. If `AuthService.isAuthenticated()` is `false`: render login prompt instead of lobby
3. If URL has `?deckId=` query param: pass to `MatchCreateComponent` as pre-selected deck (via input)

**Auth guard display:**
```
┌──────────────────────────────────────┐
│  Iniciá sesión para jugar            │
│  [Ir a registro]                     │
└──────────────────────────────────────┘
```
- `[Ir a registro]` SHALL link to `/auth/register`

#### Scenario: LobbyPage navigates after match creation
- WHEN `MatchCreateComponent` emits `created`
- THEN `MatchStateService.reset()` SHALL be called
- THEN `MatchFacadeService.reset()` SHALL be called
- THEN the router SHALL navigate to `/match/{matchId}`

#### Scenario: LobbyPage navigates after match join
- WHEN `MatchJoinComponent` emits `joined`
- THEN `MatchStateService.reset()` SHALL be called
- THEN the router SHALL navigate to `/match/{matchId}`

#### Scenario: LobbyPage handles missing auth
- WHEN `AuthService.isAuthenticated()` is `false`
- THEN the page SHALL show "Iniciá sesión para jugar" with a link to `/auth/register`

#### Scenario: LobbyPage passes playerId to child components
- WHEN page initializes with authenticated user
- THEN `MatchCreateComponent.playerId` SHALL be set to `AuthService.playerId()`
- THEN `MatchJoinComponent.playerId` SHALL be set to `AuthService.playerId()`

#### Scenario: LobbyPage wires matchSelected to join form
- WHEN `MatchListComponent.matchSelected` emits a matchId
- THEN `MatchJoinComponent.setMatchId(matchId)` SHALL be called
