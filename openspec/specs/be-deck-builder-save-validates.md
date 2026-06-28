# AI Proposal Spec: be-deck-builder-save-validates

## Change name

be-deck-builder-save-validates

## Purpose

Definir que guardar un mazo ejecute la validación previa en el backend.

## Requirements

### Requirement: Save action reuses deck validation
El sistema SHALL ejecutar las mismas validaciones al guardar que al validar.

#### Scenario: Save in create mode
- WHEN the user saves a new deck
- THEN the system SHALL run the full deck validation flow before creating it

#### Scenario: Save in edit mode
- WHEN the user saves an existing deck
- THEN the system SHALL run the full deck validation flow before updating it

#### Scenario: Validation fails on save
- WHEN validation finds an error
- THEN the system SHALL block persistence

## Non-goals

- No cambiar la UI del botón Guardar.
- No introducir reglas nuevas de validación.
