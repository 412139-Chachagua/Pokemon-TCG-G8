## ADDED Requirements

### Requirement: Card models aligned with backend DTOs
The system SHALL define TypeScript interfaces in `shared/models/card.models.ts` that match the backend Java DTOs: `CardSummaryResponse`, `CardDetailResponse`, `PaginatedCardsResponse`, `AttackModel` (with `convertedEnergyCost`), `WeaknessModel`, `ResistanceModel`, `TrainerSubtype` type, and `isMega` field.

#### Scenario: CardSummaryResponse matches backend
- **WHEN** the frontend imports `CardSummaryResponse` from `card.models.ts`
- **THEN** it SHALL have fields: `id: string`, `name: string`, `supertype: string`, `setCode: string`, `number: string`, `imageSmallUrl: string`

#### Scenario: CardDetailResponse matches backend
- **WHEN** the frontend imports `CardDetailResponse` from `card.models.ts`
- **THEN** it SHALL have all fields matching the Java `CardDetailResponse` record: `id`, `name`, `supertype`, `subtypes`, `setCode`, `number`, `imageSmallUrl`, `imageLargeUrl`, `rulesText`, `hp`, `stage`, `evolvesFrom`, `types`, `attacks`, `weaknesses`, `resistances`, `retreatCost`, `isEx`, `isMega`

#### Scenario: AttackModel includes convertedEnergyCost
- **WHEN** the frontend defines `AttackModel`
- **THEN** it SHALL include `convertedEnergyCost: number` field (matching backend `AttackDto`)

### Requirement: Game action types aligned with backend
The system SHALL define `GameActionType` in `shared/models/game-action.models.ts` with exactly 10 values: 7 active (`PUT_BASIC_ON_BENCH`, `ATTACH_ENERGY`, `EVOLVE_POKEMON`, `PLAY_TRAINER`, `DECLARE_ATTACK`, `RETREAT_ACTIVE`, `END_TURN`) + 3 deprecated (`DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, `USE_ABILITY`) + `TAKE_PRIZE_CARD`.

#### Scenario: GameActionType includes all values
- **WHEN** the frontend imports `GameActionType`
- **THEN** it SHALL be a union type containing all 10 string values

#### Scenario: Deprecated actions marked
- **WHEN** a developer inspects `GameActionType`
- **THEN** `DRAW_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`, and `USE_ABILITY` SHALL have `@deprecated` JSDoc annotations

### Requirement: GameEventDto simplified
The system SHALL define `GameEventDto` with only `type: string`, `message: string`, and optional `payload?: Record<string, unknown>`.

#### Scenario: GameEventDto fields
- **WHEN** the frontend receives a game event from the backend
- **THEN** it SHALL be typed as `GameEventDto` with `type`, `message`, and optional `payload`

### Requirement: GameActionResponse typed with proper state interfaces
The system SHALL define `GameActionResponse` with `publicState: PublicGameStateModel | null` and `privateState: PrivatePlayerStateModel | null` (not `unknown`).

#### Scenario: GameActionResponse state fields typed
- **WHEN** the frontend processes a `GameActionResponse`
- **THEN** `publicState` SHALL be typed as `PublicGameStateModel | null`
- **AND** `privateState` SHALL be typed as `PrivatePlayerStateModel | null`

### Requirement: MatchStateResponse created
The system SHALL define `MatchStateResponse` in `shared/models/game-state.models.ts` with `matchId: string`, `publicState: PublicGameStateModel`, `privateState: PrivatePlayerStateModel`.

#### Scenario: MatchStateResponse envelope
- **WHEN** the frontend calls `GET /matches/{id}/state`
- **THEN** the response SHALL be typed as `MatchStateResponse`

### Requirement: PrizeSlotModel corrected
The system SHALL define `PrizeSlotModel` with `slot: number`, `known: boolean`, `cardId: string | null` (not `card: CardModel | null`).

#### Scenario: PrizeSlotModel card reference
- **WHEN** a prize slot is known
- **THEN** `cardId` SHALL contain the card definition ID string
- **AND** when unknown, `cardId` SHALL be `null`

### Requirement: Deck models expanded
The system SHALL rename `DeckModel` to `DeckResponse` and add `CreateDeckRequest` (with `name` + `cards`, NO `playerId`), `UpdateDeckRequest`, and `ValidateDeckRequest` in `shared/models/deck.models.ts`.

#### Scenario: DeckResponse matches backend
- **WHEN** the frontend imports `DeckResponse`
- **THEN** it SHALL have: `id`, `name`, `ownerPlayerId`, `source` (string), `totalCards`, `valid`, `cards: DeckCardModel[]`, `validation: DeckValidationModel`

#### Scenario: CreateDeckRequest matches backend
- **WHEN** the frontend creates a deck
- **THEN** the request SHALL have `name: string` and `cards: { cardId: string; quantity: number }[]` — NO `playerId`

### Requirement: UI state models created
The system SHALL create `shared/models/ui-state.models.ts` with `SelectionMode` type (`'NONE' | 'SELECT_BENCH_SLOT' | 'SELECT_TARGET_POKEMON' | 'SELECT_ATTACK' | 'SELECT_RETREAT_TARGET'`) and `SelectionState` interface.

#### Scenario: SelectionMode values
- **WHEN** the frontend imports `SelectionMode`
- **THEN** it SHALL be a union of 5 string values
