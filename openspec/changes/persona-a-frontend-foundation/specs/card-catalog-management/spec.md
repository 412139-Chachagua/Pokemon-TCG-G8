## MODIFIED Requirements

### Requirement: DeckBuilderFacadeService expanded
The `DeckBuilderFacadeService` SHALL expand `DeckCardEntry` to include `name: string`, `supertype: string`, `isBasicEnergy: boolean`. The `addCard()` method SHALL accept `cardId`, `name`, `supertype`. The `removeCard()` method SHALL decrement quantity by 1 and only remove the entry when quantity reaches 0.

#### Scenario: addCard stores name and supertype
- **WHEN** `addCard('xy1-1', 'Charizard', 'POKEMON')` is called
- **THEN** the internal `DeckCardEntry` SHALL have `cardId: 'xy1-1'`, `name: 'Charizard'`, `supertype: 'POKEMON'`, `quantity: 1`

#### Scenario: addCard increments existing card
- **WHEN** `addCard('xy1-1', 'Charizard', 'POKEMON')` is called twice
- **THEN** the entry SHALL have `quantity: 2` (not two separate entries)

#### Scenario: removeCard decrements quantity
- **WHEN** a card has `quantity: 3` and `removeCard(cardId)` is called
- **THEN** the quantity SHALL become `2`

#### Scenario: removeCard removes at zero
- **WHEN** a card has `quantity: 1` and `removeCard(cardId)` is called
- **THEN** the entry SHALL be removed from the list entirely

#### Scenario: removeCard on non-existent card
- **WHEN** `removeCard('nonexistent')` is called
- **THEN** the list SHALL remain unchanged

### Requirement: Deck routes include edit path
The system SHALL add `{ path: ':id/edit', loadComponent: () => import('./pages/deck-builder-page/deck-builder-page').then(m => m.DeckBuilderPage) }` to `deck.routes.ts`.

#### Scenario: Navigate to deck edit
- **WHEN** the user navigates to `/decks/deck-123/edit`
- **THEN** the `DeckBuilderPage` component SHALL be loaded

### Requirement: App HTML includes notification placeholder
The system SHALL add the notification component placeholder to `app.html` so global notifications can render.

#### Scenario: Notification component in app template
- **WHEN** the app renders
- **THEN** `app.html` SHALL include the notification component selector
