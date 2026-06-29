# Sistema de Ataques

## Resumen

El sistema de ataques es uno de los subsistemas más complejos del motor de juego. Implementa un **patrón Chain of Responsibility** donde cada ataque pasa por una serie de pasos (steps) que validan, modifican y ejecutan el daño y los efectos secundarios. Además, utiliza un parser de efectos de texto (`TextEffectParser`) y un sistema de efectos post-daño extensible mediante `PostDamageEffect`.

## Arquitectura General

```
DeclareAttackHandler
  │
  ▼
AttackChainBuilder ──→ construye la cadena de AttackSteps
  │                        │
  │                        ▼
  │                  AttackContext (contexto compartido)
  │                        │
  ▼                        ▼
AttackTurnState ◄── Chain of Responsibility ──→ GameEngine
```

---

## `AttackChainBuilder`

**Ubicación:** `engine/attack/AttackChainBuilder.java`

Construye la cadena de pasos (steps) para cada ataque usando el patrón Chain of Responsibility.

### Steps en orden de ejecución:

| # | Step | Propósito |
|---|---|---|
| 1 | `ConditionCheckStep` | Verifica condiciones especiales del atacante (dormido, paralizado) |
| 2 | `EnergyCheckStep` | Verifica que haya energía suficiente para el ataque |
| 3 | `ConfusionCheckStep` | Maneja la autolesión por confusión |
| 4 | `PreDamageStep` | Aplica modificadores pre-daño (habilidades, herramientas) |
| 5 | `TargetSelectionStep` | Selecciona el objetivo del ataque |
| 6 | `PrerequisiteStep` | Evalúa requisitos adicionales del ataque (lanzamiento de moneda, etc.) |
| 7 | `ModifierStep` | Aplica modificadores de daño (debilidad, resistencia, bonificadores) |
| 8 | `DamageStep` | Calcula e inflige el daño base |
| 9 | `PostDamageEffectStep` | Aplica efectos secundarios del ataque (curar, quemar, cambiar, etc.) |
| 10 | `KnockoutCheckStep` | Verifica si algún Pokémon fue debilitado |

### Métodos Clave:

| Método | Descripción |
|---|---|
| `buildChain(...)` | Construye la cadena completa de steps para un ataque dado |
| `executeChain(AttackContext ctx)` | Ejecuta cada step secuencialmente |

---

## `AttackContext`

**Ubicación:** `engine/attack/AttackContext.java`

Objeto que transporta toda la información necesaria durante la ejecución de un ataque. Se va modificando a medida que avanza por la cadena de steps.

### Campos Principales:

| Campo | Descripción |
|---|---|
| `attacker` | Pokémon que realiza el ataque |
| `defender` | Pokémon objetivo del ataque |
| `attackCard` | Carta del ataque (con su efecto de texto) |
| `damage` | Daño calculado (se modifica durante el ataque) |
| `effects` | Lista de `AttackEffect` a aplicar después del daño |
| `coinFlipResults` | Resultados de lanzamientos de moneda |
| `modifiers` | Modificadores activos (debilidad x2, resistencia -20, etc.) |
| `isKnockout` | Indica si el ataque resultó en un KO |

---

## `AttackEffect` y `AttackEffectType`

**Ubicación:** `engine/attack/AttackEffect.java`, `engine/attack/AttackEffectType.java`

Representan los efectos secundarios que un ataque puede tener además del daño directo.

### `AttackEffectType` (enum):

| Efecto | Descripción |
|---|---|
| `APPLY_CONDITION` | Aplica una condición especial (quemado, dormido, etc.) |
| `HEAL_USER` | Cura al Pokémon atacante |
| `DISCARD_ENERGY` | Descarta energía del objetivo o del atacante |
| `SWITCH_DEFENDER` | Cambia el activo del oponente por uno de banca |
| `RECOIL` | Daño de retroceso al atacante |
| `DAMAGE_BENCH` | Inflige daño a Pokémon en banca |
| `DRAW_CARDS` | Roba cartas del mazo |
| `SEARCH_DECK` | Busca una carta en el mazo |
| `ATTACH_ENERGY` | Adjunta energía extra |
| `MOVE_ENERGY` | Mueve energía entre Pokémon propios |

