## MODIFIED Requirements

### Requirement: MatchFacadeService.getMatchState()

`MatchFacadeService.getMatchState()` SHALL delegate to `MatchStateService.initialize()` instead of calling `MatchApiService` directly.

**Current behavior:**
- `getMatchState()` returns `Observable<MatchStateResponse>` by calling `this.matchApi.getMatchState(mId, pId)`

**Modified behavior:**
- `getMatchState()` SHALL call `this.matchState.initialize(mId)` (returns `void`)
- The match state SHALL be available via `MatchStateService.publicState()` and `MatchStateService.privateState()` signals
- `MatchStateService` SHALL be injected into `MatchFacadeService`

#### Scenario: getMatchState delegates to MatchStateService
- WHEN `getMatchState()` is called with a valid matchId stored in the facade
- THEN `MatchStateService.initialize(matchId)` SHALL be called
- THEN `MatchStateService` SHALL connect WebSocket and fetch initial state
- THEN the caller SHALL NOT need to call `MatchApiService` directly
