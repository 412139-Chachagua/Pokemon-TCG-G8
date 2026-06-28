# AI Proposal Spec: be-card-catalog-pagination-last-items

## Change name

be-card-catalog-pagination-last-items

## Purpose

Definir la paginación del catálogo para no perder las últimas cartas devueltas por la API.

## Requirements

### Requirement: Return complete paginated results
El sistema SHALL devolver todas las cartas correspondientes a la página solicitada.

#### Scenario: Last items are present
- WHEN a request reaches the last page of a result set
- THEN the system SHALL return the remaining cards instead of dropping them

#### Scenario: Page size and total count
- WHEN the backend calculates pagination metadata
- THEN the system SHALL keep the total count consistent with the returned page data

## Non-goals

- No modificar la UI.
- No cambiar el contenido de las cartas.