Cada `AttackEffectType` se mapea a una implementación concreta de `PostDamageEffect`.

---

## `DamageCalculator`

**Ubicación:** `engine/attack/DamageCalculator.java`

Calcula el daño final considerando todos los modificadores activos.

### Flujo de Cálculo:

1. Daño base del ataque
2. +/‑ Modificadores por habilidades (ej: `FurCoatHook` reduce daño)
3. +/‑ Modificadores por energía especial (ej: Strong Energy +10)
4. ×2 por debilidad (si aplica)
5. −20 por resistencia (si aplica)
6. El resultado nunca puede ser negativo (mínimo 0)

---

## Steps Detallados

### `ConditionCheckStep`

**Ubicación:** `engine/attack/steps/ConditionCheckStep.java`

Verifica si el Pokémon atacante puede atacar según sus condiciones especiales:
- **Dormido**: lanzamiento de moneda, cara → despierta y ataca, cruz → no ataca
- **Paralizado**: no puede atacar, se elimina la parálisis al final del turno
- **Quemado/Envenenado**: no impiden atacar, pero aplican daño entre turnos

### `EnergyCheckStep`

**Ubicación:** `engine/attack/steps/EnergyCheckStep.java`

Valida que el Pokémon atacante tenga las energías requeridas para el ataque. Usa el `EnergyMatchingEngine` para verificar compatibilidad de tipos de energía.

### `ConfusionCheckStep`

**Ubicación:** `engine/attack/steps/ConfusionCheckStep.java`

Si el atacante está confundido:
1. Lanzamiento de moneda
2. Cara: ataca normalmente
3. Cruz: se autolesiona con 30 de daño y el ataque termina

### `PreDamageStep`

**Ubicación:** `engine/attack/steps/PreDamageStep.java`

Punto de extensión para aplicar efectos antes del cálculo de daño. Por ejemplo, habilidades como "Destiny Burst" que modifican el comportamiento del ataque.

### `TargetSelectionStep`

**Ubicación:** `engine/attack/steps/TargetSelectionStep.java`

Determina el objetivo del ataque:
- Ataques normales → Pokémon activo del oponente
- Ataques con "daño a banca incluido" → múltiples objetivos
- Ataques que requieren selección → espera evento del jugador

### `PrerequisiteStep`

**Ubicación:** `engine/attack/steps/PrerequisiteStep.java`

Evalúa requisitos del ataque como:
- Lanzamientos de moneda (cara para efecto, cruz para fallo)
- Descarte de energía como costo adicional
- Condiciones de estado del defensor

### `ModifierStep`

**Ubicación:** `engine/attack/steps/ModifierStep.java`

Aplica todos los modificadores activos al ataque:
- **Debilidad**: si el tipo del atacante es débil contra el defensor, daño ×2
- **Resistencia**: si el defensor resiste al tipo del atacante, daño −20
- **Habilidades**: hooks como `SpikyShieldHook` reducen daño
- **Herramientas**: efectos de cartas herramienta

### `DamageStep`

**Ubicación:** `engine/attack/steps/DamageStep.java`

Inflige el daño calculado al Pokémon defensor:
1. Toma el daño final del `AttackContext`
2. Reduce los PS del defensor
3. Registra el daño hecho para efectos posteriores

### `PostDamageEffectStep`

**Ubicación:** `engine/attack/steps/PostDamageEffectStep.java`

Aplica los efectos secundarios del ataque después de infligir daño. Utiliza un sistema de builders para construir las implementaciones de `PostDamageEffect` basadas en `AttackEffectType`.

#### Efectos Post-Daño Implementados:

| Clase | Efecto |
|---|---|
| `ApplyConditionEffect` | Aplica condición especial al defensor (quemado, dormido, etc.) |
| `HealUserEffect` | Cura PS al Pokémon atacante |
| `DiscardEnergyEffect` | Descarta energías del Pokémon objetivo |
| `SwitchDefenderEffect` | Cambia el activo del oponente por uno de banca |
| `RecoilEffect` | Inflige daño de retroceso al atacante |
| `DamageBenchEffect` | Inflige daño a uno o más Pokémon en banca |
| `DrawCardsEffect` | Hace robar cartas al jugador atacante |
| `SearchDeckEffect` | Busca una carta específica en el mazo |
| `AttachEnergyEffect` | Adjunta una carta de energía del mazo a un Pokémon |
| `MoveEnergyEffect` | Mueve energía entre Pokémon del jugador atacante |

