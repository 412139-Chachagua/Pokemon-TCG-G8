# AI Proposal Spec: fe-card-catalog-pagination-last-items

## Change name

fe-card-catalog-pagination-last-items

## Purpose

Definir que la UI del catálogo muestre también las últimas cartas de cada pestaña.

## Requirements

### Requirement: Render all cards returned for each tab
El sistema SHALL mostrar todas las cartas devueltas por la consulta de la pestaña actual.

#### Scenario: Last items are present
- WHEN a tab returns a page that includes the last available cards
- THEN the system SHALL render those cards in the list
- AND the system SHALL not drop the tail of the result set

#### Scenario: Pagination reaches the end
- WHEN the user navigates to the last page of a tab
- THEN the system SHALL show the remaining cards even if the page is not full

## Non-goals

- No cambiar la fuente de datos del catálogo.
- No modificar la API.
