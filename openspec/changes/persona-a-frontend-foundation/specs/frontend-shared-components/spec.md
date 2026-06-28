## ADDED Requirements

### Requirement: LoadingSpinnerComponent
The system SHALL create `shared/components/loading-spinner/loading-spinner.component.ts` with no inputs, rendering a centered spinner with "Cargando..." text. Uses `ChangeDetectionStrategy.OnPush`.

#### Scenario: Renders spinner
- **WHEN** `<app-loading-spinner>` is used in a template
- **THEN** it SHALL display a centered loading spinner with "Cargando..." text

### Requirement: ModalComponent
The system SHALL create `shared/components/modal/modal.component.ts` with inputs `title: string`, `open: boolean` and output `closed: void`. Renders dark overlay + centered panel + close button + `ClickOutsideDirective`.

#### Scenario: Open modal
- **WHEN** `open` input is `true`
- **THEN** the modal SHALL display with dark overlay, title, content projection, and close button

#### Scenario: Close modal on X click
- **WHEN** the user clicks the close button
- **THEN** the `closed` output SHALL emit

#### Scenario: Close modal on outside click
- **WHEN** the user clicks outside the modal panel
- **THEN** the `closed` output SHALL emit

### Requirement: ButtonComponent
The system SHALL create `shared/components/button/button.component.ts` with inputs `variant: 'primary'|'secondary'|'danger'|'ghost'`, `disabled: boolean`, `loading: boolean` and native click output. Applies CSS class per variant. Shows spinner if `loading`.

#### Scenario: Primary button style
- **WHEN** `variant` is `'primary'`
- **THEN** the button SHALL have primary color styling

#### Scenario: Loading state shows spinner
- **WHEN** `loading` is `true`
- **THEN** the button SHALL display a spinner and be visually disabled

### Requirement: NotificationComponent
The system SHALL create `shared/components/notification/notification.component.ts` that reads from `NotificationService` and renders notifications in the bottom-right corner. Auto-dismisses after duration.

#### Scenario: Shows notification
- **WHEN** `NotificationService.show('msg', 'success')` is called
- **THEN** a snackbar SHALL appear in the bottom-right corner with the message and success styling

#### Scenario: Dismiss on click
- **WHEN** the user clicks a notification
- **THEN** it SHALL be dismissed

### Requirement: CardViewComponent
The system SHALL create `shared/components/card-view/card-view.component.ts` with input `card: CardSummaryResponse`. Displays compact card view with image (via `CardImagePipe`), name, supertype, setCode. Falls back to placeholder on image error.

#### Scenario: Renders card summary
- **WHEN** `card` input is provided
- **THEN** it SHALL display the card image (small), name, supertype, and setCode

#### Scenario: Image error fallback
- **WHEN** the card image fails to load
- **THEN** a gray placeholder with the card name centered SHALL be displayed

### Requirement: PokemonCardComponent
The system SHALL create `shared/components/pokemon-card/pokemon-card.component.ts` with input `card: CardDetailResponse`. Displays full card detail: image, name, supertype, HP, attacks with energy costs, weaknesses, resistances, retreat cost, EX/MEGA badges.

#### Scenario: Renders full card detail
- **WHEN** `card` input is a Pokemon card
- **THEN** it SHALL display: large image, name, supertype, stage, HP, attacks (with energy cost icons via `EnergyIconPipe`), weaknesses, resistances, retreat cost, EX/MEGA badges

#### Scenario: EX badge shown
- **WHEN** `card.isEx` is `true`
- **THEN** an `[EX]` badge SHALL be displayed

#### Scenario: MEGA badge shown
- **WHEN** `card.isMega` is `true`
- **THEN** a `[MEGA]` badge SHALL be displayed
