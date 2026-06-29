# Sistema de Habilidades Pokémon

## Resumen

El sistema de habilidades Pokémon implementa un **patrón Registry + Resolver + Hooks** que permite que los Pokémon tengan habilidades pasivas que modifican el juego en momentos específicos. Las habilidades se registran en un registro central (`AbilityRegistry`), se resuelven mediante `AbilityResolver` (interfaz funcional), y se ejecutan en puntos específicos del flujo de juego llamados hooks.

## Arquitectura

```
AbilityRegistry
│   (Map<String, AbilityResolver>)
│
├── Resolvers (6 implementaciones)
│   ├── HealOnBenchResolver
│   ├── DamageBoostResolver
│   ├── StatusEffectResolver
│   ├── EnergyEffectResolver
│   ├── DrawCardsOnEvolutionResolver
│   └── TypeChangeResolver
│
└── Hooks (5 puntos de enganche)
    ├── FurCoatHook
    ├── SpikyShieldHook
    ├── SweetVeilHook
    ├── DestinyBurstHook
    └── ForestsCurseHook
```

---

## `AbilityRegistry`

**Ubicación:** `engine/ability/AbilityRegistry.java`

Registro central que mapea nombres de habilidades a sus resolvers. Cada habilidad tiene un nombre único (ej: "Pelaje Frondoso", "Escudo Pinchudo", "Velo Dulce") que se usa como clave.

### Métodos Clave:

| Método | Descripción |
|---|---|
| `registerAbility(String name, AbilityResolver resolver)` | Registra una habilidad |
| `getResolver(String name)` | Obtiene el resolver para una habilidad |
| `hasAbility(String name)` | Verifica si una habilidad está registrada |

El registro se popula al inicio con las habilidades implementadas. Cuando un Pokémon está en juego, se verifica qué habilidades tiene y se obtiene el resolver correspondiente.

---

## `AbilityResolver` (Interfaz Funcional)

**Ubicación:** `engine/ability/AbilityResolver.java`

Interfaz funcional que define cómo se ejecuta una habilidad.

```java
AbilityResult resolve(AbilityContext context);
```

Cada implementación recibe un `AbilityContext` con:
- El Pokémon que tiene la habilidad
- El Pokémon objetivo (si aplica)
- El estado actual del juego
- El evento que disparó la habilidad (si aplica)

---

## Resolvers Concretos (6)

### `HealOnBenchResolver`

Cura al Pokémon mientras está en la banca. Representa habilidades como "Curación Natural" que curan PS entre turnos.

- **Cuándo se activa**: Al final del turno del jugador
- **Efecto**: Recupera `N` PS al Pokémon en banca
- **No aplica**: Al Pokémon activo (solo banca)

### `DamageBoostResolver`

Aumenta el daño de los ataques del Pokémon bajo ciertas condiciones.

- **Cuándo se activa**: Durante `ModifierStep` en la cadena de ataque
- **Efecto**: +N al daño si se cumplen las condiciones (ej: si el oponente tiene cierta energía)
- **Ejemplo**: "Energía Solar" → +10 de daño por cada energía adjunta

### `StatusEffectResolver`

Aplica o previene condiciones especiales.

- **Cuándo se activa**: Cuando se aplicaría una condición especial
- **Efecto**: Puede prevenir la condición en sí mismo o en aliados
- **Ejemplo**: "Velo Dulce" → previene condiciones especiales en todos los Pokémon del jugador

### `EnergyEffectResolver`

Modifica cómo funciona la energía adjunta a este Pokémon.

- **Cuándo se activa**: Durante verificación de energía o matching
- **Efecto**: Puede tratar energía de un tipo como otro tipo
- **Ejemplo**: "Metamorfosis Energética" → energía incolora cuenta como cualquier tipo

### `DrawCardsOnEvolutionResolver`

Roba cartas adicionales cuando el Pokémon evoluciona.

- **Cuándo se activa**: Después de una evolución exitosa
- **Efecto**: El jugador roba `N` cartas del mazo

### `TypeChangeResolver`

Cambia el tipo del Pokémon (útil para matching de energía o debilidad).

- **Cuándo se activa**: Durante verificación de tipo (ataque, energía, debilidad)
- **Efecto**: El tipo efectivo del Pokémon cambia a otro
- **Ejemplo**: "Cambio de Tipo" → trata al Pokémon como tipo Planta

---

## Hooks (5 puntos de enganche)

Los hooks son interfaces que definen puntos específicos en el flujo del juego donde las habilidades pueden intervenir.

### `FurCoatHook`

**Ubicación:** `engine/ability/hooks/FurCoatHook.java`

Reduce el daño recibido por el Pokémon que tiene la habilidad.

- **Cuándo se ejecuta**: Durante `ModifierStep`, antes de aplicar daño
- **Efecto**: Reduce el daño en una cantidad fija (ej: −20)
- **Implementación típica**: Habilidad "Pelaje Frondoso" o similar
- **Influencia en ataque**: Se suma al cálculo de `DamageCalculator`, reduciendo el daño final antes de aplicar debilidad/resistencia

### `SpikyShieldHook`

**Ubicación:** `engine/ability/hooks/SpikyShieldHook.java`

Refleja daño al atacante cuando este Pokémon recibe daño.

