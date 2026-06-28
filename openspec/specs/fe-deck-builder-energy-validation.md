# AI Proposal Spec: fe-deck-builder-energy-validation

## Change name

fe-deck-builder-energy-validation

## Purpose

Definir cómo la UI del builder muestra la validación de energías y tipos de Pokémon.

## Requirements

### Requirement: Surface validation feedback
El sistema SHALL mostrar al usuario el resultado de la validación de forma clara.

#### Scenario: Validation warning
- WHEN the deck is playable but risky
- THEN the system SHALL display a warning message

#### Scenario: Validation error
- WHEN the deck is impossible to play
- THEN the system SHALL display an error message that blocks the invalid state

#### Scenario: Validation state in builder
- WHEN the builder receives validation results
- THEN the system SHALL update the validation summary and deck state accordingly

## Non-goals

- No cambiar la lógica de validación del backend.
- No modificar el catálogo.
