# Sistema de Energías

## Resumen

El sistema de energía maneja cómo las cartas de energía se adjuntan a los Pokémon, cómo se verifica que un Pokémon tiene la energía necesaria para atacar, y cómo diferentes tipos de energía especial implementan comportamientos únicos. Utiliza el **patron Strategy** a través de `EnergyResolutionStrategy` para permitir que cada tipo de energía especial (Doble Incolora, Arcoíris, Fuerte, etc.) tenga su propia lógica de resolución.

## Arquitectura

```
EnergyService (servicio principal)
├── EnergyMatchingEngine (motor de matching)
├── EnergyStrategyRegistry (registro de estrategias)
│   ├── BasicEnergyStrategy
│   ├── DoubleColorlessEnergyStrategy
│   ├── RainbowEnergyStrategy
│   └── StrongEnergyStrategy
│
└── Modelos de datos
    ├── AttachmentContext
    ├── AttachmentOrigin
    ├── EnergyAllocation
    ├── EnergyPaymentResult
    ├── EnergySource
    ├── MatchBehavior
    ├── DamageModifier
    ├── ModifierCondition
    └── ModifierOperator
```

---

## `EnergyService`

**Ubicación:** `engine/energy/EnergyService.java`

Servicio principal que orquesta todas las operaciones relacionadas con energía. Es el punto de entrada utilizado por los handlers y el motor de juego.

### Responsabilidades:
- Adjuntar energía a un Pokémon
- Remover energía de un Pokémon
- Verificar disponibilidad de energía para ataques
- Procesar pagos de energía (descarte por costos de ataque o retirada)
- Delegar en `EnergyResolutionStrategy` para energías especiales

### Métodos Clave:

| Método | Descripción |
|---|---|
| `attachEnergy(AttachmentContext ctx)` | Adjunta energía a un Pokémon según su estrategia |
| `detachEnergy(PokemonInPlay pokemon, int count)` | Remueve `count` energías de un Pokémon |
| `canPayCost(PokemonInPlay pokemon, EnergyCost cost)` | Verifica si el Pokémon puede pagar un costo |
| `payCost(PokemonInPlay pokemon, EnergyCost cost)` | Descarta energías para pagar un costo |
| `getEnergyCount(PokemonInPlay pokemon)` | Cantidad total de energía adjunta |
| `getEnergyByType(PokemonInPlay pokemon)` | Energía desglosada por tipo |

### Flujo de `attachEnergy`:

1. Recibe un `AttachmentContext` con la energía, el Pokémon destino y el origen
2. Busca la estrategia correspondiente en `EnergyStrategyRegistry`
3. Ejecuta `EnergyResolutionStrategy.resolve(ctx)` que puede:
   - Adjuntar la energía directamente (energía básica)
   - Aplicar efectos adicionales (Arcoíris: hacer 10 de daño)
   - Verificar condiciones (Fuerte: solo a Pokémon de tipo específico)
4. Actualiza el `PokemonInPlay.attachedEnergy`

---

## `EnergyMatchingEngine`

**Ubicación:** `engine/energy/EnergyMatchingEngine.java`

Motor que determina si un Pokémon puede pagar un costo de energía dado. Implementa la lógica de matching considerando energías específicas, incoloras y cualquier.

### Conceptos Clave:

| Término | Significado |
|---|---|
| **Energía Específica** | Fuego, Agua, Planta, Rayo, etc. Solo cuenta para costos de ese tipo |
| **Energía Incolora** | Puede usarse para cualquier tipo de costo |
| **Energía Cualquiera** | `EnergyType.ANY` — cuenta para cualquier requisito |
| **Doble Incolora** | Cuenta como 2 energías incoloras |
| **Arcoíris** | Cuenta como 1 energía de cualquier tipo (pero hace daño al adjuntar) |
| **Fuerte** | Cuenta como 1 energía de tipo específico + daño extra en ataques |

### Algoritmo de Matching:

1. Separa el costo en energía específica e incolora
2. Matching greedy: primero asigna energía específica a costos específicos
3. Luego asigna energía incolora/any a costos incoloros
4. Si la energía específica sobrante puede cubrir costos incoloros, lo hace
5. Retorna `EnergyPaymentResult` indicando si es pagable y cómo

---

## `EnergyStrategyRegistry`

**Ubicación:** `engine/energy/EnergyStrategyRegistry.java`

Registro que mapea `EnergyStrategyKey` a implementaciones de `EnergyResolutionStrategy`.

### `EnergyStrategyKey`

Compuesto por `EnergyType` y `energyCardId`. Permite registrar estrategias específicas para cartas de energía concretas (ej: una carta "Doble Incolora" específica tiene su propia estrategia).

---

## `EnergyResolutionStrategy` (Interfaz)

**Ubicación:** `engine/energy/EnergyResolutionStrategy.java`

Interfaz funcional que define cómo se resuelve la adhesión de una carta de energía.

```java
EnergyResolutionResult resolve(AttachmentContext context);
```

Cada estrategia recibe el contexto de adhesión y retorna un resultado que puede incluir efectos secundarios.

---

## Estrategias Concretas

### `BasicEnergyStrategy`

**Ubicación:** `engine/energy/BasicEnergyStrategy.java`

La estrategia más simple. Simplemente adjunta la energía al Pokémon sin efectos secundarios.

- **Comportamiento**: Agrega 1 unidad de energía de tipo específico
- **Efectos secundarios**: Ninguno
- **Uso**: Todas las energías básicas (Fuego, Agua, Planta, etc.)

### `DoubleColorlessEnergyStrategy`

**Ubicación:** `engine/energy/DoubleColorlessEnergyStrategy.java`

Estrategia para la energía Doble Incolora.

- **Comportamiento**: Agrega 2 unidades de energía incolora en una sola carta
- **Efectos secundarios**: Ninguno
- **Regla especial**: Solo cuenta como 1 carta para límite de adhesión por turno, pero provee 2 energías

### `RainbowEnergyStrategy`

**Ubicación:** `engine/energy/RainbowEnergyStrategy.java`

Estrategia para la energía Arcoíris.

- **Comportamiento**: Provee 1 energía de cualquier tipo
- **Efectos secundarios**: Hace 10 de daño al Pokémon al adjuntarse
- **Uso**: Energía versátil con costo de HP

### `StrongEnergyStrategy`

**Ubicación:** `engine/energy/StrongEnergyStrategy.java`

Estrategia para la energía Fuerte (Fighting Energy mejorada).

- **Comportamiento**: Provee 1 energía de tipo específico + un modificador de daño
- **Modificador de daño**: +10 al daño de ataques del Pokémon
- **Efectos secundarios**: Aplica un `DamageModifier` al `PokemonInPlay`

---

## Modelos de Datos

### `AttachmentContext`

**Ubicación:** `engine/energy/AttachmentContext.java`

Contexto completo de una operación de adhesión de energía.

| Campo | Descripción |
|---|---|
| `energyCard` | Carta de energía a adjuntar |
| `targetPokemon` | Pokémon destino |
| `player` | Jugador que realiza la adhesión |
| `origin` | `AttachmentOrigin` (MANO, MAZO, DESCARTE, EFECTO) |

### `AttachmentOrigin`

**Ubicación:** `engine/energy/AttachmentOrigin.java`

Enum que indica de dónde viene la energía:

| Origen | Descripción |
|---|---|
| `HAND` | De la mano del jugador (acción normal de turno) |
| `DECK` | Buscada del mazo (efecto de carta) |
| `DISCARD` | Recuperada del descarte |
| `EFFECT` | Colocada por efecto de habilidad o entrenador |

### `EnergyAllocation`

**Ubicación:** `engine/energy/EnergyAllocation.java`

Representa cómo se asignan las energías específicas para pagar un costo. Usado por `EnergyMatchingEngine` para detallar qué energías se usan para qué parte del costo.

### `EnergyPaymentResult`

