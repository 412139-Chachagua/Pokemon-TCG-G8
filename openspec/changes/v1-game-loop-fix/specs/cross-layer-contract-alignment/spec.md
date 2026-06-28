## MODIFIED Requirements

### Requirement: RuleValidator accepts Active Pokémon for evolution
The RuleValidator for `EVOLVE_POKEMON` SHALL accept the target Pokémon whether it is in the Active position OR on the Bench. Previously only Bench targets were accepted.

#### Scenario: Evolve Active Pokémon passes validation
- **GIVEN** a player has an Active Pokémon with `cardDefinitionId = "xy1-4"` and `evolvedThisTurn = false`
- **AND** the player has a `xy1-6` card in hand that evolves from `xy1-4`
- **WHEN** the player executes `EVOLVE_POKEMON` with `targetPokemonInstanceId` pointing to the Active Pokémon
- **THEN** `RuleValidator.validate()` SHALL return `true`

#### Scenario: Evolve non-existent target fails validation
- **GIVEN** a player executes `EVOLVE_POKEMON` with `targetPokemonInstanceId` that is neither Active nor on Bench
- **WHEN** `RuleValidator.validate()` runs
- **THEN** it SHALL return `false` with error code `CARD_NOT_IN_PLAY`

### Requirement: GameAction provides type-safe payload accessors
`GameAction` SHALL provide helper methods `getPayloadString(String key)` and `getPayloadInt(String key)` that access the payload map and convert values to the expected type, tolerating both `String` and `UUID` object representations for UUID fields.

#### Scenario: getPayloadString reads UUID as string
- **GIVEN** a `GameAction` with payload containing `"targetPokemonInstanceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"`
- **WHEN** `action.getPayloadString("targetPokemonInstanceId")` is called
- **THEN** it SHALL return `"a1b2c3d4-e5f6-7890-abcd-ef1234567890"`
- **AND** `UUID.fromString(result)` SHALL NOT throw

#### Scenario: getPayloadInt reads integer value
- **GIVEN** a `GameAction` with payload containing `"handIndex": 2`
- **WHEN** `action.getPayloadInt("handIndex")` is called
- **THEN** it SHALL return `2`

#### Scenario: All handlers use getPayloadString for UUID fields
- **GIVEN** any handler that reads a UUID field from the payload (e.g., `targetPokemonInstanceId`, `newActivePokemonInstanceId`)
- **WHEN** the handler processes the action
- **THEN** it SHALL use `action.getPayloadString(key)` instead of casting directly with `(String) payload.get(key)`

### Requirement: CardAttackEntity maps base_damage column
`CardAttackEntity` SHALL include an `Integer baseDamage` field mapped to the `base_damage` column, and `CardMapper.toDetailResponse()` SHALL include this value in the attack DTO.

#### Scenario: base_damage appears in card detail
- **GIVEN** a card with an attack whose `base_damage = 30`
- **WHEN** `GET /api/cards/{id}` is called
- **THEN** the attack in the response SHALL include `"baseDamage": 30`
