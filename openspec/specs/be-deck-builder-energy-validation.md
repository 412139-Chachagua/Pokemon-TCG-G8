# AI Proposal Spec: be-deck-builder-energy-validation

## Change name

be-deck-builder-energy-validation

## Purpose

Definir la validación backend de energías y tipos de Pokémon en mazos.

## Requirements

### Requirement: Validate energy and Pokémon type compatibility
El sistema SHALL verificar que la combinación de Pokémon y energías no produzca mazos inviables.

#### Scenario: Basic compatibility check
- WHEN a deck is validated
- THEN the system SHALL verify that the energy mix can support the Pokémon types in the deck

#### Scenario: Deck without usable energies
- WHEN the deck contains Pokémon whose attacks cannot be paid with any energy in the list
- THEN the system SHALL flag the deck as impossible or invalid

#### Scenario: Deck with poor energy balance
- WHEN the deck is technically valid but has very few compatible energies
- THEN the system SHALL warn that the deck may be difficult to play

#### Scenario: Mixed type deck
- WHEN the deck uses multiple Pokémon types
- THEN the system SHALL evaluate whether the energy base is broad enough to support them

## Non-goals

- No cambiar componentes de interfaz.
- No redefinir otras reglas de construcción.
