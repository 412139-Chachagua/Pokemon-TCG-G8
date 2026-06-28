## MODIFIED Requirements

### Requirement: Stadium card stays in play after being played

`PlayTrainerHandler` SHALL NOT move Stadium cards to the discard pile when played. Instead, the card SHALL be placed in `GameState.stadiumCardInstanceId` via `StadiumPlayResolver`. The Stadium SHALL persist between turns until replaced or removed by an effect.

#### Scenario: Stadium is placed in play zone
- **WHEN** a player plays a Stadium card
- **THEN** `PlayTrainerHandler` SHALL delegate to `StadiumPlayResolver`
- **THEN** `StadiumPlayResolver` SHALL assign the card to `GameState.stadiumCardInstanceId`
- **THEN** the card SHALL NOT be moved to the discard pile
- **THEN** `STADIUM_PLAYED` event SHALL be published

#### Scenario: Stadium replaced moves old one to discard
- **WHEN** a player plays a new Stadium while `stadiumCardInstanceId` is already set
- **THEN** `StadiumPlayResolver` SHALL move the old Stadium to its owner's discard pile
- **THEN** `STADIUM_REMOVED` event SHALL be published for the old Stadium
- **THEN** the new Stadium SHALL be assigned to `stadiumCardInstanceId`
- **THEN** `STADIUM_PLAYED` event SHALL be published for the new Stadium

#### Scenario: Stadium persists between turns
- **WHEN** a turn ends and a new turn begins
- **THEN** `TurnManager.startTurn()` SHALL NOT clear `stadiumCardInstanceId`
- **THEN** the Stadium SHALL remain in play

### Requirement: Only one Stadium at a time

`RuleValidator` SHALL validate that a Stadium card can only be played when no other Stadium is in play, unless the new Stadium explicitly replaces it (handled by `StadiumPlayResolver`).

#### Scenario: Second Stadium blocked
- **WHEN** a player tries to play a Stadium while one is already in play
- **THEN** the action SHALL be allowed (the existing behavior replaces the old Stadium)
