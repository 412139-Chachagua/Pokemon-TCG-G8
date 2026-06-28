# AI Proposal Spec: fe-deck-builder-random-valid-decks

## Change name

fe-deck-builder-random-valid-decks

## Purpose

Definir la acción de generación aleatoria de mazos en la interfaz del builder.

## Requirements

### Requirement: Trigger random deck generation
El sistema SHALL exponer una acción de UI para solicitar un mazo aleatorio.

#### Scenario: User requests a random deck
- WHEN the user clicks the random deck action
- THEN the system SHALL request a generated deck from the backend
- AND the system SHALL render the returned deck in the builder

#### Scenario: Generation error
- WHEN the backend cannot generate a valid deck
- THEN the system SHALL show an error message to the user

## Non-goals

- No definir algoritmos de generación.
- No modificar el catálogo de cartas.
