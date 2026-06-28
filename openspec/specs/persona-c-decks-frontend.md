# AI Proposal Spec: persona-c-decks-frontend

## Change name

persona-c-decks-frontend

## Purpose

Definir la implementación FrontEnd de **Persona C — Gestión de Mazos** dentro de `features/decks/` usando Tailwind CSS.

Este change cubre:

- FE-03 — Deck List Page
- FE-04 — Deck Builder Page

No modifica `features/cards/`, `features/lobby/`, `features/match/` ni `core/`.

## Mandatory context files

OpenSpec MUST read and obey:

- `FE/src/app/features/decks/routes.ts`
- `FE/src/app/features/decks/pages/deck-list-page/deck-list-page.ts`
- `FE/src/app/features/decks/pages/deck-builder-page/deck-builder-page.ts`
- `FE/src/app/features/decks/services/deck-builder-facade.service.ts`
- `FE/src/app/core/api/deck-api.service.ts`
- `FE/src/app/core/api/card-api.service.ts`
- `FE/src/app/core/services/notification.service.ts`
- `FE/src/app/shared/models/deck.models.ts`
- `FE/src/app/shared/models/card.models.ts`
- `FE/src/app/features/cards/` (para coordinar `CardItemComponent` con Persona B)

## Scope

El trabajo debe permanecer dentro de `FE/src/app/features/decks/`, solo consumir servicios/modelos ya existentes y construir la UI con Tailwind CSS.

No se debe introducir CSS global ni SCSS.

### Components and pages in scope

- DeckListPage
- DeckListComponent
- DeckItemComponent
- DeckValidationComponent
- DeckBuilderPage
- DeckSearchComponent
- DeckCardListComponent
- DeckSummaryComponent

## Requirements

### Requirement: Deck list page loads decks by player
La página de mazos SHALL permitir cargar la lista de mazos por `playerId` usando `DeckApiService.listByPlayer(playerId)`.

#### Scenario: PlayerId loaded by button
- **WHEN** the user enters a `playerId` and clicks “Cargar mazos”
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
- **THEN** the system SHALL show “No hay mazos disponibles.”

#### Scenario: Load error
- **WHEN** loading decks fails
- **THEN** the system SHALL show an error message and a “Reintentar” action

### Requirement: Deck item shows actions and validation badge
Cada mazo SHALL renderizar nombre, cantidad total, fuente y badge de validación.

#### Scenario: Valid deck
- **WHEN** `deck.validation.valid === true`
- **THEN** `DeckValidationComponent` SHALL show a green “✅ Válido” badge
- **AND** `DeckItemComponent` SHALL show the play action

#### Scenario: Invalid deck
- **WHEN** `deck.validation.valid === false`
- **THEN** `DeckValidationComponent` SHALL show a red “❌ Inválido” badge
- **AND** `DeckItemComponent` SHALL hide the play action

#### Scenario: Delete action
- **WHEN** the user selects “Eliminar”
- **THEN** the item SHALL show an inline confirmation with Cancelar and Eliminar ⚠️

#### Scenario: Delete confirmed
- **WHEN** the user confirms deletion
- **THEN** the system SHALL call `DeckApiService.delete(id)`
- **AND** remove the deck from the local list
- **AND** show a snackbar through `NotificationService`

#### Scenario: Validate action
- **WHEN** the user selects “Validar”
- **THEN** the system SHALL call `DeckApiService.validate(id)`
- **AND** update `deck.validation` in the local list
- **AND** show a snackbar through `NotificationService`

#### Scenario: Play action
- **WHEN** the user selects “Jugar”
- **THEN** the system SHALL navigate to `/lobby?deckId={id}`
- **AND** only expose the action when the deck is valid

### Requirement: Deck list page supports navigation
La página SHALL ofrecer acceso a creación y edición de mazos.

#### Scenario: New deck
- **WHEN** the user clicks “[+ Nuevo mazo]”
- **THEN** the system SHALL navigate to `/decks/new`

#### Scenario: Edit deck
- **WHEN** the user triggers edit on a deck
- **THEN** the system SHALL navigate to `/decks/{id}/edit`

### Requirement: Deck builder loads create and edit modes
La página de editor SHALL soportar modo creación y modo edición.

