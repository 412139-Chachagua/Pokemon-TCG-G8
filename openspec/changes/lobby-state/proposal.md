## Why

The lobby page currently uses a stub for listing available matches. Players cannot see available WAITING matches to join, blocking the core multiplayer flow. This change implements the `GET /api/matches` endpoint and connects the `MatchListComponent` to the real API.

## What Changes

- **Backend:** Add `GET /api/matches` endpoint to `MatchController` with optional `status` query parameter
- **Backend:** Add `findByStatus` query method to `MatchJpaRepository`
- **Backend:** Add `listAvailableMatches()` method to `MatchApplicationService`
- **Frontend:** Add `listMatches()` method to `MatchApiService`
- **Frontend:** Replace stub data in `MatchListComponent` with live API call

## Capabilities

### New Capabilities
- `lobby-list-matches`: Display available WAITING matches in a list for players to join

### Modified Capabilities

No existing capabilities have their requirements changed.

## Impact

- **Backend:** `MatchController` gains a new `GET /api/matches` endpoint; `MatchJpaRepository` gains a query method; `MatchApplicationService` gains a new service method
- **Frontend:** `MatchApiService` gains a new method; `MatchListComponent` no longer uses stubs
- **Dependencies:** None — this is self-contained within existing lobby feature
