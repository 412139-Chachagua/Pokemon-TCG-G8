# Spec: deck-save-button-validates

## Capability

The "Guardar" button SHALL run full deck validation before persisting.

## Requirements

### R1: Validate before create

WHEN the user clicks "Guardar" in create mode (`/decks/new`)
THEN the system SHALL call `facade.validate()` before calling `deckApi.create()`
AND SHALL only proceed to create if validation returns `valid: true`

### R2: Validate before update

WHEN the user clicks "Guardar" in edit mode (`/decks/:id/edit`)
THEN the system SHALL call `deckApi.validate(id)` before calling `deckApi.update()`
AND SHALL only proceed to update if validation returns `valid: true`

### R3: Show validation feedback on block

WHEN validation fails on save
THEN the system SHALL set `validationResult` to the validation response
AND SHALL show a warning notification
AND SHALL NOT send the save request

### R4: Reuse existing feedback UI

WHEN validation errors are displayed after a failed save
THEN they SHALL use the same `validationResult` signal and `DeckSummaryComponent` rendering as the "Validar" button
