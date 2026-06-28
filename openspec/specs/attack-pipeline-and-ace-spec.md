# Attack Pipeline & Ace Spec Validation

Implementación de efectos posteriores al daño, modificadores de daño, condiciones especiales desde ataques, y validación de AS TÁCTICO en mazos.

**Referencia:** `docs/informe-validaciones-faltantes-backend.md` — Detalles 36, 31/34, 51 y 77 (prioridad 🔴 Crítico).

---

## Background / Context

### Problemas cubiertos

| # | Aspecto | Estado actual | Archivos clave |
|---|---------|---------------|----------------|
| 36 | Efectos posteriores al daño | ❌ No se aplican condiciones, descartes, curación, daño a banca, robo, etc. después del daño | `DeclareAttackHandler.java`, `AttackResolver.java` |
| 31/34 | Modificadores de daño | ❌ `attackerModifiers` y `defenderModifiers` hardcodeados en 0. `DamageModifyResolver` escribe en `TurnFlags.damageModifiers` pero `AttackResolver.calculateDamage()` nunca lo lee. | `AttackResolver.java`, `TurnFlags.java`, `DamageModifyResolver.java` |
| 51 | Condiciones desde ataques | ❌ `AttackResolver.applyCondition()` existe pero nunca es invocado desde `DeclareAttackHandler` | `DeclareAttackHandler.java`, `AttackResolver.java` |
| 77 | Límite AS TÁCTICO | ❌ `DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED` definido pero nunca verificado en `DeckValidator` | `DeckValidator.java`, `TrainerCardDefinition.java` |

### Arquitectura actual relevante

- `AttackResolver.resolve()` → `calculateDamage()` retorna `DamageCalcResult` con `baseDamage`, `weaknessMultiplier`, `resistanceValue`, `finalDamage`. Sin pipeline de modificadores.
- `DeclareAttackHandler.handle()` aplica daño y verifica KO, pero no ejecuta efectos del texto del ataque.
- `CardAttackEntity` tiene columnas `effect_code` y `effect_text` que no son consumidas por el engine.
- `TurnFlags.damageModifiers` (Map<String, Object>) existe y `DamageModifyResolver` lo puebla, pero nadie lo lee.
- `TrainerCardDefinition.isAceSpec` y `TrainerSubtype.ACE_SPEC` existen pero `DeckValidator` no los verifica.

---

## Requirements

### R1 — Pipeline de modificadores de daño (31/34)

- SHALL modificar `AttackResolver.calculateDamage()` para aceptar y aplicar modificadores desde `TurnFlags.damageModifiers`
- SHALL consultar `TurnFlags.damageModifiers` con la `instanceId` del Pokémon atacante y defensor como claves
- SHALL aplicar modificadores del atacante sobre `baseDamage` (ej: +10 por Bate) y modificadores del defensor sobre `finalDamage` (ej: -20 por Escudo Plateado)
- SHALL resetear `TurnFlags.damageModifiers` al iniciar cada turno en `TurnManager.startTurn()`
- Firma sugerida del refactor:

```java
private static DamageCalcResult calculateDamage(
    PokemonInPlay attacker,
    PokemonInPlay defender,
    CardLookupPort cardLookup,
    int attackIndex,
    Map<String, Object> damageModifiers  // NUEVO
)
```

### R2 — Sistema de efectos de ataque (36 + 51)

- SHALL crear `AttackEffectType` enum con los tipos de efecto que un ataque puede producir post-daño:

| Tipo | Comportamiento |
|------|---------------|
| `APPLY_SPECIAL_CONDITION` | Aplica una condición especial al defensor (Paralizado, Dormido, Confundido, Quemado, Envenenado) |
| `DISCARD_ENERGY` | Descarta N energías del defensor (el atacante elige cuáles) |
| `DAMAGE_BENCH` | Hace N daño a 1+ Pokémon en Banca del rival |
| `HEAL_USER` | Cura N contadores de daño del atacante |
| `DRAW_CARDS` | Roba N cartas |
| `SWITCH_AFTER_DAMAGE` | Cambia al atacante por uno de la Banca después del daño |
| `COIN_FLIP_BEFORE_DAMAGE` | Efecto condicionado a moneda previa al daño |
| `COIN_FLIP_AFTER_DAMAGE` | Efecto condicionado a moneda posterior al daño |

