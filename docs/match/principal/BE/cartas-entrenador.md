# Sistema de Cartas de Entrenador

## Resumen

El sistema de cartas de entrenador implementa un **patrón Registry + Resolver** que permite manejar los 23 tipos diferentes de efectos que pueden tener las cartas de entrenador en el juego. Cada efecto se registra en un registro central (`TrainerEffectRegistry`) y se resuelve mediante una implementación específica de `TrainerEffectResolver`. El sistema maneja tanto entrenadores de objeto (Item), de partidario (Supporter), de estadio (Stadium) como herramientas (Tool).

## Arquitectura

```
TrainerEffectRegistry
│   (Map<EffectType, TrainerEffectResolver>)
│
├── Resolvers (23 implementaciones)
│   ├── DrawCardsResolver
│   ├── HealResolver
│   ├── SearchBasicPokemonResolver
│   ├── StadiumPlayResolver
│   ├── SwitchPokemonResolver
│   ├── EvolveSearchResolver
│   ├── DiscardAndDrawResolver
│   ├── SearchEnergyResolver
│   ├── ToolAttachResolver
│   ├── ConditionRemoveResolver
│   ├── EvolveDirectResolver
│   ├── ... (12 más)
│
├── PlayTrainerHandler (punto de entrada) 
└── AttachToolHandler (herramientas)
```

---

## `PlayTrainerHandler`

**Ubicación:** `engine/handlers/PlayTrainerHandler.java`

Maneja el evento `PLAY_TRAINER` cuando un jugador juega una carta de entrenador desde su mano.

### Flujo General:

1. **Recepción del evento**: el jugador envía `PLAY_TRAINER` con `cardId` y `targets` opcionales
2. **Validación**:
   - Es el turno del jugador
   - La carta está en la mano
   - Se puede jugar (no hay restricciones activas como "efecto de bloqueo de entrenadores")
3. **Determinación de tipo**:
   - `ITEM`: se juega durante el turno, consume la carta
   - `SUPPORTER`: una por turno, consume la carta
   - `STADIUM`: reemplaza el estadio activo
   - `TOOL`: se adjunta a un Pokémon
   - `TECHNICAL_MACHINE`: se juega como herramienta con un ataque
4. **Resolución de efectos**:
   - Parsea el `EffectType` de la carta
   - Busca el resolver correspondiente en `TrainerEffectRegistry`
   - Ejecuta el resolver con el contexto
5. **Post-resolución**:
   - Mueve la carta al descarte (o al campo, en caso de estadios/herramientas)
   - Aplica restricciones de "una por turno"

---

## `TrainerEffectRegistry`

**Ubicación:** `engine/trainer/TrainerEffectRegistry.java`

Registro central que mapea cada `EffectType` a su `TrainerEffectResolver` correspondiente.

### Registro:

```java
Map<EffectType, TrainerEffectResolver> resolvers = new HashMap<>();
```

Se popula en la construcción con todos los resolvers disponibles. Cada carta de entrenador define su `EffectType` en sus datos, y el registro se usa para buscar cómo resolverlo.

---

## `TrainerEffectResolver` (Interfaz Funcional)

**Ubicación:** `engine/trainer/TrainerEffectResolver.java`

Interfaz que define cómo se resuelve un efecto de entrenador.

```java
TrainerEffectResult resolve(TrainerEffectContext context);
```

Recibe un contexto con toda la información necesaria y retorna un resultado que puede incluir efectos secundarios, selecciones del jugador, o cartas afectadas.

---

## `EffectType`

**Ubicación:** `engine/trainer/EffectType.java`

Enum que cataloga los 23 tipos de efectos de entrenador:

