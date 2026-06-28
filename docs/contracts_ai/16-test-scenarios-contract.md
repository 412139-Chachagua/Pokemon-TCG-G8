# Test Scenarios Contract

## Goal

Define required tests and MVP test cases.

The TPI requires unit tests, integration tests, JaCoCo, and specific coverage for RuleValidator, AttackResolver, and StatusEffectManager.

## Backend test location

```
src/test/java/ar/edu/utn/frc/tup/piii/
```

No es `backend/src/test/java/com/pokemontcg/`.

## Frontend test location

```
frontend/src/app/
```

## Required backend unit tests

### RuleValidator

Must test:
- cannot act when not your turn
- cannot attach more than 1 Energy per turn
- cannot bench non-Basic Pokémon
- cannot bench when Bench has 5 Pokémon
- cannot attack on first turn if player started
- cannot attack without required Energy
- cannot retreat if ASLEEP
- cannot retreat if PARALYZED

### AttackResolver

Must test:
- damage calculation: base damage only
- weakness x2
- resistance -20
- minimum damage 0
- damage counters = final damage / 10
- energy requirement validation: sufficient types match
- energy requirement: COLORLESS wildcard matching
- energy requirement: insufficient energy → rejection
- confusion self-damage on tails
- poison/burn between-turn processing (private helper — test via TurnManager integration or direct method call)

### Status effects (within AttackResolver)

Must test:
- POISONED adds 1 damage counter between turns
- BURNED with tails adds 2 damage counters
- ASLEEP with heads is removed
- ASLEEP with tails remains
- PARALYZED is removed at proper between-turn timing
- ASLEEP, CONFUSED and PARALYZED replace each other
- BURNED and POISONED coexist

## Required integration tests

### Full basic match

Given:
- two players
- two seed decks

When:
- match is created
- second player joins
- setup completes
- players take turns
- attacks are declared

Then:
- state changes
- events are logged
- WebSocket publisher is called
- match can finish

### Knockout and prize

Given:
- defender has 50 HP
- attack deals 50 damage

Then:
- defender is knocked out
- attached cards are discarded
- attacker takes 1 Prize
- if defender has no Bench, attacker wins

### EX knockout

Given:
- defender is Pokémon-EX

Then:
- attacker takes 2 Prize cards

### Deck out

Given:
- current player's deck is empty at required draw

Then:
- current player loses
- opponent wins by DECK_OUT

## Existing tests

Los siguientes tests existen y cubren los escenarios requeridos:

- `RuleValidatorTest.java` — verificar cobertura contra los escenarios listados
- `TurnManagerTest.java`
- `DeckValidatorTest.java`
- `GameActionTest.java`
- `RetreatActiveHandlerTest.java`
- `TakePrizeCardHandlerTest.java`

## Example unit test case JSON fixture

```json
{
  "name": "attack_with_weakness",
  "attacker": {
    "type": "FIRE",
    "baseDamage": 30
  },
  "defender": {
    "weakness": {
      "type": "FIRE",
      "multiplier": 2
    },
    "resistance": null
  },
  "expected": {
    "finalDamage": 60,
    "damageCounters": 6
  }
}
```

## Required frontend E2E MVP

Flow:
1. Open lobby
2. Create match with seed deck
3. Join match in second session/browser
4. Setup completes
5. Current player attaches Energy
6. Current player attacks
7. Game log updates
8. Opponent state updates

## Coverage target

Backend:
- global >= 80%
- RuleValidator >= 90%
- AttackResolver >= 90%
- StatusEffectManager >= 90% (pendiente de implementación como componente separado)

## Rule

Tests must follow contracts.

If implementation changes a DTO or enum, tests must fail until the contract is updated.
