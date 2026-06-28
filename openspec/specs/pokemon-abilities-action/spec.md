## ADDED Requirements

### Requirement: USE_ABILITY action type
The system SHALL add `USE_ABILITY` to the `GameActionType` enum.

#### Scenario: USE_ABILITY is a valid action type
- **WHEN** a `GameAction` with `type: "USE_ABILITY"` is received
- **THEN** the system SHALL recognize it as a valid action type

### Requirement: USE_ABILITY request format
The system SHALL accept `USE_ABILITY` actions with payload containing `pokemonInstanceId` (String UUID) and `abilityName` (String). Additional fields may be required by specific resolvers (e.g., `targetPokemonInstanceId`, `energyCardInstanceId`).

#### Scenario: Valid USE_ABILITY request
- **WHEN** a `USE_ABILITY` action is sent with valid `pokemonInstanceId` and `abilityName`
- **THEN** the system SHALL process the action through the standard GameEngine flow

#### Scenario: Missing pokemonInstanceId
- **WHEN** a `USE_ABILITY` action is sent without `pokemonInstanceId` in payload
- **THEN** the system SHALL reject the action with error code `MISSING_TARGET`

#### Scenario: Missing abilityName
- **WHEN** a `USE_ABILITY` action is sent without `abilityName` in payload
- **THEN** the system SHALL reject the action with error code `ABILITY_NOT_FOUND`

### Requirement: UseAbilityHandler
The system SHALL create `UseAbilityHandler` implementing `GameHandler` that processes `USE_ABILITY` actions.

#### Scenario: Handler resolves ability successfully
- **WHEN** `UseAbilityHandler.handle()` is called with a valid action where ability exists, Pokemon is not incapacitated, ability not used yet, and resolver succeeds (ctx.getError() is null)
- **THEN** it SHALL: (1) extract pokemonInstanceId + abilityName, (2) find Pokemon, (3) resolve card definition, (4) find ability in definition, (5) check tracking, (6) get resolver from registry, (7) call resolver.resolve(), (8) **only if ctx.getError() == null**: add abilityName to pokemon.abilitiesUsedThisTurn, (9) emit ABILITY_USED event

#### Scenario: Handler does not register usage on resolver error
- **WHEN** `UseAbilityHandler` calls resolver.resolve() and ctx.getError() is not null afterward
- **THEN** it SHALL NOT add abilityName to abilitiesUsedThisTurn and SHALL emit ABILITY_BLOCKED event

#### Scenario: Handler sets error on missing ability
- **WHEN** `UseAbilityHandler` cannot find the ability name in PokemonCardDefinition.abilities
- **THEN** it SHALL set `ctx.setError()` with code `ABILITY_NOT_FOUND`

#### Scenario: Handler sets error on already used
- **WHEN** `UseAbilityHandler` finds the ability already in abilitiesUsedThisTurn
- **THEN** it SHALL set `ctx.setError()` with code `ABILITY_ALREADY_USED`

#### Scenario: Handler sets error on incapacitated Pokemon
- **WHEN** `UseAbilityHandler` finds the Pokemon has ASLEEP or PARALYZED condition
- **THEN** it SHALL set `ctx.setError()` with code `POKEMON_CANNOT_USE_ABILITY`

#### Scenario: Handler sets error on resolver not found
- **WHEN** `UseAbilityHandler` calls registry.get() and it returns null
- **THEN** it SHALL set `ctx.setError()` with code `ABILITY_NOT_FOUND`

### Requirement: Rule validation for USE_ABILITY
The system SHALL add `USE_ABILITY` validation to `RuleValidator.validate()` that checks: phase is MAIN, target Pokemon exists and belongs to player, ability exists in PokemonCardDefinition, ability not already used this turn, Pokemon is not ASLEEP or PARALYZED.

#### Scenario: Valid ability activation
- **WHEN** `USE_ABILITY` is validated with valid payload, MAIN phase, ability exists, not used yet, Pokemon not incapacitated
- **THEN** validation SHALL pass

#### Scenario: Wrong phase
- **WHEN** `USE_ABILITY` is validated outside MAIN phase
- **THEN** validation SHALL fail

#### Scenario: Pokemon not found
- **WHEN** `USE_ABILITY` is validated with non-existent pokemonInstanceId
- **THEN** validation SHALL fail

#### Scenario: Ability not found on Pokemon
- **WHEN** `USE_ABILITY` is validated with abilityName not in PokemonCardDefinition.abilities
- **THEN** validation SHALL fail

#### Scenario: Ability already used this turn
- **WHEN** `USE_ABILITY` is validated and abilityName is in PokemonInPlay.abilitiesUsedThisTurn
- **THEN** validation SHALL fail

#### Scenario: Pokemon is asleep
- **WHEN** `USE_ABILITY` is validated and Pokemon has ASLEEP condition
- **THEN** validation SHALL fail

#### Scenario: Pokemon is paralyzed
- **WHEN** `USE_ABILITY` is validated and Pokemon has PARALYZED condition
- **THEN** validation SHALL fail

### Requirement: Ability events
The system SHALL add `ABILITY_USED` and `ABILITY_BLOCKED` to `GameEventType`.

#### Scenario: Ability used event
- **WHEN** an ability is successfully activated
- **THEN** the system SHALL emit an event with message like "Greninja used Water Shuriken."

#### Scenario: Ability blocked event
- **WHEN** an ability activation is blocked by rules or resolver error
- **THEN** the system SHALL emit an event with message like "Greninja cannot use Water Shuriken: already used this turn."

### Requirement: Error codes for abilities
The system SHALL add `ABILITY_NOT_FOUND`, `ABILITY_ALREADY_USED`, `POKEMON_CANNOT_USE_ABILITY` to `ErrorCode`.

#### Scenario: Ability not found error
- **WHEN** a USE_ABILITY action references an ability that doesn't exist on the Pokemon
- **THEN** the error code SHALL be `ABILITY_NOT_FOUND`

#### Scenario: Ability already used error
- **WHEN** a USE_ABILITY action references an ability already used this turn
- **THEN** the error code SHALL be `ABILITY_ALREADY_USED`

#### Scenario: Pokemon cannot use ability error
- **WHEN** a USE_ABILITY action targets a Pokemon that cannot use abilities (ASLEEP/PARALYZED)
- **THEN** the error code SHALL be `POKEMON_CANNOT_USE_ABILITY`
