# Sistema de Condiciones Especiales

## Resumen

Las condiciones especiales (también conocidas como estados alterados) son efectos de estado que afectan a los Pokémon en juego. El sistema implementa 5 condiciones especiales que siguen las reglas oficiales del Pokémon TCG, divididas en dos categorías: volátiles y persistentes. Las condiciones se aplican durante los ataques (via `PostDamageEffect`), cartas de entrenador, o habilidades, y se verifican en múltiples puntos del flujo del juego.

## Arquitectura

```
SpecialCondition (enum de 5 condiciones)
│
├── Volátiles (mutuamente excluyentes)
│   ├── ASLEEP (Dormido)
│   ├── CONFUSED (Confundido)
│   └── PARALYZED (Paralizado)
│
├── Persistentes (pueden coexistir entre sí y con volátiles)
│   ├── BURNED (Quemado)
│   └── POISONED (Envenenado)
│
└── Manejadores
    ├── StatusEffectManager (aplicación/verificación centralizada)
    ├── ConditionCheckStep (verifica condiciones al atacar)
    └── ConfusionCheckStep (manejo específico de confusión)
```

---

## `SpecialCondition` (enum)

**Ubicación:** `engine/SpecialCondition.java`

Enum que define las 5 condiciones especiales del juego.

| Condición | Categoría | Efecto Principal |
|---|---|---|
| `ASLEEP` | Volátil | No puede atacar a menos que salga cara en lanzamiento |
| `CONFUSED` | Volátil | Riesgo de autolesión al atacar |
| `PARALYZED` | Volátil | No puede atacar ni retirarse |
| `BURNED` | Persistente | 20 de daño entre turnos |
| `POISONED` | Persistente | 10 de daño entre turnos (creciente en algunas reglas) |

### Reglas de Exclusividad:

- **Volátiles**: Un Pokémon solo puede tener una condición volátil a la vez. Si se aplica una nueva, reemplaza a la anterior.
- **Persistentes**: Pueden coexistir entre sí (un Pokémon puede estar quemado y envenenado al mismo tiempo) y también con una condición volátil.
- **Ejemplo**: Un Pokémon puede estar `ASLEEP + BURNED` pero no `ASLEEP + PARALYZED`.

---

## `StatusEffectManager`

**Ubicación:** `engine/attack/StatusEffectManager.java`

Clase central que maneja toda la lógica de condiciones especiales. Es utilizada por los steps de ataque y por el sistema de fin de turno.

### Métodos Clave:

| Método | Descripción |
|---|---|
| `canAttack(PokemonInPlay pokemon)` | Determina si el Pokémon puede atacar según sus condiciones |
| `applyBurnDamage(PokemonInPlay pokemon)` | Aplica 20 de daño por quemadura entre turnos |
| `applyPoisonDamage(PokemonInPlay pokemon)` | Aplica 10 de daño por veneno entre turnos |
| `checkWakeUp(PokemonInPlay pokemon)` | Lanzamiento de moneda: cara → despierta |
| `checkConfusion(PokemonInPlay pokemon)` | Lanzamiento de moneda: cara → ataca normal, cruz → autolesión 30 |
| `applyCondition(PokemonInPlay pokemon, SpecialCondition condition)` | Aplica una condición respetando reglas de exclusividad |
| `removeCondition(PokemonInPlay pokemon, SpecialCondition condition)` | Remueve una condición específica |
| `clearAllConditions(PokemonInPlay pokemon)` | Remueve todas las condiciones |
| `isVolatile(SpecialCondition c)` | Retorna true si es ASLEEP, CONFUSED o PARALYZED |
| `isPersistent(SpecialCondition c)` | Retorna true si es BURNED o POISONED |

### Lógica de `canAttack`:

1. Si está **DORMIDO**: lanzamiento de moneda
   - Cara: despierta → puede atacar
   - Cruz: sigue dormido → no puede atacar
2. Si está **PARALIZADO**: no puede atacar (la parálisis se cura al final del turno)
3. Si está **CONFUNDIDO**: lanzamiento de moneda (manejado por `ConfusionCheckStep`)
   - Cara: ataca normalmente
   - Cruz: se autolesiona con 30 de daño, termina el ataque
4. **QUEMADO** y **ENVENENADO**: no impiden atacar (solo hacen daño entre turnos)

---

## Steps Relacionados

### `ConditionCheckStep`

**Ubicación:** `engine/attack/steps/ConditionCheckStep.java`

Primer paso de la cadena de ataque. Verifica si el Pokémon atacante puede atacar dadas sus condiciones especiales.

#### Flujo:

1. Si el Pokémon está **DORMIDO**:
   - Realiza lanzamiento de moneda
   - Cara: remueve `ASLEEP`, continúa con el ataque
   - Cruz: el ataque falla, termina el turno