| EffectType | Descripción | Ejemplo de Carta |
|---|---|---|
| `DRAW_CARDS` | Robar cartas del mazo | Profesor Oak (Nuevos Investigadores) |
| `HEAL` | Curar PS a un Pokémon | Pocion |
| `SEARCH_BASIC_POKEMON` | Buscar Pokémon básico en el mazo | Búsqueda de Pokémon |
| `STADIUM_PLAY` | Activar un estadio | Bosque de Virdi |
| `SWITCH_POKEMON` | Cambiar Pokémon activo por banca | Relevo |
| `EVOLVE_SEARCH` | Buscar evolución en el mazo | Búsqueda de Evoluciones |
| `DISCARD_AND_DRAW` | Descartar y robar (mano nueva) | Profesor Oak (Nuevos Investigadores) versión moderna |
| `SEARCH_ENERGY` | Buscar energía en el mazo | Búsqueda de Energía |
| `TOOL_ATTACH` | Adjuntar una herramienta a un Pokémon | Burbuja de Escape |
| `CONDITION_REMOVE` | Curar condiciones especiales | Antídoto, Despertar |
| `EVOLVE_DIRECT` | Evolucionar directamente sin esperar turno | Evolución Rápida |
| `DAMAGE` | Hacer daño directo | Martillo Salvaje |
| `PREVENT_EVOLUTION` | Prevenir evolución del oponente | Goma de Mascar |
| `SEARCH_ANY_CARD` | Buscar cualquier carta en el mazo | Búsqueda Maestra |
| `RECYCLE` | Recuperar carta del descarte | Reciclaje |
| `DISCARD_OPPONENT_ENERGY` | Descartar energía del oponente | Martillo Hechizado |
| `ADDITIONAL_ENERGY_ATTACH` | Adjuntar energía extra este turno | Energía Extra |
| `CHANGE_TYPE` | Cambiar tipo de Pokémon | Distorsión de Tipo |
| `PREVENT_DAMAGE` | Prevenir daño por un turno | Escudo Protector |
| `DRAW_AND_REARRANGE` | Robar y reordenar el mazo | Investigación de Profesor |
| `MOVE_ENERGY` | Mover energía entre Pokémon propios | Transferencia de Energía |
| `COPY_ABILITY` | Copiar habilidad de otro Pokémon | Copia de Poder |
| `RESTORE_DECK` | Reintegrar cartas del descarte al mazo | Recuperación de Mazo |

---

## Resolvers Detallados

### `DrawCardsResolver`

Hace robar `N` cartas del mazo al jugador. El parámetro `N` está definido en la carta.

- **Validación**: Mazo no vacío
- **Efecto**: Transfiere `N` cartas de `PlayerState.deck` a `PlayerState.hand`
- **Ejemplo**: Profesor Oak → robar 7 cartas

### `HealResolver`

Cura una cantidad específica de PS a un Pokémon objetivo.

- **Validación**: Pokémon objetivo existe y tiene daño
- **Efecto**: Reduce el daño en `N` PS (sin exceder el PS máximo)
- **Opcional**: Puede incluir cura de condiciones especiales

### `SearchBasicPokemonResolver`

Busca un Pokémon básico en el mazo y lo pone en la mano (o banca, según carta).

- **Validación**: Jugador tiene al menos un Pokémon básico en el mazo
- **Efecto**: El jugador selecciona un Pokémon básico del mazo → va a la mano
- **Variante**: Algunas cartas permiten ponerlo directamente en banca

### `StadiumPlayResolver`

Activa una carta de estadio en el campo. Si ya hay un estadio activo, se descarta y se reemplaza.

- **Validación**: No hay restricciones de estadio activas
- **Efecto**: `GameState.activeStadium = card`. Estadio anterior va al descarte

### `SwitchPokemonResolver`

Cambia el Pokémon activo por uno de la banca (sin pagar costo de retirada).

- **Validación**: El jugador tiene al menos un Pokémon en banca
- **Efecto**: Intercambia activo ↔ banca (equivalente a retirada forzada sin costo)

### `EvolveSearchResolver`

Busca una carta de evolución específica en el mazo para un Pokémon en juego.

- **Validación**: El Pokémon objetivo tiene una evolución válida en el mazo
- **Efecto**: Añade la evolución a la mano

### `DiscardAndDrawResolver`

