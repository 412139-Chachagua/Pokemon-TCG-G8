## 1. Backend - Repository Layer

- [x] 1.1 Add `findByStatus(String status)` query method to `MatchJpaRepository`
- [x] 1.2 Verify the return type is `List<MatchEntity>`

## 2. Backend - Service Layer

- [x] 2.1 Add `listAvailableMatches()` method to `MatchApplicationService` that calls `matchJpaRepository.findByStatus("WAITING")` and maps results via `matchMapper.toMatchResponse()`
- [x] 2.2 Handle the `status` parameter: accept optional status string, default to `"WAITING"`

## 3. Backend - Controller Layer

- [x] 3.1 Add `@GetMapping` method to `MatchController` with optional `@RequestParam(defaultValue = "WAITING") String status`
- [x] 3.2 Return `ResponseEntity<List<MatchResponse>>` with the result of `matchApplicationService.listAvailableMatches()`

## 4. Frontend - API Service

- [x] 4.1 Add `listMatches(status?: string): Observable<MatchResponse[]>` method to `MatchApiService`
- [x] 4.2 Construct query params and call `this.apiClient.get<MatchResponse[]>('/matches', ...)`

## 5. Frontend - MatchListComponent

- [x] 5.1 Inject `MatchApiService` into `MatchListComponent`
- [x] 5.2 Implement `ngOnInit()` to call `loadMatches()`
- [x] 5.3 Implement `loadMatches()` that calls `matchApi.listMatches()` and updates the `matches` signal
- [x] 5.4 Handle errors by setting `matches` to an empty array
- [x] 5.5 Replace stub `onRefresh()` with a real call to `loadMatches()`
- [x] 5.6 Show player display name in the match row (not just match ID)

## 6. Testing - Backend

- [x] 6.1 Write unit test for `MatchApplicationService.listAvailableMatches()`
- [x] 6.2 Write integration test for `GET /api/matches` endpoint via `MatchController`

## 7. Testing - Frontend

- [x] 7.1 Write unit test for `MatchApiService.listMatches()`
- [x] 7.2 Write unit test for `MatchListComponent` loading matches on init

## 8. Verification

- [x] 8.1 Run backend build: `mvn clean test` from `BE/` (5 new tests pass, 1 pre-existing failure unrelated)
- [x] 8.2 Run frontend build: `npm test` from `FE/` (6 total, 6 SUCCESS)
