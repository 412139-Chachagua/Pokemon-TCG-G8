# Configuración Inicial de la Partida

## Resumen

La fase de setup-inicial es el primer paso de cualquier partida de Pokémon TCG. Antes de que los turnos comiencen, ambos jugadores deben preparar sus mazos, colocar sus Pokémon iniciales y resolver cualquier mano inicial que no contenga Pokémon básicos (mulligan). Este proceso está orquestado por `SetupManager` y manejado a través de eventos enviados por los jugadores.

## Arquitectura

El sistema de setup sigue un patrón de **manejo de eventos (handler pattern)** donde cada acción del jugador durante la configuración es un evento que el `GameEngine` delega a un handler específico.

```
SetupManager (orquestador)
├── ConfirmSetupHandler
├── SetupPlaceActiveHandler
├── SetupPlaceBenchHandler
├── SetupRemoveActiveHandler
├── SetupRemoveBenchHandler
├── ResolveInitialMulliganHandler
└── ResolveMulliganDrawHandler
```

### Flujo General

1. Ambos jugadores confirman su mano inicial (`ConfirmSetupHandler`)
2. Cada jugador coloca un Pokémon activo (`SetupPlaceActiveHandler`)
3. Cada jugador coloca Pokémon en banca (`SetupPlaceBenchHandler`, `SetupRemoveActiveHandler`, `SetupRemoveBenchHandler`)
4. Se resuelven los mulligans — jugadores sin Pokémon básicos roban hasta tener uno (`ResolveInitialMulliganHandler`, `ResolveMulliganDrawHandler`)
5. Se determina quién empieza

---

## `SetupManager`

**Ubicación:** `engine/setup/SetupManager.java`

Clase principal que orquesta toda la fase de setup. Coordina la transición del estado de la partida desde `WAITING_SETUP` hasta los turnos normales.

### Responsabilidades:
- Validar que ambos jugadores hayan completado su setup
- Manejar el flujo de mulligan
- Determinar el orden de turno
- Construir el `GameState` inicial y el `EngineContext`

### Métodos Clave:

| Método | Descripción |
|---|---|
| `handleEvent(GameEvent event)` | Punto de entrada: recibe un evento de setup y lo deriva al handler correspondiente |
| `isSetupComplete()` | Verifica que ambos jugadores ya colocaron sus Pokémon activos y de banca |
| `determineFirstPlayer()` | Decide aleatoriamente quién juega primero (o según reglas de torneo) |
| `startGame()` | Transiciona la partida al estado `PLAYER_TURN` |

### Estados de Setup:

El setup se mueve a través de varios estados dentro de `GameState.setupPhase`:
- `WAITING_CONFIRMATION` — esperando que los jugadores confirmen mano
- `PLACING_ACTIVE` — colocación de Pokémon activo
- `PLACING_BENCH` — colocación de Pokémon en banca
- `RESOLVING_MULLIGAN` — resolución de manos sin Pokémon básicos
- `READY_TO_START` — setup completo, listo para comenzar

---

## `ConfirmSetupHandler`

**Ubicación:** `engine/handlers/ConfirmSetupHandler.java`

Maneja el evento `CONFIRM_SETUP` donde un jugador indica que está listo para comenzar con su mano inicial.

### Flujo:
1. Recibe el evento con el `playerId`
2. Marca al jugador como "listo" (`PlayerState.setupConfirmed = true`)
3. Si ambos jugadores confirmaron, avanza a la fase de `PLACING_ACTIVE`
4. Si algún jugador no tiene Pokémon básicos en su mano, se dispara el flujo de mulligan

---

## `SetupPlaceActiveHandler`

**Ubicación:** `engine/handlers/SetupPlaceActiveHandler.java`

Maneja el evento `PLACE_ACTIVE` donde un jugador elige qué Pokémon básico de su mano será su activo.

