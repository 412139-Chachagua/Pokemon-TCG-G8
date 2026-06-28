## MODIFIED Requirements

### Requirement: AttackResolver does NOT mutate on confusion self-hit

`AttackResolver.resolve()` SHALL NOT mutate the Pokemon's damage counters when a confused Pokemon hits itself. Instead it SHALL report the self-hit information in `AttackResolutionResult`.

#### Scenario: Confused Pokemon hits itself - resolver reports only
- **WHEN** a confused Pokemon attacks and the coin flip results in self-hit
- **THEN** `AttackResolver.resolve()` SHALL return `AttackResolutionResult` with `confusedSelfHit = true` and `selfDamageCounters = 3`
- **THEN** `AttackResolver.resolve()` SHALL NOT modify the attacker's damage counters
- **THEN** `AttackResolver.resolve()` SHALL NOT apply any damage to the defender

### Requirement: DeclareAttackHandler handles confusion self-hit as successful action

`DeclareAttackHandler` SHALL apply self-hit damage from confusion, check for KO, and treat the action as successfully completed.

#### Scenario: Confused Pokemon self-hit without KO
- **WHEN** `DeclareAttackHandler` receives `AttackResolutionResult` with `confusedSelfHit = true`
- **THEN** it SHALL apply 3 damage counters to the attacker
- **THEN** it SHALL check if the attacker is KO'd
- **THEN** if the attacker survives, it SHALL set `hasAttacked = true` on turn flags
- **THEN** it SHALL advance the turn phase
- **THEN** it SHALL publish `CONFUSION_SELF_HIT` and `DAMAGE_APPLIED` events
- **THEN** the action SHALL be reported as successful (no error)

#### Scenario: Confused Pokemon KOs itself
- **WHEN** `DeclareAttackHandler` applies self-hit damage and the attacker's HP reaches 0
- **THEN** it SHALL apply the KO: move attacker to discard, check for replacement
- **THEN** it SHALL publish `CONFUSION_SELF_HIT`, `DAMAGE_APPLIED`, and `POKEMON_KNOCKED_OUT` events
- **THEN** if the player has bench Pokemon, `KO_REPLACEMENT_REQUIRED` SHALL be published
