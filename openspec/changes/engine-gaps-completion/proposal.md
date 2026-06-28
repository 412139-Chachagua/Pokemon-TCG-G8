## Why

The engine has 7 implementation gaps identified across the task files (persona2, persona3, persona4): TurnManager lacks `advancePhase()`, evolvedThisTurn reset, and auto-resolve DRAW; handlers don't auto-end-turn after attack nor clean up specialConditions on retreat/evolve; and DTOs lack Jakarta validation. These gaps block correct game flow and defensive input handling.

## What Changes

- **TurnManager:** Add `advancePhase()` method, reset `evolvedThisTurn` in `startTurn`, auto-resolve DRAW phase on turn start
- **DeclareAttackHandler:** Auto-end-turn after successful attack by calling `advancePhase()` to BETWEEN_TURNS
- **RetreatActiveHandler:** Clear specialConditions on the Pokémon that retreats from Active to Bench
- **EvolvePokemonHandler:** Clear specialConditions on the Pokémon that evolves
- **DTOs:** Add `@NotBlank`/`@NotNull` Jakarta validation on `CreateMatchRequest`, `JoinMatchRequest`, `GameActionRequest`
- **Controllers:** Enable `@Valid` on controller endpoints
- **GlobalExceptionHandler:** Handle `MethodArgumentNotValidException` returning 400

## Capabilities

### New Capabilities
- `engine-turn-completion`: Complete TurnManager phase lifecycle and auto-resolve DRAW
- `engine-handler-cleanup`: Auto-end-turn after attack and specialConditions cleanup on retreat/evolve
- `dto-jakarta-validation`: Jakarta validation annotations on match DTOs

### Modified Capabilities
- `engine-gaps-completion` (new): Initial spec covering all gaps

## Impact

- **Backend:** 7 Java files modified across `engine/turn/`, `engine/handlers/`, `dtos/matches/`, `controllers/matches/`, `advice/`
- **Dependencies:** No new dependencies; Jakarta validation already included via `spring-boot-starter-validation`
- **Engine isolation preserved:** No Spring/JPA imports added to engine package
