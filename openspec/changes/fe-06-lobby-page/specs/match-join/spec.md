## ADDED Requirements

### Requirement: MatchJoinComponent

`MatchJoinComponent` SHALL provide a form to join an existing match.

**Inputs:**
- `playerId: string` (from auth, passed by parent)

**Outputs:**
- `joined: EventEmitter<MatchResponse>` (emits when match is successfully joined)

**Local signals (private):**
- `loading: Signal<boolean>`
- `error: Signal<string | null>`
- `decks: Signal<DeckResponse[]>`
- `matchIdField: Signal<string>` (bound to match ID input)
- `playerName: Signal<string>` (bound to name input)
- `selectedDeckId: Signal<string | null>` (bound to deck selector)

**Public method:**
- `setMatchId(id: string)`: updates `matchIdField` signal (called from `MatchListComponent`)

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
- On init: call `DeckApiService.listByPlayer(playerId)` → populate `decks`
- Select dropdown shows deck names, value is deck id
- `matchIdField` can be pre-filled via `setMatchId(id)` (called from MatchListComponent)
- `[Unirse]` button disabled if matchId, name, or deck not filled; or while loading
- On submit: call `MatchFacadeService.joinMatch(matchIdField, name, selectedDeckId)`
- On success: emit `joined` with the `MatchResponse`
- On error: set `error`, call `NotificationService.show(message, 'error')`
- `ChangeDetectionStrategy.OnPush`

#### Scenario: MatchJoinComponent loads decks on init
- WHEN component initializes with a valid `playerId`
- THEN `DeckApiService.listByPlayer(playerId)` SHALL be called

#### Scenario: MatchJoinComponent joins match
- WHEN user fills matchId, name, selects deck, and clicks "Unirse"
- THEN `MatchFacadeService.joinMatch(matchId, name, deckId)` SHALL be called
- THEN on success, `joined` SHALL emit

#### Scenario: MatchJoinComponent pre-fills matchId
- WHEN `setMatchId('abc-123')` is called externally
- THEN the matchId input field SHALL display `'abc-123'`