- SHALL crear `AttackEffect` como clase de dominio que modele un efecto individual:

```java
public class AttackEffect {
    private AttackEffectType type;
    private Map<String, Object> params;  // ej: {"condition": "PARALYZED"}, {"count": 2}, {"target": "BENCH"}
}
```

- SHALL agregar `List<AttackEffect> effects` a `PokemonCardDefinition.AttackDefinition`
- SHALL modificar `CardLookupAdapter.toPokemon()` para mapear `CardAttackEntity.effectCode` + `effectText` a `AttackEffect` (parsing del effectCode y effectText)
- SHALL crear interfaz `AttackEffectResolver` con Strategy pattern:

```java
public interface AttackEffectResolver {
    void resolve(EngineContext ctx, PokemonInPlay attacker, PokemonInPlay defender, AttackEffect effect, Map<String, Object> payload);
    AttackEffectType getType();
}
```

- SHALL crear `AttackEffectRegistry` similar a `TrainerEffectRegistry` para mapear `AttackEffectType` → `AttackEffectResolver`
- SHALL modificar `DeclareAttackHandler.handle()` para que después de aplicar daño (y antes de avanzar fase) ejecute la lista de `AttackEffect` del ataque usado
- SHALL invocar `AttackResolver.applyCondition()` desde el resolver de `APPLY_SPECIAL_CONDITION` (reutilizando la lógica existente)

### R3 — Resolvers concretos para xy1 (36 + 51)

Se requiere implementar resolvers para los efectos de ataque presentes en el set `xy1`:

| Resolver | Efecto |
|----------|--------|
| `ApplySpecialConditionResolver` | Aplica `SpecialCondition` al defensor usando `AttackResolver.applyCondition()`. Publica evento `STATUS_APPLIED`. |
| `DiscardEnergyResolver` | El atacante selecciona N energías del defensor para descartar. Requiere payload con `targetEnergies`. |
| `DamageBenchResolver` | Aplica N contadores de daño a Pokémon en Banca del rival. Requiere payload con `benchTargets`. |
| `HealUserResolver` | Remueve N contadores de daño del atacante. Publica evento `POKEMON_HEALED`. |
| `DrawCardsResolver` | Roba N cartas del mazo del atacante a su mano. Reutiliza lógica de `DrawCardHandler` o `DrawCardsResolver` de trainer. |
| `CoinFlipEffectResolver` | Resuelve un efecto condicionado a moneda. Según el resultado, aplica o no el efecto secundario. |

### R4 — Eventos nuevos

| Evento | Propósito | Payload |
|--------|-----------|---------|
| `STATUS_APPLIED` | Condición especial aplicada por ataque | `targetPokemonInstanceId`, `condition`, `sourceAttackName` |
| `ENERGY_DISCARDED` | Energía descartada por efecto de ataque | `targetPokemonInstanceId`, `discardedEnergies`, `count` |
| `BENCH_DAMAGE` | Daño aplicado a Pokémon en Banca | `targets: [{instanceId, damageCounters}]` |
| `ATTACK_EFFECT_RESOLVED` | Efecto de ataque ejecutado | `effectType`, `attackName`, `result` |

### R5 — Validación de AS TÁCTICO en DeckValidator (77)

- SHALL modificar `DeckValidator.validate()` para contar cartas AS TÁCTICO
- SHALL usar `cardLookupPort.getCardById()` para resolver cada `DeckCard.cardId` a su `CardDefinition`
- SHALL verificar si `def instanceof TrainerCardDefinition trainer && trainer.isAceSpec()`
- SHALL contar total de cartas AS TÁCTICO (considerando `DeckCard.quantity`)
- SHALL agregar error `DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED` si el total > 1
- SHALL validar antes del chequeo de `MORE_THAN_4_COPIES` (para consistencia, el orden sugerido: tamaño → ace spec → 4 copias → básico)

