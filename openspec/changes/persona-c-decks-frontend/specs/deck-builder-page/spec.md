## ADDED Requirements

### Requirement: Deck builder loads create and edit modes
The editor page SHALL support create mode and edit mode.

#### Scenario: New mode
- **WHEN** the route is `/decks/new`
- **THEN** the system SHALL start with an empty deck

#### Scenario: Edit mode
- **WHEN** the route is `/decks/:id/edit`
- **THEN** the system SHALL load the deck via `DeckApiService.get(id)`
- **AND** hydrate `DeckBuilderFacadeService`

### Requirement: Deck search adds cards using CardApiService
The search SHALL query cards and allow selecting a card to add to the deck.

#### Scenario: Search cards
- **WHEN** the user types in the search field
- **THEN** the system SHALL call `CardApiService.searchCards()`

#### Scenario: Select a card
- **WHEN** the user clicks a card result
- **THEN** the system SHALL emit `cardSelected`
- **AND** call `DeckBuilderFacadeService.addCard(cardId, name, supertype)`

#### Scenario: CardItemComponent dependency missing
- **WHEN** `CardItemComponent` from Persona B is not yet available
- **THEN** the feature SHALL use `CardViewComponent` from shared as a temporary placeholder

### Requirement: Deck card list updates quantities
The deck list SHALL allow incrementing and decrementing quantities.

#### Scenario: Add quantity
- **WHEN** the user clicks `[+]`
- **THEN** the system SHALL increase the quantity
- **AND** disable `[+]` when the quantity is already 4

#### Scenario: Remove quantity
- **WHEN** the user clicks `[-]`
- **THEN** the system SHALL decrease the quantity
- **AND** remove the card from the list when the quantity becomes 0

### Requirement: Deck summary reflects total and validation state
The summary SHALL show the total card count and validation state.

#### Scenario: Not validated yet
- **WHEN** validation is `null`
- **THEN** the system SHALL show "Aún no validado."

#### Scenario: Valid deck
- **WHEN** validation.valid is true
- **THEN** the system SHALL show "✅ Listo para jugar."

#### Scenario: Invalid deck
- **WHEN** validation.valid is false
- **THEN** the system SHALL show the validation errors in detail

### Requirement: Deck builder validates before saving
The editor SHALL validate and save using `DeckApiService`.

#### Scenario: Validate new deck
- **WHEN** the deck has no id
- **THEN** the system SHALL call `DeckApiService.validateCards({ cards })`

#### Scenario: Validate existing deck
- **WHEN** the deck has an id
- **THEN** the system SHALL call `DeckApiService.validate(id)`

#### Scenario: Save new deck
- **WHEN** the user clicks "[Guardar]" in creation mode
- **THEN** the system SHALL call `DeckApiService.create(req)`
- **AND** navigate to `/decks` on success

#### Scenario: Save existing deck
- **WHEN** the user clicks "[Guardar]" in edit mode
- **THEN** the system SHALL call `DeckApiService.update(id, req)`
- **AND** navigate to `/decks` on success

#### Scenario: Save disabled
- **WHEN** the deck is empty or the name is blank
- **THEN** the save action SHALL be disabled

#### Scenario: Save error
- **WHEN** saving fails
- **THEN** the system SHALL show a snackbar and remain on the page

### Requirement: Deck builder uses Tailwind layout
The builder page SHALL use Tailwind CSS utility classes for the two-panel layout and mobile stacking.

#### Scenario: Builder styling
- **WHEN** the deck builder page renders
- **THEN** the two-panel desktop layout and stacked mobile layout SHALL use Tailwind utility classes

### Requirement: Layout adapts to mobile
The builder page SHALL use two panels on desktop and stacked on mobile, with breakpoint at 600px.

#### Scenario: Wide screen
- **WHEN** the viewport is 600px or wider
- **THEN** the search and builder panels SHALL render side by side

#### Scenario: Mobile screen
- **WHEN** the viewport is below 600px
- **THEN** the panels SHALL stack vertically with search on top
