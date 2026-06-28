## ADDED Requirements

### Requirement: ChooseKOReplacementHandler

The system SHALL include `ChooseKOReplacementHandler` that processes player selection of a replacement Pokemon after a KO.

#### Scenario: Valid replacement completes successfully
- **WHEN** a player sends `CHOOSE_KO_REPLACEMENT` with a valid `benchPokemonId`
- **THEN** the selected Pokemon SHALL move from bench to active position
- **THEN** `pendingKOReplacement` SHALL be set to `false`
- **THEN** `knockedOutPlayerId` SHALL be set to `null`
- **THEN** `KO_REPLACEMENT_DONE` event SHALL be published
- **THEN** the turn phase SHALL advance

#### Scenario: Invalid bench Pokemon is rejected
- **WHEN** a player sends `CHOOSE_KO_REPLACEMENT` with a Pokemon not on their bench
- **THEN** the engine SHALL return `GameError` with code `INVALID_TARGET`
- **THEN** `pendingKOReplacement` SHALL remain `true`

#### Scenario: Replacement attempted when not pending
- **WHEN** a player sends `CHOOSE_KO_REPLACEMENT` but `pendingKOReplacement` is `false`
- **THEN** `RuleValidator` SHALL reject the action

### Requirement: DeclareAttackHandler detects KO and delegates

`DeclareAttackHandler` SHALL detect when the opponent's Active Pokemon is knocked out, set pending KO replacement state, and delegate selection to the affected player instead of auto-selecting bench.get(0).

#### Scenario: Active Pokemon KO'd triggers replacement request
- **WHEN** `DeclareAttackHandler` applies damage that KOs the opponent's Active Pokemon
- **THEN** it SHALL set `pendingKOReplacement = true` in GameState
- **THEN** it SHALL set `knockedOutPlayerId` to the opponent's UUID
- **THEN** it SHALL publish `KO_REPLACEMENT_REQUIRED` event with the list of bench candidates
- **THEN** the turn phase SHALL NOT advance until the player chooses a replacement

#### Scenario: Empty bench causes instant loss
- **WHEN** the Active Pokemon is KO'd and the affected player has no benched Pokemon
- **THEN** the affected player SHALL lose the game immediately (GAME_OVER)
- **THEN** no `KO_REPLACEMENT_REQUIRED` event SHALL be published
