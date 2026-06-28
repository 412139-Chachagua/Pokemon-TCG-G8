## Context

`RuleValidator` es actualmente un stub que siempre retorna `true`. Su implementación real debe cubrir las validaciones V1 definidas en `rule-validator-spec.md` y Contract 09.

Actualmente `GameEngine.applyAction()` no invoca `RuleValidator.validate()` — el handler se ejecuta directamente sin validación previa.

## Goals / Non-Goals

**Goals:**

- Implementar `validateAttachEnergy`, `validatePutBasicOnBench`, `validateEvolve`, `validatePlayTrainer`, `validateRetreat`, `validateAttack`, `validateEndTurn` con lógica real
- Crear `validateDrawCard`, `validateTakePrizeCard`, `validateChooseKnockoutReplacement` como no-op stubs (fuera de V1)
- Integrar `RuleValidator.validate()` en `GameEngine.applyAction()` como paso previo al handler (step 5 del flujo de Persona 1)
- Registrar `RuleValidator` como `@Bean` en `GameEngineConfig`

**Non-Goals:**

- No modificar handlers, modelos, ports, ni enums del engine
- No mutar `GameState`, `EngineContext`, phase, flags ni cartas
- No implementar validación de energías suficientes para ataque (pertenece a `AttackResolver`)
- No modificar `TurnManager`, `SetupManager` ni `VictoryConditionChecker`

## Decisions

1. **Dispatch por GameActionType**: `validate()` mapea `action.getType()` al método específico (`validateAttachEnergy`, etc.). Acciones sin validación V1 (`DRAW_CARD`, `TAKE_PRIZE_CARD`, `CHOOSE_KNOCKOUT_REPLACEMENT`) retornan `true`.
2. **Optional<GameError> interno**: Cada `validate*()` retorna `Optional<GameError>`. Si está presente, `validate()` retorna `false` y `GameEngine.applyAction()` construye el `ActionResult` con error. Si vacío, retorna `true`.
3. **CardLookupPort para definiciones**: Las validaciones que necesitan datos del catálogo (ej: `evolvesFrom`, tipo de carta) usan `CardLookupPort.findById()`.
4. **Side-effect free**: Todos los métodos solo leen de `EngineContext` — no mutan estado, no llaman `ctx.addEvent()`.
5. **Three-strike pattern**: `GameEngine` primero verifica `matchId`/`status`/`currentPlayerId` (ya implementado), luego llama `RuleValidator.validate()` para validaciones de reglas, y finalmente ejecuta el handler.

## Risks / Trade-offs

- **CardLookupPort.findById() puede retornar null** → Cada lookup debe validar que el resultado no sea null antes de acceder a sus campos
- **Handlers duplican validación inline** → Algunos handlers tienen sus propios checks inline. En V1 la validación en RuleValidator y en handler coexiste (no se quita del handler). En V2+ se puede migrar completamente a RuleValidator.
- **No-op stubs en V1** → `validateDrawCard`, `validateTakePrizeCard`, `validateChooseKnockoutReplacement` existen pero no validan. En V2+ se implementarán con lógica real.
