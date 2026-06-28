# AI Proposal Spec: be-card-catalog-pokemon-copy-limit

## Change name

be-card-catalog-pokemon-copy-limit

## Purpose

Definir el límite backend de copias al agregar cartas Pokémon al mazo.

## Requirements

### Requirement: Limit Pokémon cards to four copies
El sistema SHALL impedir que se agreguen más de 4 copias de la misma carta Pokémon al mazo.

#### Scenario: Add first copies
- WHEN the same Pokémon card is added fewer than 4 times
- THEN the system SHALL allow the addition

#### Scenario: Reach the limit
- WHEN the deck already has 4 copies of the same Pokémon card
- THEN the system SHALL reject any further addition of that card

## Non-goals

- No cambiar el comportamiento visual.
- No modificar otras categorías de cartas.
