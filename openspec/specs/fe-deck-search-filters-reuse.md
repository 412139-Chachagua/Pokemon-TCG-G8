# AI Proposal Spec: fe-deck-search-filters-reuse

## Change name

fe-deck-search-filters-reuse

## Purpose

Definir la reutilización de filtros del catálogo en la búsqueda de cartas dentro del editor de mazos.

## Requirements

### Requirement: Reuse catalog filter behavior in deck search
El sistema SHALL usar el mismo comportamiento de filtros en el catálogo y en la búsqueda del editor de mazos.

#### Scenario: Same filter set
- WHEN the user applies the same filters in both screens
- THEN the system SHALL return consistent results

#### Scenario: Filter updates
- WHEN the user changes a filter in deck search
- THEN the system SHALL update the result list using the same filtering rules as the catalog

#### Scenario: Shared implementation
- WHEN a catalog filter component or service already exists
- THEN the deck editor search SHALL reuse it or its underlying logic

## Non-goals

- No modificar la lógica de backend.
- No redefinir el modelo de filtros.