```java
// Lógica a agregar en DeckValidator.validate(), después del chequeo de tamaño:
int aceSpecCount = 0;
for (DeckCard dc : cards) {
    CardDefinition def = cardLookupPort.getCardById(dc.getCardId());
    if (def instanceof TrainerCardDefinition trainer && trainer.isAceSpec()) {
        aceSpecCount += dc.getQuantity();
    }
}
if (aceSpecCount > 1) {
    errors.add(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED);
}
```

---

## Architecture & Design Constraints

- SHALL usar patrón **Strategy** para `AttackEffectResolver`, análogo a `TrainerEffectResolver`
- SHALL reutilizar resolvers existentes del sistema de entrenador cuando aplique (ej: `DrawCardsResolver`, `HealResolver`)
- `AttackEffectRegistry` SHALL ser un componente separado (no acoplado a `TrainerEffectRegistry`)
- SHALL modelar `AttackEffect` como lista plana en `AttackDefinition`, no como pipeline anidado
- SHALL no modificar el schema de base de datos existente (`effect_code` y `effect_text` ya existen en `CardAttackEntity`)
- SHALL mantener retrocompatibilidad: ataques sin `effects` definidos continúan funcionando (lista vacía = solo daño)
- Los modificadores de daño (`TurnFlags.damageModifiers`) SHALL resetearse en `TurnManager.startTurn()` junto con los demás flags
- Para AS TÁCTICO: `TrainerCardDefinition.isAceSpec` y `TrainerSubtype.ACE_SPEC` ya existen; solo falta la validación en `DeckValidator`

---

## Files Affected

### Modificar
- `BE/.../engine/attack/AttackResolver.java` — integrar `damageModifiers` en `calculateDamage()`
- `BE/.../engine/handlers/DeclareAttackHandler.java` — ejecutar `AttackEffect` list después del daño
- `BE/.../engine/model/TurnFlags.java` — agregar reset de `damageModifiers` (o asegurar que `TurnManager` lo haga)
- `BE/.../engine/turn/TurnManager.java` — resetear `damageModifiers` en `startTurn()`
- `BE/.../cards/domain/PokemonCardDefinition.java` — agregar `List<AttackEffect> effects` a `AttackDefinition`
- `BE/.../engine/ports/impl/CardLookupAdapter.java` — mapear `effectCode`/`effectText` a `AttackEffect`
- `BE/.../services/decks/DeckValidator.java` — agregar validación de AS TÁCTICO

### Crear
- `BE/.../engine/attack/AttackEffectType.java` — enum con tipos de efecto de ataque
- `BE/.../engine/attack/AttackEffect.java` — clase de dominio para efecto de ataque
- `BE/.../engine/attack/AttackEffectRegistry.java` — facade de registro/resolución
- `BE/.../engine/attack/AttackEffectResolver.java` — interfaz Strategy
- `BE/.../engine/attack/resolvers/ApplySpecialConditionResolver.java`
- `BE/.../engine/attack/resolvers/DiscardEnergyResolver.java`
- `BE/.../engine/attack/resolvers/DamageBenchResolver.java`
- `BE/.../engine/attack/resolvers/HealUserResolver.java`
- `BE/.../engine/attack/resolvers/DrawCardsResolver.java` (o reutilizar el de trainer)
- `BE/.../engine/attack/resolvers/CoinFlipEffectResolver.java`
- `BE/.../engine/event/AttackEffectEvent.java` — eventos específicos de efectos de ataque

---

## Dependencias entre ítems

```
31/34 (modificadores)  ──→  AttackResolver.calculateDamage() ← depende de TurnFlags
       │
       ▼
36 (post-damage effects) ──→  DeclareAttackHandler.handle()  ← depende de AttackEffect system
       │
       ▼
51 (conditions from attacks) ──→  reutiliza AttackResolver.applyCondition() ← subconjunto de 36
       │
       ▼
77 (Ace Spec validation) ──→  DeckValidator.validate() ← independiente de los demás
```

Orden de implementación sugerido: 77 (independiente) → 31/34 (solo toca calculateDamage) → 51+36 (comparten el sistema AttackEffect).
