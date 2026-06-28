## Context

The engine's TurnManager, handlers, and DTO layer have 7 implementation gaps identified across persona task files. TurnManager exists but lacks `advancePhase()` and auto-resolve DRAW. Handlers implement core logic but miss cleanup side-effects. DTOs have no input validation.

## Goals / Non-Goals

**Goals:**
- Add `advancePhase()` to TurnManager with DRAW→MAIN→ATTACK→BETWEEN_TURNS cycle
- Reset `evolvedThisTurn` per-player in `startTurn()`
- Auto-resolve DRAW phase (draw 1 card, flag hasDrawnForTurn, handle deck-out)
- Auto-advance to BETWEEN_TURNS after successful attack in DeclareAttackHandler
- Clear specialConditions on retreat (RetreatActiveHandler) and evolve (EvolvePokemonHandler)
- Add `@NotBlank`/`@NotNull` validation on CreateMatchRequest, JoinMatchRequest, GameActionRequest
- Enable `@Valid` in MatchController and GameActionController; handle validation errors

**Non-Goals:**
- Tests (covered by separate task)
- WebSocket or frontend changes
- New DTOs or entities
- Game engine architectural changes (only filling gaps)

## Decisions

1. **advancePhase as package-private** — Called internally by TurnManager and handlers; not exposed beyond engine package. The phase enum `TurnPhase` already defines the sequential order.
2. **Auto-resolve DRAW via internal method** — `startTurn` invokes a private `autoResolveDraw()` that delegates to `DrawCardHandler` logic rather than duplicating it, keeping engine isolation.
3. **specialConditions cleanup via setter** — Both RetreatActiveHandler and EvolvePokemonHandler call `pokemon.setSpecialConditions(null)` or `clear()`. Simple one-liner, no architectural impact.
4. **Auto-end-turn in DeclareAttackHandler** — After setting `hasAttacked = true`, call `TurnManager.advancePhase(state)` to move to BETWEEN_TURNS. The `endTurn` in TurnManager already handles BETWEEN_TURNS processing (status effects, switch player, set DRAW).
5. **Jakarta validation via existing starter** — `spring-boot-starter-validation` is already a dependency. No new imports needed.

## Risks / Trade-offs

- **[Auto-resolve DRAW changes turn start behavior]** Existing code may expect manual DRAW_CARD action. Mitigation: `startTurn` only auto-resolves if phase is DRAW and `hasDrawnForTurn` is false; existing flow still works for tests.
- **[Auto-end-turn after attack may skip other MAIN actions]** Attack already sets `hasAttacked = true`, preventing further attacks. Standard TCG rules say attack ends the turn, so this is correct behavior.
- **[Clear specialConditions on retreat/evolve]** If any code relies on conditions persisting after these operations, it will break. Per TCG rules, retreat and evolve both clear conditions (Reglas p.264, p.241).