#### Scenario: New mode
- **WHEN** the route is `/decks/new`
- **THEN** the system SHALL start with an empty deck

#### Scenario: Edit mode
- **WHEN** the route is `/decks/:id/edit`
- **THEN** the system SHALL load the deck via `DeckApiService.get(id)`
- **AND** hydrate `DeckBuilderFacadeService`

### Requirement: Deck search adds cards using CardApiService
El buscador SHALL consultar cartas y permitir seleccionar una carta para agregar al mazo.

#### Scenario: Search cards
- **WHEN** the user types in the search field
- **THEN** the system SHALL call `CardApiService.searchCards()`

#### Scenario: Select a card
- **WHEN** the user clicks a card result
- **THEN** the system SHALL emit `cardSelected`
- **AND** call `DeckBuilderFacadeService.addCard(cardId, name, supertype)`

#### Scenario: CardItemComponent dependency missing
- **WHEN** `CardItemComponent` from Persona B is not yet available
- **THEN** the feature SHALL allow a temporary simple placeholder only to unblock development

### Requirement: Deck card list updates quantities
La lista del mazo SHALL permitir sumar y restar cantidades.

#### Scenario: Add quantity
- **WHEN** the user clicks `[+]`
- **THEN** the system SHALL increase the quantity
- **AND** disable `[+]` when the quantity is already 4

#### Scenario: Remove quantity
- **WHEN** the user clicks `[-]`
- **THEN** the system SHALL decrease the quantity
- **AND** remove the card from the list when the quantity becomes 0

### Requirement: Deck summary reflects total and validation state
El resumen SHALL mostrar el total de cartas y el estado de validación.

#### Scenario: Not validated yet
- **WHEN** validation is `null`
- **THEN** the system SHALL show “Aún no validado.”

#### Scenario: Valid deck
- **WHEN** validation.valid is true
- **THEN** the system SHALL show “✅ Listo para jugar.”

#### Scenario: Invalid deck
- **WHEN** validation.valid is false
- **THEN** the system SHALL show the validation errors in detail

### Requirement: Deck builder validates before saving
El editor SHALL validar y guardar usando `DeckApiService`.

#### Scenario: Validate new deck
- **WHEN** the deck has no id
- **THEN** the system SHALL call `DeckApiService.validateCards({ cards })`

#### Scenario: Validate existing deck
- **WHEN** the deck has an id
- **THEN** the system SHALL call `DeckApiService.validate(id)`

#### Scenario: Save new deck
- **WHEN** the user clicks “[Guardar]” in creation mode
- **THEN** the system SHALL call `DeckApiService.create(req)`
- **AND** navigate to `/decks` on success

#### Scenario: Save existing deck
- **WHEN** the user clicks “[Guardar]” in edit mode
- **THEN** the system SHALL call `DeckApiService.update(id, req)`
- **AND** navigate to `/decks` on success

#### Scenario: Save disabled
- **WHEN** the deck is empty or the name is blank
- **THEN** the save action SHALL be disabled

#### Scenario: Save error
- **WHEN** saving fails
- **THEN** the system SHALL show a snackbar and remain on the page

### Requirement: Deck pages use Tailwind layout rules
La UI de Persona C SHALL implementarse con Tailwind CSS utilities.

#### Scenario: Deck list styling
- **WHEN** the deck list page renders
- **THEN** the layout, buttons and badges SHALL use Tailwind utility classes

#### Scenario: Deck builder styling
- **WHEN** the deck builder page renders
- **THEN** the two-panel desktop layout and stacked mobile layout SHALL use Tailwind utility classes

### Requirement: Layout adapts to mobile
La página de builder SHALL usar dos paneles en desktop y apilado en mobile, siguiendo el breakpoint mobile del proyecto (menor a 600px).

#### Scenario: Wide screen
- **WHEN** the viewport is wide enough for the desktop layout
- **THEN** the search and builder panels SHALL render side by side

#### Scenario: Mobile screen
- **WHEN** the viewport is in mobile size
- **THEN** the panels SHALL stack vertically with search on top

## Non-goals

No implementar:

- cambios en backend
- CSS global o SCSS
- `features/cards/`, `features/lobby/`, `features/match/`, `core/`
- reglas nuevas de juego
- sincronización de catálogo
- autenticación o JWT
- componentes fuera de `features/decks/`