**Ubicación:** `engine/energy/EnergyPaymentResult.java`

Resultado de una verificación de pago: indica si el costo es pagable y qué energías se descartarían.

| Campo | Descripción |
|---|---|
| `canPay` | Booleano: si el Pokémon puede pagar |
| `allocations` | Lista de `EnergyAllocation` (cómo se asignan) |
| `excessEnergy` | Energía sobrante después del pago |

### `EnergySource`

**Ubicación:** `engine/energy/EnergySource.java`

Representa una fuente de energía individual adjunta a un Pokémon, con referencias a la carta física y su tipo.

### `MatchBehavior`

**Ubicación:** `engine/energy/MatchBehavior.java`

Define cómo un tipo de energía se comporta durante el matching:

| Comportamiento | Descripción |
|---|---|
| `SPECIFIC` | Solo cuenta para su tipo específico |
| `COLORLESS` | Cuenta para cualquier tipo |
| `SPECIFIC_OR_COLORLESS` | Cuenta como específico o incoloro (elección) |
| `DISCARD_ON_USE` | Se descarta al usarse para un ataque |

### `DamageModifier`

**Ubicación:** `engine/energy/DamageModifier.java`

Representa un modificador de daño aplicado por una energía especial (ej: Strong Energy +10).

### `ModifierCondition` y `ModifierOperator`

- **`ModifierCondition`**: Condición bajo la cual el modificador está activo (ej: "solo si el ataque es de tipo Fighting")
- **`ModifierOperator`**: Operador del modificador (`ADD`, `SUBTRACT`, `MULTIPLY`)

---

## Handlers

### `AttachEnergyHandler`

**Ubicación:** `engine/handlers/AttachEnergyHandler.java`

Maneja el evento `ATTACH_ENERGY` del jugador.

### Flujo:

1. El jugador envía `ATTACH_ENERGY` con `energyCardId` y `targetPokemonId`
2. Se verifica que:
   - Es el turno del jugador
   - El jugador no ha adjuntado energía ya este turno (1 por turno, salvo efectos)
   - La carta de energía está en la mano
   - El Pokémon destino está en juego
3. Se crea un `AttachmentContext`
4. Se delega en `EnergyService.attachEnergy(ctx)`
5. Se remueve la carta de energía de la mano del jugador

### Límites por Turno:

- Por defecto: 1 adhesión de energía por turno desde la mano
- Efectos de cartas pueden permitir adhesiones adicionales
- Habilidades pueden adjuntar energía como parte de su efecto

---

## Integración con Ataques

Cuando un ataque requiere energía:

1. `EnergyCheckStep` llama a `EnergyService.canPayCost(attacker, cost)`
2. Si es pagable, `payCost()` descarta las energías necesarias
3. Las energías con `MatchBehavior.DISCARD_ON_USE` se descartan específicamente
4. Las energías con modificadores de daño (Strong Energy) aplican sus bonos en `ModifierStep`

---

## Resumen Visual del Flujo de Adhesión de Energía

```
[Jugador: ATTACH_ENERGY]
  │
  ▼
AttachEnergyHandler.handleEvent()
  │
  ├── Validar turno
  ├── Validar límite de adhesión (1/turno)
  ├── Validar carta en mano
  └── Validar Pokémon en juego
  │
  ▼
EnergyService.attachEnergy(AttachmentContext)
  │
  ▼
EnergyStrategyRegistry.getStrategy(energyType, cardId)
  │
  ▼
EnergyResolutionStrategy.resolve(ctx)
  │
  ├── BasicEnergyStrategy → adjunta 1 energía de tipo X
  ├── DoubleColorlessEnergyStrategy → adjunta 2 incoloras
  ├── RainbowEnergyStrategy → adjunta 1 cualquiera + 10 de daño
  └── StrongEnergyStrategy → adjunta 1 tipo X + DamageModifier
  │
  ▼
PokemonInPlay.attachedEnergy += nueva energía
  │
  ▼
[Éxito]
```
