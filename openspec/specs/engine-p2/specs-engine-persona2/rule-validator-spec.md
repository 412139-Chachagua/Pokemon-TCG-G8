# AI Proposal Spec: persona2-rule-validator

## Change name

persona2-rule-validator

---

# Depends on

OpenCode MUST read and obey:

- `openspec/specs/engine-persona2-master-spec.md`
- `BE/docs/contracts_ai/08-game-action-contract.md`
- `BE/docs/contracts_ai/09-rule-validation-contract.md`
- `BE/docs/contracts_ai/16-test-scenarios-contract.md`
- `docs/Reglas_Pokemon_TCG.md`

Los contratos del proyecto son la fuente de verdad.
Si este spec entra en conflicto con los contratos, los contratos ganan.

---

# Purpose

Implementar la lógica real de `engine/rules/RuleValidator`.

Este spec cubre únicamente:
- legalidad de acciones
- restricciones de turno
- restricciones de fase
- validaciones de precondiciones

---

# Existing ports — do not recreate

El siguiente port ya existe y NO debe ser recreado ni modificado:

- `engine/ports/CardLookupPort.java`

---

# Ownership

Persona 2 owns exclusively:

- `engine/rules/RuleValidator`

NO modificar ninguna clase fuera de este ownership.

---

# Non-goals

NO implementar:

- handlers
- combat
- damage
- attack execution
- KO resolution
- trainer effects
- persistence
- event sourcing
- validación de energías suficientes para atacar
  (eso pertenece a AttackResolver — Persona 3)

---

# Method signature

`RuleValidator` ya tiene una firma definida por Persona 1 que DEBE
mantenerse para compatibilidad con `GameEngine.applyAction()`:

```java
public boolean validate(EngineContext ctx, GameAction action)
```

Internamente, cada validación específica puede usar `Optional<GameError>`
para retornar el error descriptivo. Si hay error, `validate()` retorna
`false`. Si no hay error, retorna `true`.

NO cambiar esta firma bajo ninguna circunstancia.

---

# Side-effect constraint

`RuleValidator` MUST be side-effect free.

Los métodos de validación:
- NO mutan `GameState`
- NO mutan `EngineContext`
- NO cambian `phase`
- NO mueven cartas
- NO modifican flags
- NO llaman `ctx.addEvent()`

`RuleValidator` únicamente lee y retorna errores.

---

# Dependencies allowed

`RuleValidator` puede depender únicamente de:

- `CardLookupPort`
- `engine/model/*`
- `engine/action/*`
- `engine/EngineContext`
- enums del engine ya definidos por Persona 1
- enums de `cards/domain/*` (TrainerSubtype, etc.)

Solo usar campos ya expuestos por la jerarquía `CardDefinition` existente.
NO acceder a repositorios ni entidades JPA.

---

# Separation from AttackResolver

`RuleValidator.validateAttack()` valida precondiciones del turno:

- que sea el turno del jugador
- que no sea el primer turno del jugador que empieza
- que el Activo no tenga `ASLEEP` ni `PARALYZED`
- que el índice de ataque exista en la lista de ataques del Activo

`RuleValidator` NO valida si las energías son suficientes para el ataque.
Esa responsabilidad pertenece exclusivamente a `AttackResolver` (Persona 3).

---

# validateAttachEnergy

Rechazar si:

- la fase actual no es `MAIN`
- el `playerId` no es el `currentPlayerId`
- `TurnFlags.hasAttachedEnergy` es `true`
- la carta en `handIndex` no está en la mano del jugador
- la carta no es instancia de `EnergyCardDefinition`
- el target no es un Pokémon propio en juego (Activo o Bench)

---

# validatePutBasicOnBench

Rechazar si:

- la carta en `handIndex` no está en la mano del jugador
- la carta no es `PokemonCardDefinition` con `stage == "BASIC"`
- el bench del jugador ya tiene 5 Pokémon

---

# validateEvolve

Rechazar si:

- la fase actual no es `MAIN`
- la carta en `handIndex` no está en la mano del jugador
- la carta no es `PokemonCardDefinition` con `stage != "BASIC"`
- el target no está en juego (Activo o Bench)
- `target.enteredTurnNumber` es igual al `turnNumber` actual
- `target.evolvedThisTurn` es `true`
- el valor de `evolvesFrom` de la carta no coincide con el nombre
  del Pokémon target (consultar via `CardLookupPort`)

