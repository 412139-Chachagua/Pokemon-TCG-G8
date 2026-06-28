## ADDED Requirements

### Requirement: DeclareAttackHandler auto-ends turn after attack
The system SHALL automatically advance the phase to BETWEEN_TURNS after a successful attack, without requiring an explicit END_TURN action.

#### Scenario: Advance phase after successful attack
- **WHEN** `DeclareAttackHandler.handle` completes a successful attack (damage applied)
- **THEN** the phase SHALL advance to BETWEEN_TURNS via `TurnManager.advancePhase(state)`

#### Scenario: No phase change on failed attack
- **WHEN** the attack fails (energy insufficient, confused self-hit, invalid target)
- **THEN** the phase SHALL NOT change

### Requirement: RetreatActiveHandler clears specialConditions
The system SHALL clear all special conditions from a Pokémon when it retreats from Active to Bench.

#### Scenario: Clear conditions on retreat
- **WHEN** a Pokémon retreats from Active to Bench
- **THEN** its `specialConditions` list SHALL be cleared

### Requirement: EvolvePokemonHandler clears specialConditions
The system SHALL clear all special conditions from a Pokémon when it evolves.

#### Scenario: Clear conditions on evolve
- **WHEN** a Pokémon evolves (Basic→Stage1 or Stage1→Stage2)
- **THEN** its `specialConditions` list SHALL be cleared
