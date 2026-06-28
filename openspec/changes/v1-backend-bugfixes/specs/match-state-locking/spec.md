## ADDED Requirements

### Requirement: Match state locked per action
The system SHALL prevent concurrent modification of the same match state by two simultaneous action requests.

#### Scenario: Sequential requests are safe
- WHEN two concurrent `POST /api/matches/{id}/actions` requests arrive for the same match
- THEN one SHALL succeed and the other SHALL fail with HTTP 409 Conflict
- AND the match state SHALL NOT be corrupted

#### Scenario: Lock is per-match, not global
- WHEN two concurrent action requests arrive for different matches
- THEN both SHALL proceed concurrently without blocking each other

#### Scenario: Lock is released after save
- WHEN an action request completes (success or failure)
- THEN the lock for that match SHALL be released
- AND the next queued request SHALL be able to acquire it
