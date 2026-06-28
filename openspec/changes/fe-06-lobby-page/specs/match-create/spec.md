## ADDED Requirements

### Requirement: MatchCreateComponent

`MatchCreateComponent` SHALL provide a form to create a match.

**Inputs:**
- `playerId: string` (from auth, passed by parent)
- `preSelectedDeckId?: string | null` (optional, from URL query param `?deckId=`, pre-selects a deck in the dropdown)

**Outputs:**
- `created: EventEmitter<MatchResponse>` (emits when match is successfully created)

**Local signals (private):**
- `loading: Signal<boolean>`
- `error: Signal<string | null>`
- `decks: Signal<DeckResponse[]>`
- `playerName: Signal<string>` (bound to name input)
- `selectedDeckId: Signal<string | null>` (bound to deck selector)

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
- On init: call `DeckApiService.listByPlayer(playerId)` → populate `decks` signal
- Select dropdown SHALL show deck names, value SHALL be deck id
- `[Crear partida]` button SHALL be disabled if name is empty, no deck selected, or `loading` is true
- On submit: call `MatchFacadeService.createMatch(name, selectedDeckId)`
- On success: set `loading = false`, emit `created` with the `MatchResponse`
- On error: set `loading = false`, set `error` signal, call `NotificationService.show(message, 'error')`
- `ChangeDetectionStrategy.OnPush`

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
- THEN `NotificationService.show()` SHALL be called with error message and type `'error'`

#### Scenario: MatchCreateComponent button disabled states
- WHEN `playerName()` is empty and `selectedDeckId()` is null
- THEN the submit button SHALL be disabled
- WHEN either field is filled
- THEN the submit button SHALL be enabled
- WHEN `loading()` is true
- THEN the submit button SHALL be disabled and show spinner