### Flujo:
1. Valida que la carta seleccionada sea un Pokémon básico
2. La remueve de la mano del jugador
3. La coloca en `PlayerState.activePokemon`
4. Avanza al estado `PLACING_BENCH`

---

## `SetupPlaceBenchHandler`

**Ubicación:** `engine/handlers/SetupPlaceBenchHandler.java`

Maneja el evento `PLACE_BENCH` donde un jugador coloca Pokémon adicionales en la banca.

### Flujo:
1. Recibe una lista de `cardIds` a colocar en banca
2. Valida que cada carta sea un Pokémon básico
3. Valida que no exceda el límite de 5 bancas
4. Remueve las cartas de la mano y las agrega a `PlayerState.benchPokemon`

---

## `SetupRemoveActiveHandler` y `SetupRemoveBenchHandler`

**Ubicación:** `engine/handlers/SetupRemoveActiveHandler.java`, `engine/handlers/SetupRemoveBenchHandler.java`

Manejan la remoción de Pokémon previamente colocados durante el setup. Esto permite al jugador reorganizar su formación inicial antes de confirmar.

---

## Flujo de Mulligan

### `ResolveInitialMulliganHandler`

**Ubicación:** `engine/handlers/ResolveInitialMulliganHandler.java`

Cuando un jugador no tiene Pokémon básicos en su mano inicial:

1. El jugador revela su mano al oponente
2. La mano se descarta
3. Se roba una nueva mano de 7 cartas
4. Si la nueva mano tampoco tiene Pokémon básicos, se repite el proceso
5. Por cada mulligan, el oponente puede robar una carta extra (`ResolveMulliganDrawHandler`)

### `ResolveMulliganDrawHandler`

**Ubicación:** `engine/handlers/ResolveMulliganDrawHandler.java`

Permite al jugador que **no** hizo mulligan robar una carta por cada mulligan del oponente. Esto ocurre antes de que comience el primer turno.

---

## `GameEngine` y el Setup

**Ubicación:** `engine/GameEngine.java`

El `GameEngine` es el núcleo de la partida. Durante el setup:

1. Inicializa el `EngineContext` con la configuración de la partida
2. Reparte 7 cartas a cada jugador
3. Delega los eventos de setup al `SetupManager`
4. Una vez que `SetupManager` determina que el setup está completo y determina quién empieza, el engine transiciona al loop de turnos

---

## `EngineContext`

**Ubicación:** `engine/EngineContext.java`

Contenedor de todas las dependencias y servicios del motor de juego. Se construye durante el setup y contiene:

- Referencia al `GameState`
- Servicios de energía, ataque, habilidades, etc.
- Registro de efectos de entrenador
- Registro de estrategias de energía
- Validador de reglas

---

## `GameState` y `PlayerState`

**Ubicación:** `engine/model/GameState.java`, `engine/model/PlayerState.java`

- **`GameState`**: Estado global de la partida (fase actual, turno, jugadores, condiciones de victoria)
- **`PlayerState`**: Estado individual de cada jugador (mano, mazo, activo, banca, premios, energía, condiciones especiales)

Durante el setup, `GameState.setupPhase` rastrea en qué paso del setup nos encontramos.

---

## Resumen Visual del Flujo de Setup

```
INICIO
  │
  ▼
[Repartir 7 cartas]
  │
  ▼
[Confirmar mano] ──¿Tiene Pokémon básico?──→ NO ──→ [Mulligan: descartar y robar 7 nuevas]
  │                                                  │
  │                                                  ▼
  │                                             [Oponente roba 1 carta extra]
  │                                                  │
  SÍ                                                SÍ
  │                                                  │
  ▼                                                  │
[Colocar Pokémon Activo] ◄───────────────────────────┘
  │
  ▼
[Colocar Pokémon en Banca] (0-5 Pokémon)
  │
  ▼
[Determinar quién empieza] (aleatorio)
  │
  ▼
[¡Comienza la partida!] ──→ Estado PLAYER_TURN
```
