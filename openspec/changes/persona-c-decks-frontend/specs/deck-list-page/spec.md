## ADDED Requirements

### Requirement: Deck list page loads decks by player
The system SHALL allow loading decks by `playerId` using `DeckApiService.listByPlayer(playerId)`.

#### Scenario: PlayerId loaded by button
- **WHEN** the user enters a `playerId` and clicks "Cargar mazos"
- **THEN** the system SHALL call `DeckApiService.listByPlayer(playerId)`
- **AND** render the returned decks in `DeckListComponent`

#### Scenario: PlayerId loaded on field change
- **WHEN** the user changes the `playerId` field
- **THEN** the system SHALL call `DeckApiService.listByPlayer(playerId)`
- **AND** render the returned decks in `DeckListComponent`

#### Scenario: Empty playerId
- **WHEN** the user leaves the field empty
- **THEN** the system SHALL use the configured development default player id (`player-dev` in local development)

#### Scenario: Empty result
- **WHEN** the service returns an empty list
- **THEN** the system SHALL show "No hay mazos disponibles."

#### Scenario: Load error
- **WHEN** loading decks fails
- **THEN** the system SHALL show an error message and a "Reintentar" action

### Requirement: Deck item shows actions and validation badge
Each deck SHALL render name, total quantity, source, and validation badge.

#### Scenario: Valid deck
- **WHEN** `deck.validation.valid === true`
- **THEN** `DeckValidationComponent` SHALL show a green "✅ Válido" badge
- **AND** `DeckItemComponent` SHALL show the play action

#### Scenario: Invalid deck
- **WHEN** `deck.validation.valid === false`
- **THEN** `DeckValidationComponent` SHALL show a red "❌ Inválido" badge
- **AND** `DeckItemComponent` SHALL hide the play action

#### Scenario: Delete action
- **WHEN** the user selects "Eliminar"
- **THEN** the item SHALL show an inline confirmation with Cancelar and Eliminar ⚠️

#### Scenario: Delete confirmed
- **WHEN** the user confirms deletion
- **THEN** the system SHALL call `DeckApiService.delete(id)`
- **AND** remove the deck from the local list
- **AND** show a snackbar through `NotificationService`

#### Scenario: Validate action
- **WHEN** the user selects "Validar"
- **THEN** the system SHALL call `DeckApiService.validate(id)`
- **AND** update `deck.validation` in the local list
- **AND** show a snackbar through `NotificationService`

#### Scenario: Play action
- **WHEN** the user selects "Jugar"
- **THEN** the system SHALL navigate to `/lobby?deckId={id}`
- **AND** only expose the action when the deck is valid

### Requirement: Deck list page supports navigation
The page SHALL offer access to creation and editing of decks.

#### Scenario: New deck
- **WHEN** the user clicks "[+ Nuevo mazo]"
- **THEN** the system SHALL navigate to `/decks/new`

#### Scenario: Edit deck
- **WHEN** the user triggers edit on a deck
- **THEN** the system SHALL navigate to `/decks/{id}/edit`

### Requirement: Deck list uses Tailwind styling
The deck list page SHALL use Tailwind CSS utility classes for all styling.

#### Scenario: Deck list styling
- **WHEN** the deck list page renders
- **THEN** the layout, buttons and badges SHALL use Tailwind utility classes
