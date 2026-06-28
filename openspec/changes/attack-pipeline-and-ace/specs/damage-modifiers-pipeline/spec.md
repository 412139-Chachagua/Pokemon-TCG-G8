## ADDED Requirements

### Requirement: AttackResolver applies damage modifiers from TurnFlags

`AttackResolver.calculateDamage()` SHALL accept and apply damage modifiers from `TurnFlags.damageModifiers`.

- SHALL read `damageModifiers` map keyed by `PokemonInPlay.instanceId.toString()`
- SHALL apply attacker modifiers (values associated with attacker instanceId) to `baseDamage`
- SHALL apply defender modifiers (values associated with defender instanceId) to `finalDamage`
- SHALL reset `TurnFlags.damageModifiers` to `null` at the start of each turn in `TurnManager.startTurn()`

#### Scenario: Attacker modifier increases base damage

- **WHEN** `TurnFlags.damageModifiers` contains an entry with the attacker's instanceId as key and `10` as value
- **AND** `calculateDamage()` computes `baseDamage = 50`
- **THEN** `finalDamage` SHALL be computed from `baseDamage + 10 = 60` before applying weakness/resistance

#### Scenario: Defender modifier reduces final damage

- **WHEN** `TurnFlags.damageModifiers` contains an entry with the defender's instanceId as key and `-20` as value
- **AND** `baseDamage * weaknessMultiplier + resistanceValue = 100`
- **THEN** `finalDamage` SHALL be `Math.max(100 + (-20), 0) = 80`

#### Scenario: No modifiers present

- **WHEN** `TurnFlags.damageModifiers` is `null` or empty
- **THEN** `calculateDamage()` SHALL behave identically to current behavior with no modifier adjustments

#### Scenario: Modifiers reset between turns

- **WHEN** `TurnManager.startTurn()` is called
- **THEN** `TurnFlags.damageModifiers` SHALL be set to `null`