El jugador descarta `X` cartas de su mano y roba `Y` cartas (o descarta toda la mano y roba 7).

- **Validación**: Jugador tiene cartas en la mano
- **Efecto**: Descarta cartas seleccionadas → roba según carta
- **Ejemplo**: `Discard 2, draw 3`

### `SearchEnergyResolver`

Busca una carta de energía específica en el mazo y la pone en la mano (o la adjunta directamente).

- **Validación**: La energía existe en el mazo
- **Efecto**: El jugador selecciona energía → mano

### `ToolAttachResolver`

Adjunta una carta de herramienta a un Pokémon.

- **Validación**: El Pokémon no tiene ya una herramienta adjunta
- **Efecto**: `PokemonInPlay.attachedTool = toolCard`

### `ConditionRemoveResolver`

Elimina condiciones especiales de un Pokémon (quemado, dormido, paralizado, confundido, envenenado, o combinaciones).

- **Validación**: El Pokémon tiene condiciones removibles
- **Efecto**: Limpia las condiciones correspondientes

### `EvolveDirectResolver`

Evoluciona un Pokémon inmediatamente, sin necesidad de esperar un turno (salta la validación de `turnsInPlay > 0`).

- **Validación**: El Pokémon puede evolucionar legalmente
- **Efecto**: Aplica evolución como `EvolvePokemonHandler` pero sin restricción de tiempo

---

## `AttachToolHandler`

**Ubicación:** `engine/handlers/AttachToolHandler.java`

Maneja específicamente el evento `ATTACH_TOOL` para cuando un jugador adjunta una carta de herramienta a un Pokémon.

### Flujo:
1. El jugador envía `ATTACH_TOOL` con `toolCardId` y `targetPokemonId`
2. Valida que el Pokémon no tenga ya una herramienta
3. Adjunta la herramienta al Pokémon (activo o banca)
4. Las herramientas pueden modificar: costo de retirada, daño recibido, ataques disponibles

---

## Integración con Habilidades

Algunos efectos de entrenador pueden ser modificados por habilidades activas:

- **`FurCoatHook`**: Puede reducir el daño de entrenadores que hacen daño directo
- **`SweetVeilHook`**: Previene condiciones especiales aplicadas por entrenadores del oponente
- **`ForestsCurseHook`**: Puede afectar cómo se aplican efectos de tipo Planta

---

## Reglas por Tipo de Entrenador

| Tipo | Límite por Turno | ¿Se descarta? | Notas |
|---|---|---|---|
| **ITEM** | Sin límite | Sí, después de usarse | Se pueden jugar múltiples objetos por turno |
| **SUPPORTER** | 1 por turno | Sí, después de usarse | No se puede jugar Partidario en primer turno (reglas estándar) |
| **STADIUM** | 1 activo a la vez | No (permanece en campo) | Se reemplaza al jugar otro estadio |
| **TOOL** | 1 por Pokémon | No (permanece adjunta) | Removida al evolucionar (si es incompatible) o por efectos |
| **TECHNICAL_MACHINE** | 1 por Pokémon | No (como herramienta) | Provee un ataque adicional |

---

## Resumen Visual del Flujo de Jugar una Carta de Entrenador

```
[Jugador: PLAY_TRAINER]
  │
  ▼
PlayTrainerHandler.handleEvent()
  │
  ├── Validar turno
  ├── Validar carta en mano
  ├── Determinar tipo (ITEM/SUPPORTER/STADIUM/TOOL)
  │
  ├── ¿Supporter? ──→ ¿Ya jugó supporter este turno? ──→ SÍ → ❌
  │
  ├── ¿Stadium? ──→ Reemplazar estadio activo actual
  │
  ├── Buscar EffectType en TrainerEffectRegistry
  │
  ├── Obtener TrainerEffectResolver correspondiente
  │
  ├── Ejecutar resolver con contexto
  │
  ├── ¿Tool? ──→ AttachToolHandler (adjuntar a Pokémon)
  │
  └── Mover carta al descarte (o mantener en campo si estadio/herramienta)
```
