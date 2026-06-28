# AI Proposal Spec: fe-deck-builder-save-validates

## Change name

fe-deck-builder-save-validates

## Purpose

Definir que el botón Guardar use la misma validación que el botón Validar en la UI.

## Requirements

### Requirement: Save action reuses validation
El botón “Guardar” SHALL ejecutar la misma validación que el botón “Validar” antes de persistir el mazo.

#### Scenario: Save in create mode
- WHEN the user clicks “Guardar” while creating a deck
- THEN the system SHALL run the full deck validation flow
- AND the system SHALL only create the deck if validation succeeds

#### Scenario: Save in edit mode
- WHEN the user clicks “Guardar” while editing a deck
- THEN the system SHALL run the full deck validation flow
- AND the system SHALL only update the deck if validation succeeds

#### Scenario: Validation fails on save
- WHEN validation finds an error
- THEN the system SHALL block the save operation
- AND the system SHALL show the same validation feedback used by the “Validar” action

## Non-goals

- No cambiar la lógica de backend.
- No introducir una validación distinta para guardar.
