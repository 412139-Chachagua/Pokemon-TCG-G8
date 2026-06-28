## MODIFIED Requirements

Source spec: `openspec/specs/engine-p2/specs-engine-persona2/rule-validator-spec.md`

### Requirement: V1 action validations

The spec defines 7 V1 validations covering all actions listed in Contract 09:
`validateAttachEnergy`, `validatePutBasicOnBench`, `validateEvolve`, `validatePlayTrainer`, `validateRetreat`, `validateAttack`, `validateEndTurn`.

Each validation checks: phase, player turn, resource availability, and card type constraints per Contract 09.

#### Scenario: AttachEnergy rejects when energy already attached
- **WHEN** `hasAttachedEnergy` is true
- **THEN** validation fails with `ENERGY_ALREADY_ATTACHED`

#### Scenario: PutBasicOnBench rejects when bench is full
- **WHEN** bench has 5 Pokémon
- **THEN** validation fails with `BENCH_FULL`

#### Scenario: Evolve rejects when Pokémon entered this turn
- **WHEN** `enteredTurnNumber == turnNumber`
- **THEN** validation fails with `EVOLVE_NOT_ALLOWED`

#### Scenario: PlayTrainer rejects when Supporter already played
- **WHEN** `hasPlayedSupporter` is true and `trainerSubtype == TrainerSubtype.SUPPORTER`
- **THEN** validation fails with `SUPPORTER_ALREADY_PLAYED`

#### Scenario: Retreat rejects when Active is ASLEEP or PARALYZED
- **WHEN** Active Pokémon has `ASLEEP` or `PARALYZED` in `specialConditions`
- **THEN** validation fails with `POKEMON_ASLEEP` or `POKEMON_PARALYZED`

#### Scenario: Attack rejects on first player's first turn
- **WHEN** `turnNumber == 1` and `currentPlayerId == firstPlayerId`
- **THEN** validation fails with `CANNOT_ATTACK_FIRST_TURN`

#### Scenario: EndTurn is always valid for current player
- **WHEN** `playerId == currentPlayerId`
- **THEN** validation passes

### Requirement: V2+ no-op stubs

`validateDrawCard`, `validateTakePrizeCard`, `validateChooseKnockoutReplacement` exist as no-op stubs returning `true` without validation. They exist for compatibility with `GameEngine.applyAction()` but are out of V1 scope per Contract 09 and divisionEngine.md.

#### Scenario: Stubs return true
- **WHEN** any out-of-V1-scope action type is validated
- **THEN** `validate()` returns `true` with no error

### Requirement: Dependencies

`RuleValidator` may depend on `CardLookupPort`, `engine/model/*`, `engine/action/*`, `engine/EngineContext`, engine enums, and `cards/domain/*` (TrainerSubtype). No Spring/JPA/repository dependencies.

#### Scenario: Dependencies are pure Java
- **WHEN** compiling `engine/rules/RuleValidator`
- **THEN** no import from Spring, JPA, repositories, services, or controllers

### Requirement: Side-effect free

All validation methods read-only. No mutation of `GameState`, `EngineContext`, phase, flags, or cards. No calls to `ctx.addEvent()`.

#### Scenario: Valid state is not mutated
- **WHEN** validating any action
- **THEN** `GameState`, `PlayerState`, `PokemonInPlay`, and `TurnFlags` remain unchanged
