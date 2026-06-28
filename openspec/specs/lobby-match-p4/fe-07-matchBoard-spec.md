# Spec: FE-07 — Match Board Structure

## Dependencias
- FE-05 stores (MatchState, MatchInteraction, GameActionDispatcher)
- `card-repository.service.ts`, `card-image.pipe.ts`, `energy-icon.pipe.ts`, `condition-icon.pipe.ts`
- `CardViewComponent`, `LoadingSpinnerComponent`
- `auth.service.ts`

## Archivos a crear
- `features/match/components/match-header/match-header.component.ts`
- `features/match/components/match-header/match-header.component.html`
- `features/match/components/pokemon-slot/pokemon-slot.component.ts`
- `features/match/components/pokemon-slot/pokemon-slot.component.html`
- `features/match/components/bench-zone/bench-zone.component.ts`
- `features/match/components/bench-zone/bench-zone.component.html`
- `features/match/components/prize-zone/prize-zone.component.ts`
- `features/match/components/prize-zone/prize-zone.component.html`
- `features/match/components/game-log/game-log.component.ts`
- `features/match/components/game-log/game-log.component.html`
- `features/match/components/player-area/player-area.component.ts`
- `features/match/components/player-area/player-area.component.html`
- `features/match/components/opponent-area/opponent-area.component.ts`
- `features/match/components/opponent-area/opponent-area.component.html`

## Archivos a modificar
- `features/match/pages/match-page/match-page.ts` (completar stub — inline template)

---

### Requirement: MatchHeaderComponent

**Inputs:** `publicState: PublicGameStateModel | null`, `myPlayerId: string | null`

**Display:**
```
┌───────────────────────────────────────────────────────────┐
│  Turno 3  |  Fase: MAIN  |  Jugador actual: Santi  |  ⭐ │
└───────────────────────────────────────────────────────────┘
```
- Show `⭐` if `currentPlayerId === myPlayerId`, `⏳` otherwise
- Show loading state if `publicState` is null
- `ChangeDetectionStrategy.OnPush`

#### Scenario: MatchHeader shows turn indicator
- WHEN `publicState.currentPlayerId` equals `myPlayerId`
- THEN the header SHALL display `⭐`
- WHEN they differ
- THEN the header SHALL display `⏳`

---

### Requirement: PokemonSlotComponent

**Inputs:**
- `pokemon: PublicPokemonSlotModel | null`
- `cardDef: CardDetailResponse | null`
- `isActive: boolean`
- `isOwn: boolean`
- `isHighlighted: boolean` (for valid selection targets)

**Empty slot display:**
```
┌──────────────────┐
│   (slot vacío)   │
└──────────────────┘
```

**Occupied slot:**
```
┌──────────────────────┐
│  [imagen]            │
│  Charizard           │
│  HP: 90 / 210        │
│  [F][F] · 🔥 BURNED  │
│  [T]                 │
└──────────────────────┘
```

**Behavior:**
- HP display = `cardDef.hp - pokemon.damageCounters`
- Energy icons via `EnergyIconPipe`
- Condition icons via `ConditionIconPipe`
- Card image via `cardId | cardImage : 'small'`
- If `cardDef === null`: show `LoadingSpinnerComponent`
- If `isHighlighted`: golden border
- `@HostListener('click')` emits output if in selection mode

**Outputs:** `slotClicked: PublicPokemonSlotModel | null`

#### Scenario: PokemonSlot shows HP correctly
- WHEN `pokemon.damageCounters` is 120 and `cardDef.hp` is 210
- THEN displayed HP SHALL be "90 / 210"

#### Scenario: PokemonSlot shows loading for unresolved card
- WHEN `pokemon` is not null but `cardDef` is null
- THEN `LoadingSpinnerComponent` SHALL be displayed

---

### Requirement: BenchZoneComponent

**Inputs:**
- `bench: PublicPokemonSlotModel[]`
- `cardDefs: Map<string, CardDetailResponse>`
- `isOwn: boolean`
- `validTargets: string[]`
- `selectionMode: SelectionMode`

**Outputs:** `slotClicked: PublicPokemonSlotModel | null`

**Display:**
- 5 slots in a horizontal row
- Slots with `instanceId` in `validTargets` have golden border
- Empty slots shown as `(slot vacío)`

---

### Requirement: PrizeZoneComponent

**Inputs:** `prizeCount: number`, `isOwn: boolean`