2. Si el Pokémon está **PARALIZADO**:
   - El ataque falla automáticamente
   - Remueve `PARALYZED` (se cura después del intento fallido de ataque)
   - Termina el turno
3. Si el Pokémon está **CONFUNDIDO**:
   - Delega en `ConfusionCheckStep` para el manejo específico
4. Si el Pokémon está **QUEMADO** o **ENVENENADO**:
   - No afectan la capacidad de atacar

### `ConfusionCheckStep`

**Ubicación:** `engine/attack/steps/ConfusionCheckStep.java`

Maneja específicamente el estado de confusión durante el ataque.

#### Flujo:

1. Lanzamiento de moneda
2. **Cara**: el Pokémon ataca normalmente (no hay autolesión)
3. **Cruz**: el Pokémon se autolesiona con 30 de daño:
   - Aplica 30 de daño a sí mismo
   - El ataque termina (no se ejecutan los pasos siguientes)
   - La confusión **no se cura** (sigue confundido)

---

## Aplicación de Condiciones Especiales

### Desde Ataques:

Las condiciones especiales se aplican como efectos post-daño a través del `PostDamageEffectStep`. El `AttackEffectType.APPLY_CONDITION` se mapea a `ApplyConditionEffect`:

1. El texto del ataque especifica la condición (ej: "Quemado", "Dormido")
2. `TextEffectParser` parsea el texto y crea un `AttackEffect(APPLY_CONDITION, condition)`
3. `PostDamageEffectStep` construye el `ApplyConditionEffect`
4. `StatusEffectManager.applyCondition()` aplica la condición al defensor

### Desde Cartas de Entrenador:

Varios resolvers de entrenador pueden aplicar o remover condiciones:
- `ConditionRemoveResolver`: remueve condiciones
- `StatusEffectResolver` en habilidades: puede prevenir condiciones

### Desde Habilidades:

- **`SweetVeilHook`**: previene que se apliquen condiciones al Pokémon que tiene la habilidad
- **`SpikyShieldHook`**: puede aplicar condiciones al atacante como efecto de retorno

---

## Fin de Turno y Condiciones

Al final de cada turno, el `StatusEffectManager` procesa condiciones persistentes:

1. **Quemado**: `applyBurnDamage()` → 20 de daño. Lanzamiento de moneda: cara → se cura la quemadura
2. **Envenenado**: `applyPoisonDamage()` → 10 de daño (no se cura automáticamente)
3. **Dormido**: No ocurre nada (solo se verifica al atacar o al cambiar)
4. **Paralizado**: Se cura automáticamente después de un turno
5. **Confundido**: No se cura automáticamente (requiere cambio a banca o carta de curación)

---

## Interacción con Retirada y Cambio

| Acción | Efecto sobre Condiciones |
|---|---|
| **Retirada (cambio activo → banca)** | Se curan TODAS las condiciones del Pokémon que pasa a banca |
| **Cambio forzado (efecto de carta)** | Se curan TODAS las condiciones del Pokémon que pasa a banca |
| **Evolución** | Las condiciones se conservan (no se curan al evolucionar) |
| **Herramienta "Burbuja de Escape"** | No cura condiciones (solo reduce costo de retirada) |
| **Carta "Antídoto"** | Cura envenenado específicamente |
| **Carta "Despertar"** | Cura dormido específicamente |

---

## Resumen Visual del Manejo de Condiciones

```
┌─────────────────────────────────────────────────────┐
│              CONDICIONES ESPECIALES                  │
├─────────────────┬───────────────────────────────────┤
│   VOLÁTILES     │          PERSISTENTES              │
│ (mutuamente     │  (pueden coexistir)                │
│  excluyentes)   │                                   │
├─────────────────┼───────────────────────────────────┤
│ ASLEEP (Dormido)│ BURNED (Quemado: 20 daño/turno)   │
│ CONFUSED (Conf.)│ POISONED (Veneno: 10 daño/turno)  │
│ PARALYZED (Par.)│                                   │
└─────────────────┴───────────────────────────────────┘

FLUJO POR CONDICIÓN:

DORMIDO:
  [Intenta atacar] → [Moneda: cara?]
    ├── SÍ → Despierta, ataca normalmente
    └── NO → No ataca, sigue dormido

PARALIZADO:
  [Intenta atacar] → Falla ❌ → Se cura la parálisis
  [Intenta retirarse] → No puede

CONFUNDIDO:
  [Intenta atacar] → [Moneda: cara?]
    ├── SÍ → Ataca normalmente (sigue confundido)
    └── NO → 30 de daño a sí mismo, termina ataque

QUEMADO:
  [Fin de turno] → 20 de daño → [Moneda: cara?]
    ├── SÍ → Se cura la quemadura
    └── NO → Sigue quemado

ENVENENADO:
  [Fin de turno] → 10 de daño (no se cura automáticamente)
```