- **Cuándo se ejecuta**: Después de recibir daño (en `PostDamageEffectStep` o como efecto reactivo)
- **Efecto**: Hace `N` de daño al Pokémon atacante
- **Influencia en ataque**: Puede resultar en que el atacante reciba daño de vuelta

### `SweetVeilHook`

**Ubicación:** `engine/ability/hooks/SweetVeilHook.java`

Previene que el Pokémon sea afectado por condiciones especiales.

- **Cuándo se ejecuta**: Durante `ConditionCheckStep` o cuando se aplicaría una condición especial
- **Efecto**: Bloquea la aplicación de condiciones especiales (dormido, paralizado, etc.)
- **Influencia en ataque**: Afecta qué condiciones puede aplicar el ataque del oponente

### `DestinyBurstHook`

**Ubicación:** `engine/ability/hooks/DestinyBurstHook.java`

Se activa cuando el Pokémon es debilitado (KO), causando un efecto adicional.

- **Cuándo se ejecuta**: Durante `KnockoutCheckStep`, cuando el Pokémon es debilitado
- **Efecto**: Puede hacer daño al Pokémon del oponente, descartar energía, o aplicar condiciones
- **Influencia en ataque**: El atacante puede recibir consecuencias incluso al hacer KO
- **Ejemplo**: Habilidad "Estallido Final" que hace 50 de daño al atacante al ser debilitado

### `ForestsCurseHook`

**Ubicación:** `engine/ability/hooks/ForestsCurseHook.java`

Agrega el tipo Planta al Pokémon (útil para debilidad/resistencia adicional).

- **Cuándo se ejecuta**: Durante verificación de tipo/debilidad
- **Efecto**: El Pokémon gana el tipo Planta además de su tipo original
- **Influencia en ataque**: Ahora es débil a ataques de tipo Planta (si no lo era antes), y puede resistir ataques que antes no resistía

---

## `UseAbilityHandler`

**Ubicación:** `engine/handlers/UseAbilityHandler.java`

Maneja el evento `USE_ABILITY` para habilidades que requieren activación voluntaria por parte del jugador (no todas las habilidades son pasivas; algunas requieren acción del jugador para activarse).

### Flujo:

1. El jugador envía `USE_ABILITY` con `abilityName` y opcionalmente `targets`
2. Se verifica que:
   - Un Pokémon del jugador tiene la habilidad
   - La habilidad puede activarse en este momento (no está en "cooldown" o bloqueada)
   - Se cumplen los requisitos de activación
3. Se obtiene el resolver de `AbilityRegistry`
4. Se ejecuta el resolver con el contexto
5. Se aplican los resultados al `GameState`

### Habilidades Pasivas vs Activas:

| Tipo | Descripción | Ejemplo |
|---|---|---|
| **Pasiva** | Siempre activa, no requiere acción del jugador | Pelaje Frondoso (reduce daño siempre) |
| **Activa** | Requiere que el jugador la active voluntariamente | Curar 20 PS de un Pokémon en banca |
| **Reactiva** | Se activa automáticamente ante un evento específico | Estallido Final (al ser debilitado) |

---

## Integración con el Sistema de Ataque

Las habilidades interactúan con la cadena de ataque en múltiples puntos:

1. **`ConditionCheckStep`**: `SweetVeilHook` puede prevenir condiciones que bloquearían el ataque
2. **`EnergyCheckStep`**: `EnergyEffectResolver` puede modificar cómo se cuentan las energías
3. **`ModifierStep`**: `FurCoatHook` reduce daño recibido, `DamageBoostResolver` aumenta daño
4. **`PostDamageEffectStep`**: `SpikyShieldHook` puede causar daño de retorno
5. **`KnockoutCheckStep`**: `DestinyBurstHook` se activa si el Pokémon es debilitado

---

## Integración con el Sistema de Entrenador

- **`FurCoatHook` reduce el daño de entrenadores que infligen daño directo
- **`SweetVeilHook` previene condiciones aplicadas por entrenadores del oponente
- **`StatusEffectResolver` puede prevenir condiciones de cartas de entrenador

---

## Resumen Visual

```
AbilityRegistry (mapeo nombre → resolver)
  │
  ├── HealOnBenchResolver ─── "Curación Natural"
  ├── DamageBoostResolver ─── "Energía Solar"
  ├── StatusEffectResolver ── "Velo Dulce" (prevenir condiciones)
  ├── EnergyEffectResolver ── "Metamorfosis Energética"
  ├── DrawCardsOnEvolution ── "Evolución Robacartas"
  └── TypeChangeResolver ──── "Cambio de Tipo"

Hooks (puntos de enganche en el flujo del juego)
  │
  ├── FurCoatHook ──────── reduce daño recibido
  ├── SpikyShieldHook ──── refleja daño al atacante
  ├── SweetVeilHook ────── previene condiciones especiales
  ├── DestinyBurstHook ─── efecto al ser debilitado
  └── ForestsCurseHook ─── agrega tipo Planta

Flujo de activación de habilidad (activa):
  [Jugador: USE_ABILITY]
       │
       ▼
  UseAbilityHandler.handleEvent()
       │
       ├── Verificar que tiene la habilidad
       ├── Verificar que puede activarse
       │
       ▼
  AbilityRegistry.getResolver(abilityName)
       │
       ▼
  AbilityResolver.resolve(context)
       │
       ▼
  [Resultado aplicado al GameState]
```
