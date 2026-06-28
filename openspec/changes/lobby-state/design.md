## Context

The `MatchListComponent` currently uses stub data. A `lobby-state.md` spec already defines the `GET /api/matches` endpoint and the `MatchListComponent` API. The backend `MatchController`, `MatchApplicationService`, and `MatchJpaRepository` exist with stubs for other operations. The frontend `MatchApiService` exists but lacks a `listMatches()` method.

## Goals / Non-Goals

**Goals:**
- Add `findByStatus` query to `MatchJpaRepository`
- Add `listAvailableMatches()` to `MatchApplicationService`
- Add `GET /api/matches` endpoint to `MatchController`
- Add `listMatches()` to `MatchApiService`
- Connect `MatchListComponent` to the real API, removing stubs

**Non-Goals:**
- Real-time lobby updates via WebSocket (future)
- Pagination or filtering beyond status
- Authentication/authorization (post-MVP)
- Lobby chat, player presence, or match creation UI changes

## Decisions

1. **Single `status` query param with default `WAITING`** — Simple, matches existing `MatchStatus` enum. The `MatchController` accepts an optional `status` param defaulting to `WAITING`, avoiding a dedicated endpoint per status.
2. **`findByStatus(String)` in JPA repository** — Straightforward Spring Data query method. No custom JPQL needed. The `MatchStatus` enum value is passed as its string representation.
3. **MatchListComponent calls API on init and refresh** — No polling or WebSocket subscription for v1. The existing `onRefresh()` and a manual refresh pattern keep complexity minimal.
4. **Error → empty array** — The component treats API errors as "no matches available", matching the stub behavior. Avoids error states that have no user-facing recovery in v1.

## Risks / Trade-offs

- **[Stale list]** The match list is static after first load. Players must manually refresh. Acceptable for v1; polling or WebSocket updates can be added later.
- **[No auth guard]** Unauthenticated users can list matches. This is consistent with the current project state where auth is postponed.
- **[No pagination]** A large number of WAITING matches could produce a long list. Acceptable for MVP with limited test users.
