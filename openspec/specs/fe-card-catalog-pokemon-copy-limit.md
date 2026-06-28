# AI Proposal Spec: fe-card-catalog-pokemon-copy-limit

## Change name

fe-card-catalog-pokemon-copy-limit

## Purpose

Definir la restricción visual para no superar 4 copias de una carta Pokémon desde el catálogo.

## Requirements

### Requirement: Limit Pokémon cards to four copies
El sistema SHALL impedir que se agreguen más de 4 copias de la misma carta Pokémon al mazo desde el catálogo.

#### Scenario: Add first copies
- WHEN the user adds the same Pokémon card fewer than 4 times
- THEN the system SHALL allow the addition

#### Scenario: Reach the limit
- WHEN the user reaches 4 copies of the same Pokémon card
- THEN the system SHALL block any further additions of that card

#### Scenario: Limit feedback
- WHEN the add action is blocked by the copy limit
- THEN the system SHALL inform the user that the maximum has been reached

## Non-goals

- No cambiar la lógica del backend.
- No modificar otras reglas de validación.
