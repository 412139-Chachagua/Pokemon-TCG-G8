## 1. MatchFacadeService (modify)

- [ ] 1.1 Inject `MatchStateService` into `MatchFacadeService`
- [ ] 1.2 Change `getMatchState()` to call `this.matchState.initialize(mId)` instead of `this.matchApi.getMatchState(mId, pId)`
- [ ] 1.3 Change return type from `Observable<MatchStateResponse>` to `void`

## 2. MatchCreateComponent

- [ ] 2.1 Create `features/lobby/components/match-create/match-create.component.ts` with `playerId` input, `created` output
- [ ] 2.2 Add local signals: `loading`, `error`, `decks`, `playerName`, `selectedDeckId`
- [ ] 2.3 Implement `ngOnInit()` to call `DeckApiService.listByPlayer(playerId)` and populate `decks`
- [ ] 2.4 Implement form template with name input + deck selector dropdown + submit button
- [ ] 2.5 Implement `onSubmit()` — calls `MatchFacadeService.createMatch()`, on success emits `created`
- [ ] 2.6 Add optional `preSelectedDeckId` input to allow pre-selecting a deck from URL param
- [ ] 2.7 Handle error: set `error` signal + call `NotificationService.show()`

## 3. MatchJoinComponent

- [ ] 3.1 Create `features/lobby/components/match-join/match-join.component.ts` with `playerId` input, `joined` output
- [ ] 3.2 Add local signals: `loading`, `error`, `decks`, `matchIdField`, `playerName`, `selectedDeckId`
- [ ] 3.3 Implement `ngOnInit()` to load decks via `DeckApiService.listByPlayer(playerId)`
- [ ] 3.4 Implement `setMatchId(id: string)` method for external pre-fill
- [ ] 3.5 Implement form template with matchId + name + deck selector + submit button
- [ ] 3.6 Implement `onSubmit()` — calls `MatchFacadeService.joinMatch()`, on success emits `joined`
- [ ] 3.7 Handle error similarly to MatchCreateComponent

## 4. MatchListComponent

- [ ] 4.1 Create `features/lobby/components/match-list/match-list.component.ts` with `matchSelected` output
- [ ] 4.2 Add placeholder list with static data (stub — no BE endpoint)
- [ ] 4.3 Implement "Usar este" button that emits `matchSelected`
- [ ] 4.4 Implement "↻ Actualizar" button (stub: no action yet)
- [ ] 4.5 Show empty state message when no matches

## 5. LobbyPage (modify)

- [ ] 5.1 Inject `MatchStateService`, `MatchFacadeService`, `AuthService`, `Router`, `DestroyRef`
- [ ] 5.2 Add auth guard: if `!AuthService.isAuthenticated()`, show login prompt with link to `/auth/register`
- [ ] 5.3 Compose layout with `MatchCreateComponent`, `MatchListComponent`, `MatchJoinComponent`
- [ ] 5.4 Wire `created` event: call `matchState.reset()` + `matchFacade.reset()` + navigate to `/match/{id}`
- [ ] 5.5 Wire `joined` event: same as created
- [ ] 5.6 Wire `matchSelected` from MatchListComponent → pre-fill `MatchJoinComponent.setMatchId()`
- [ ] 5.7 Pass `playerId` from `AuthService.playerId()` to child components

## 6. Build verification

- [ ] 6.1 Run `ng build` and fix any compilation errors
- [ ] 6.2 Run `tsc --noEmit` to verify type correctness
