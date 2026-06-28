# AI Proposal Spec: be-deck-builder-random-valid-decks

## Change name

be-deck-builder-random-valid-decks

## Purpose

Definir la generación aleatoria de mazos válidos desde el backend.

## Requirements

### Requirement: Generate valid random decks
El sistema SHALL poder generar un mazo aleatorio que respete las reglas de construcción y sea jugable.

#### Scenario: Random deck is generated
- WHEN the user requests a random deck
- THEN the system SHALL build a deck that respects copy limits, deck size rules and card compatibility
- AND the system SHALL avoid impossible or extremely hard-to-play combinations

#### Scenario: Generated deck is invalid
- WHEN the generated deck does not satisfy the validation rules
- THEN the system SHALL regenerate the deck or refuse the result
- AND the system SHALL not persist an invalid deck

#### Scenario: Not enough compatible cards
- WHEN the available card pool cannot produce a valid deck
- THEN the system SHALL inform the caller that a valid random deck could not be produced

## Non-goals

- No modificar la UI.
- No cambiar las reglas oficiales del juego.
