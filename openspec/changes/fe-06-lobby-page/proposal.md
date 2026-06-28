## Why

The lobby page is currently a stub (`features/lobby/pages/lobby-page/lobby-page.ts`) with a placeholder message. Players cannot create matches, join existing ones, or see available matches. FE-05 already provides `MatchStateService`, `MatchInteractionService`, and the refactored `GameActionDispatcherService` — but the lobby itself has no UI to trigger match creation/join flows.

## What Changes

- Create `MatchCreateComponent` — form with player name + deck selector to create a match
- Create `MatchJoinComponent` — form with matchId + player name + deck selector to join a match
- Create `MatchListComponent` — displays available waiting matches (stub: no BE endpoint yet)
- Modify `LobbyPage` — compose all components, navigate on create/join
- Modify `MatchFacadeService.getMatchState()` — delegate to `MatchStateService.initialize()` instead of calling `MatchApiService` directly

## Capabilities

### New Capabilities
- `lobby-create-match`: Form to fill name, select deck, and create a new match. On success, navigates to `/match/{id}`.
- `lobby-join-match`: Form to enter match ID, name, and deck to join an existing match. On success, navigates to `/match/{id}`.
- `lobby-match-list`: List of available matches with quick-join (stub, placeholder for future BE endpoint).
- `lobby-auth-guard`: Shows login prompt when `AuthService.isAuthenticated()` is `false`.

## Impact

- `FE/src/app/features/lobby/components/match-create/`: 2 new files (`.ts`, `.html`)
- `FE/src/app/features/lobby/components/match-join/`: 2 new files (`.ts`, `.html`)
- `FE/src/app/features/lobby/components/match-list/`: 2 new files (`.ts`, `.html`)
- `FE/src/app/features/lobby/pages/lobby-page/lobby-page.ts`: modified (compose components, navigation)
- `FE/src/app/features/match/services/match-facade.service.ts`: modified (`getMatchState()` → delegates to `MatchStateService.initialize()`)
- New dependency on `MatchStateService` for `reset()` before navigation
- New dependency on `NotificationService` for error toasts
