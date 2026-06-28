## MODIFIED Requirements

### Requirement: Cached lookup
The system SHALL serve cached card definitions and invalidate the cache when the card catalog is re-synchronized.

#### Scenario: Cached lookup serves from cache
- WHEN the engine calls getCardById with the same card ID twice
- THEN the second call SHALL be served from cache
- AND no additional database query SHALL be made

#### Scenario: Cache invalidated on resync
- WHEN a card catalog sync completes via `POST /api/cards/sync`
- THEN the cards cache SHALL be cleared
- AND subsequent getCardById calls SHALL query the database for fresh data