**Display:**
```
■ ■ ■ ■ ■ ■   ← 6 prizes (6 taken = 0 remaining shown)
```
- Fill `■` for remaining prizes, `□` for taken prizes
- If `isOwn`: bright colors. If `!isOwn`: muted colors.

---

### Requirement: GameLogComponent

**Inputs:** `events: GameEventDto[]`

**Display:**
```
┌──────────────────────────────────────────────────────┐
│  > Santi attached Fire Energy to Slugma.             │
│  > Slugma dealt 60 damage to Froakie.                │
│  > Turno 3 - Fase MAIN                               │
└──────────────────────────────────────────────────────┘
```

**Behavior:**
- Show last 10 events, most recent first
- Uses `event.message` for display text
- Auto-scroll to top on new events

---

### Requirement: OpponentAreaComponent

**Inputs:**
- `playerState: PublicPlayerStateModel | null`
- `cardDefs: Map<string, CardDetailResponse>`
- `validTargets: string[]`
- `selectionMode: SelectionMode`

**Outputs:** `pokemonClicked: PublicPokemonSlotModel`

**Display:**
- `PokemonSlotComponent` for active Pokemon
- `BenchZoneComponent` for bench (5 slots)
- `PrizeZoneComponent` for prizes
- Deck count (opponent's deck) — number only
- Discard count — number only

**Privacy:**
- SHOW: active Pokemon details, bench slot count, prize count
- HIDE: hand cards (show only `handCount`), deck order, prize content

---

### Requirement: PlayerAreaComponent

**Inputs:**
- `playerState: PublicPlayerStateModel | null`
- `privateState: PrivatePlayerStateModel | null`
- `cardDefs: Map<string, CardDetailResponse>`
- `validTargets: string[]`
- `selectionMode: SelectionMode`

**Outputs:** `pokemonClicked: PublicPokemonSlotModel`

**Display:**
- Same as OpponentAreaComponent for public state
- Hand count from `privateState`
- Deck count from `privateState`
- Discard count from `privateState`
- Prize details from `privateState`

---

### Requirement: MatchPage — Board assembly and polling

**MatchPageComponent** SHALL:
1. Read `matchId` from `ActivatedRoute` params
2. Read `playerId` from `AuthService.playerId()`
3. If no `playerId`: redirect to `/lobby`
4. Call `matchState.initialize(matchId)` — connects WebSocket + fetches initial state
5. Subscribe to `MatchStateService` signals
6. Preload card definitions via `CardRepositoryService.preload(cardIds)` for all Pokemon in play
7. Start polling fallback: `setInterval(() => { if disconnected, poll via REST }, 5000)`
8. On destroy: `clearInterval()` + `matchState.reset()`

**Layout:**
```
┌──────────────────────────────────────────────────┐
│  MatchHeaderComponent                             │
├──────────────────────────────────────────────────┤
│  OpponentAreaComponent                           │
│  ┌──────────────┐ ┌───────────────────────────┐  │
│  │ Active Pokemon│ │ Bench (5 slots)  Prizes   │  │
│  └──────────────┘ └───────────────────────────┘  │
├──────────────────────────────────────────────────┤
│  GameLogComponent                                │
├──────────────────────────────────────────────────┤
│  PlayerAreaComponent                             │
│  ┌──────────────┐ ┌───────────────────────────┐  │
│  │ Active Pokemon│ │ Bench (5 slots)  Prizes   │  │
│  └──────────────┘ └───────────────────────────┘  │
├──────────────────────────────────────────────────┤
│  HandZone (from FE-08)                           │
│  ActionPanel (from FE-08)                        │
└──────────────────────────────────────────────────┘
```

#### Scenario: MatchPage redirects without playerId
- WHEN navigating to `/match/:id` and `AuthService.playerId()` is null
- THEN the component SHALL redirect to `/lobby`

#### Scenario: MatchPage initializes game store
- WHEN component initializes with valid matchId and playerId
- THEN `MatchStateService.initialize(matchId)` SHALL be called
- THEN `CardRepositoryService.preload()` SHALL be called with all cardIds from active/bench Pokemon

#### Scenario: MatchPage polls when disconnected
- WHEN `matchState.connectionStatus()` equals `'DISCONNECTED'`
- THEN the component SHALL call `MatchApiService.getMatchState(matchId, playerId)` every 5 seconds

#### Scenario: MatchPage shows finished state
- WHEN `publicState.status` equals `'FINISHED'`
- THEN the victory overlay SHALL be displayed (FE-08 handles the overlay)
