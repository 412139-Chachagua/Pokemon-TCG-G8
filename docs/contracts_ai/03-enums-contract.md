# Enums Contract

## Rule

These enum names and values are canonical.
Do not translate them.
Do not create aliases.
Do not create duplicated enum types.

## Backend location

```
ar.edu.utn.frc.tup.piii.engine/
ar.edu.utn.frc.tup.piii.cards/domain/
ar.edu.utn.frc.tup.piii.decks/domain/
ar.edu.utn.frc.tup.piii.matches/domain/
```

## Frontend location

```
frontend/src/app/shared/models/
```

## MatchStatus

- WAITING
- SETUP
- ACTIVE
- FINISHED

## TurnPhase

- DRAW
- MAIN
- ATTACK
- BETWEEN_TURNS

## PlayerSide

- PLAYER_ONE
- PLAYER_TWO

## CardSupertype

- POKEMON
- ENERGY
- TRAINER

## PokemonStage

- BASIC
- STAGE_1
- STAGE_2
- MEGA
- RESTORED

MEGA and RESTORED are not required in MVP gameplay, but may exist in card data.

## EnergyCardType

- BASIC
- SPECIAL

## TrainerSubtype

- ITEM
- SUPPORTER
- STADIUM
- ACE_SPEC

## EnergyType

- GRASS
- FIRE
- WATER
- LIGHTNING
- PSYCHIC
- FIGHTING
- DARKNESS
- METAL
- FAIRY
- COLORLESS

## TrainerType

- ITEM
- STADIUM
- SUPPORTER

## SpecialCondition

- ASLEEP
- BURNED
- CONFUSED
- PARALYZED
- POISONED

## GameActionType

- PUT_BASIC_ON_BENCH
- ATTACH_ENERGY
- EVOLVE_POKEMON
- PLAY_TRAINER
- RETREAT_ACTIVE
- DECLARE_ATTACK
- END_TURN
- DRAW_CARD
- CHOOSE_KNOCKOUT_REPLACEMENT
- TAKE_PRIZE_CARD
- USE_ABILITY

## GameEventType

- CARD_DRAWN
- VICTORY_DECIDED
- POKEMON_PLACED_ON_BENCH
- ENERGY_ATTACHED
- POKEMON_EVOLVED
- TRAINER_PLAYED
- RETREAT_EXECUTED
- DAMAGE_APPLIED
- KNOCKOUT_OCCURRED
- ATTACK_DECLARED
- PHASE_CHANGED
- STATE_UPDATED
- PRIZE_TAKEN
- MULLIGAN_REVEALED
- ABILITY_USED
- ABILITY_BLOCKED

## Event format

Events are `GameEventDto` objects with type, message and payload, returned in `GameActionResponse.events[]`:

```json
{
  "type": "ENERGY_ATTACHED",
  "message": "Santi attached Fire Energy to Slugma.",
  "payload": { "playerId": "player-1", "targetPokemonInstanceId": "ci-30" }
}
```

No plain string events exist. All events use the typed GameEvent/GameEventDto format.

## FinishReason

- KNOCKOUT
- PRIZES
- DECK_OUT
- CONCEDE

## DeckValidationError codes

- DECK_SIZE_INVALID
- DUPLICATE_CARDS
- MISSING_BASIC_POKEMON
- MORE_THAN_4_COPIES
- INVALID_DECK_FORMAT
- ACE_SPEC_LIMIT_EXCEEDED

## AbilityType

- ABILITY
- POKEMON_POWER
- POKEMON_BODY

## ErrorCode

- NOT_YOUR_TURN
- WRONG_PHASE
- MATCH_NOT_ACTIVE
- ENERGY_ALREADY_ATTACHED
- BENCH_FULL
- INSUFFICIENT_ENERGY
- CANNOT_ATTACK_FIRST_TURN
- POKEMON_ASLEEP
- POKEMON_PARALYZED
- RETREAT_ALREADY_USED
- SUPPORTER_ALREADY_PLAYED
- EVOLVE_NOT_ALLOWED
- CARD_NOT_IN_HAND
- INVALID_TARGET
- KNOCKOUT_REPLACEMENT_REQUIRED
- TOOL_ALREADY_EQUIPPED
- UNKNOWN_EFFECT_CODE
- MISSING_TARGET
- ABILITY_NOT_FOUND
- ABILITY_ALREADY_USED
- POKEMON_CANNOT_USE_ABILITY