---

# validatePlayTrainer

Rechazar si:

- la fase actual no es `MAIN`
- la carta en `handIndex` no está en la mano del jugador
- la carta no es `TrainerCardDefinition`
- el `trainerSubtype` es `TrainerSubtype.SUPPORTER` y
  `TurnFlags.hasPlayedSupporter` es `true`

V1 no implementa efectos complejos de trainer.
Solo validar legalidad de jugar la carta.

---

# validateRetreat

Rechazar si:

- la fase actual no es `MAIN`
- `TurnFlags.hasRetreated` es `true`
- el Pokémon Activo del jugador tiene `ASLEEP` en `specialConditions`
- el Pokémon Activo del jugador tiene `PARALYZED` en `specialConditions`
- el bench del jugador está vacío
- la cantidad de energías adjuntas al Activo es menor que el
  `retreatCost` de la carta (cantidad total sin distinción de tipo
  para MVP)

---

# validateAttack

Rechazar si:

- el `playerId` no es el `currentPlayerId`
- la fase actual no es `MAIN`
- `turnNumber == 1` y `currentPlayerId == firstPlayerId`
- el Pokémon Activo tiene `ASLEEP` en `specialConditions`
- el Pokémon Activo tiene `PARALYZED` en `specialConditions`
- el `attackIndex` del payload no existe en la lista de ataques
  del Activo (consultar via `CardLookupPort`)

NO validar si las energías son suficientes para el ataque.
Eso pertenece a `AttackResolver` (Persona 3).

---

# validateEndTurn

Siempre válido si el `playerId` es el `currentPlayerId`.

---

# Out of V1 scope (no-op stubs)

Las siguientes acciones existen hoy en el engine (`DRAW_CARD`, `TAKE_PRIZE_CARD`,
`CHOOSE_KNOCKOUT_REPLACEMENT`) pero su validación queda **fuera del alcance V1**
según Contract 09 y la división de Persona 1.

En V1 sus métodos existen como **no-op stubs** (retornan `true` sin validar)
para mantener compatibilidad con `GameEngine.applyAction()`.

```java
public boolean validateDrawCard(EngineContext ctx, GameAction action) {
    // Existe hoy en GameActionType.DRAW_CARD pero está fuera del scope de V1
    return true;
}

public boolean validateTakePrizeCard(EngineContext ctx, GameAction action) {
    // Existe hoy en GameActionType.TAKE_PRIZE_CARD pero está fuera del scope de V1
    return true;
}

public boolean validateChooseKnockoutReplacement(EngineContext ctx, GameAction action) {
    // Existe hoy en GameActionType.CHOOSE_KNOCKOUT_REPLACEMENT pero está fuera del scope de V1
    return true;
}
```

---

# Escenarios de validación

- `validateAttachEnergy`: rechazar si `hasAttachedEnergy == true`
- `validateAttachEnergy`: rechazar si carta no es energía
- `validatePutBasicOnBench`: rechazar si bench tiene 5 Pokémon
- `validatePutBasicOnBench`: rechazar si carta no es Básico
- `validateEvolve`: rechazar si `enteredTurnNumber == turnNumber`
- `validateEvolve`: rechazar si `evolvedThisTurn == true`
- `validateRetreat`: rechazar si Activo tiene `ASLEEP`
- `validateRetreat`: rechazar si Activo tiene `PARALYZED`
- `validateAttack`: rechazar en turno 1 del primer jugador
- `validateAttack`: rechazar si Activo tiene `ASLEEP` o `PARALYZED`
- `validatePlayTrainer`: rechazar si Supporter ya fue jugado
- `validate()` retorna `true` para acción válida sin mutar estado
- Stubs out of V1 scope (`validateDrawCard`, `validateTakePrizeCard`,
  `validateChooseKnockoutReplacement`) retornan `true` sin validar

---

# Verification requirements

Debe:

- compilar con `mvn compile`
- pasar `mvn test`
- preservar `ApplicationTests.contextLoads`
- mantener compatibilidad con `GameEngine`
- no modificar clases fuera del ownership de Persona 2
- no introducir Spring, JPA ni acceso directo a repositorios

---

# Scope control

Este spec implementa únicamente validaciones y checks de legalidad.

No implementar:
- ejecución de acciones
- combat resolution
- handlers
- efectos de cartas
- damage
- victory conditions