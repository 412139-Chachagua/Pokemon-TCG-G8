## Context

The frontend already has:
- `MatchFacadeService` — orchestrates create/join with signal storage (matchId, playerId, side, status)
- `MatchStateService` (FE-05) — signal-based store for game state, with `reset()` and `initialize()`
- `DeckApiService` — `listByPlayer(playerId)` available for deck selection dropdown
- `AuthService` — `playerId()` signal, `isAuthenticated()` signal
- `NotificationService` — toast notifications
- Shared components: `ButtonComponent`, `ModalComponent`, `LoadingSpinnerComponent`

## Goals / Non-Goals

**Goals:**
- Implement full lobby page with create/join/list/match-list UI
- Wire navigation to `/match/{id}` after successful create or join
- Handle unauthenticated users with login prompt
- Keep each lobby component focused on a single form/action

**Non-Goals:**
- No BE changes (existing match endpoints work)
- No changes to `MatchApiService` or `DeckApiService` signatures
- No real match listing from BE (stub only — no endpoint exists yet)
- No match page UI (covered in FE-07, FE-08)

## Decisions

| Decision | Rationale |
|---|---|
| Each form is a separate component (MatchCreate, MatchJoin, MatchList) | Keeps components small, testable, and reusable. LobbyPage composes them. |
| `MatchCreateComponent` and `MatchJoinComponent` both load decks via `DeckApiService.listByPlayer()` | Decks are per-player and needed for match creation. Simpler to load in each component than to pass via inputs. |
| `MatchFacadeService.getMatchState()` delegates to `MatchStateService.initialize()` | Avoids duplicate API calls. `MatchStateService` already handles WS connect + HTTP fetch + signal updates. |
| `LobbyPage` calls `MatchStateService.reset()` before navigating to `/match/{id}` | Ensures clean state from previous match. Matches FE-05 spec scenario for `reset()`. |
| `MatchListComponent` is a stub with placeholder data | No BE `/matches` list endpoint yet. The component structure and layout are ready for integration. |
| Navigation uses Angular `Router.navigate()` | Standard Angular routing. The `/match/:id` route already exists. |

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| `MatchFacadeService.getMatchState()` return type changes from `Observable<MatchStateResponse>` to `void` | No existing callers besides the lobby, and it's being replaced. The state arrives via `MatchStateService` signals instead. |
| Deck list might be large | Select dropdown handles this naturally. No virtualization needed for typical deck counts (< 50). |
| User navigates away during create/join API call | `takeUntil(destroyRef$)` pattern in component to avoid memory leaks. |