Cada efecto implementa la interfaz funcional `PostDamageEffect` con el método `apply(AttackContext ctx)`.

### `KnockoutCheckStep`

**Ubicación:** `engine/attack/steps/KnockoutCheckStep.java`

Verifica si el Pokémon defensor fue debilitado (PS ≤ 0):
1. Si KO → el jugador atacante toma 1 carta de premio
2. El jugador defensor debe elegir un reemplazo de banca
3. Si no hay reemplazo → el atacante gana la partida

---

## `TextEffectParser`

**Ubicación:** `engine/attack/TextEffectParser.java`

Parsea el texto de efecto de un ataque (ej: "30+ Quemado. Descarta 1 energía.") y genera la lista de `AttackEffect` correspondiente.

### Formato:
```
<daño>[+/-<mod>] [<efecto1>. <efecto2>. ...]
```

### Ejemplos:
- `"30"` → solo 30 de daño
- `"50+ Quemado. Confundido."` → 50+ de daño, aplica quemado y confusión
- `"10× Lanzamiento de moneda. 10× por cada cara."` → ataque variable

---

## `StatusEffectManager`

**Ubicación:** `engine/attack/StatusEffectManager.java`

Centraliza la lógica de aplicación y verificación de condiciones especiales durante los ataques. Proporciona métodos auxiliares usados por los steps:

- `canAttack(PokemonInPlay pokemon)` → si el Pokémon puede atacar dadas sus condiciones
- `applyBurnDamage(PokemonInPlay pokemon)` → daño entre turnos por quemadura
- `applyPoisonDamage(PokemonInPlay pokemon)` → daño entre turnos por veneno
- `checkWakeUp(PokemonInPlay pokemon)` → lanzamiento para despertar
- `checkConfusion(PokemonInPlay pokemon)` → lanzamiento para confusión

---

## Manejo de Ataques desde la UI

### `DeclareAttackHandler`

**Ubicación:** `engine/handlers/DeclareAttackHandler.java`

Handler que recibe el evento del jugador declarando un ataque:
1. Valida que sea el turno del jugador
2. Valida que el ataque exista en el Pokémon activo
3. Crea el `AttackContext` inicial
4. Delega al `AttackChainBuilder` para construir y ejecutar la cadena

### `AttackTurnState`

**Ubicación:** `engine/turn/states/AttackTurnState.java`

Estado del turno que representa "estamos ejecutando un ataque". Maneja:
- La ejecución asincrónica de la cadena de pasos
- La interacción con el jugador cuando se requiere selección de objetivo
- La transición al estado post-ataque

---

## Resumen Visual del Flujo de Ataque

```
[Jugador declara ataque]
  │
  ▼
DeclareAttackHandler
  │
  ▼
AttackChainBuilder.buildChain()
  │
  ▼
┌─────────────────────────────────────┐
│ 1. ConditionCheckStep               │
│    ¿Dormido/Paralizado? → puede/no  │
├─────────────────────────────────────┤
│ 2. EnergyCheckStep                  │
│    ¿Energía suficiente?             │
├─────────────────────────────────────┤
│ 3. ConfusionCheckStep               │
│    ¿Confundido? → moneda            │
├─────────────────────────────────────┤
│ 4. PreDamageStep                    │
│    Hooks pre-daño                   │
├─────────────────────────────────────┤
│ 5. TargetSelectionStep              │
│    Seleccionar objetivo             │
├─────────────────────────────────────┤
│ 6. PrerequisiteStep                 │
│    Costos adicionales               │
├─────────────────────────────────────┤
│ 7. ModifierStep                     │
│    Debilidad, Resistencia, Hooks    │
├─────────────────────────────────────┤
│ 8. DamageStep                       │
│    Infligir daño                    │
├─────────────────────────────────────┤
│ 9. PostDamageEffectStep             │
│    Quemado, curar, cambiar, etc.    │
├─────────────────────────────────────┤
│10. KnockoutCheckStep                │
│    ¿KO? → Premios, reemplazo       │
└─────────────────────────────────────┘
  │
  ▼
[Resultado del ataque aplicado al GameState]
```
