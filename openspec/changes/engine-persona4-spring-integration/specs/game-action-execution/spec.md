## ADDED Requirements

### Requirement: Execute action via REST endpoint

The system SHALL expose `POST /api/matches/{id}/actions` to submit a game action.

The endpoint SHALL accept a `GameActionRequest` JSON body with:
- `playerId` (UUID, required)
- `type` (String, required): the action type matching a `GameActionType` enum value
- `payload` (Map<String, Object>, optional): action-specific parameters
- `clientRequestId` (UUID, optional): idempotency key from the client

The endpoint SHALL return HTTP 200 with a `GameActionResponse`.

#### Scenario: Execute a valid action

- **WHEN** sending `POST /api/matches/{id}/actions` with a valid `GameActionRequest` for the current player
- **THEN** the system SHALL apply the action via `GameEngine.applyAction()`, persist the new state, publish events, and return HTTP 200 with `success=true` in the response

#### Scenario: Execute action when not player's turn

- **WHEN** sending `POST /api/matches/{id}/actions` where `playerId` is NOT the `currentPlayerId`
- **THEN** the system SHALL return HTTP 200 with `success=false` and an error code

#### Scenario: Execute action for non-existent match

- **WHEN** sending `POST /api/matches/{id}/actions` for a match ID that does not exist
- **THEN** the system SHALL return HTTP 404

### Requirement: Execute action via WebSocket

The system SHALL expose a WebSocket endpoint at `/app/matches/{matchId}/actions` to submit game actions in real time.

The WebSocket controller SHALL delegate to the same `MatchApplicationService.executeAction()` used by the REST endpoint.

Responses SHALL be published to `/topic/matches/{matchId}/events`.

#### Scenario: Submit action via WebSocket

- **WHEN** sending a `GameActionRequest` via STOMP to `/app/matches/{matchId}/actions`
- **THEN** the system SHALL process the action and send the result to `/topic/matches/{matchId}/events`

### Requirement: Idempotent action execution

If a `GameActionRequest` with the same `clientRequestId` is received twice, the system SHALL return the same result without re-applying the action.

#### Scenario: Duplicate request with same clientRequestId

- **WHEN** sending the same `GameActionRequest` twice with the same `clientRequestId`
- **THEN** both requests SHALL return the same `GameActionResponse` and the action SHALL only be applied once
