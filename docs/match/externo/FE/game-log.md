# Game Log (Registro de Eventos)

## Ubicación
- Componente: `FE/src/app/features/match/components/game-log/game-log.component.ts`
- Formateador: `FE/src/app/features/match/utils/game-event-formatter.ts`

## Propósito
Muestra un registro cronológico de todos los eventos ocurridos durante la partida en un panel deslizable desde el borde derecho. Traduce los eventos crudos del backend (`GameEventDto`) a mensajes user-friendly en español con código de colores por categoría.

## Arquitectura

### Entrada de datos
El componente recibe como input:
- `events: GameEventDto[]` — arreglo de eventos del backend.
- `myPlayerId: string | null` — para resolver conjugaciones en primera/tercera persona.

### Procesamiento (`displayEntries`)
Es un `computed` que transforma los `GameEventDto` en `DisplayEntry[]`:
1. Inserta **separadores de turno** (`turn-separator`) cuando `turnNumber` cambia entre eventos consecutivos.
2. Convierte cada evento en una entrada de tipo `event` usando `formatGameEvent()`.

### Formateador (`game-event-formatter.ts`)

**Función principal**: `formatGameEvent(event, myPlayerId) → LogEntry`

Resuelve:
- **`message`**: texto en español construido desde el payload estructurado.
- **`cssClass`**: categoría visual que determina el color del borde izquierdo y el fondo.
- **`turnNumber`**: para agrupar por turno.

**Categorías y colores**:

| Clase CSS | Color borde | Eventos típicos |
|---|---|---|
| `hostile` | Rojo | DAMAGE, KO, CONFUSION_SELF_HIT, BENCH_DAMAGE, OPPONENT_DISCARD_HAND |
| `reward` | Ámbar | PRIZE, VICTORY |
| `heal` | Esmeralda | HEAL, STATUS_REMOVED |
| `energy` | Azul | ENERGY_ATTACHED, ENERGY_DISCARDED |
| `attack` | Naranja | ATTACK_DECLARED, RECOIL_OCCURRED |
| `evo` | Violeta | POKEMON_EVOLVED, TOOL_ATTACHED |
| `status` | Amarillo | STATUS_APPLIED |
| `phase` | Cian | PHASE_CHANGED |
| `mulligan` | Ámbar | MULLIGAN_REVEALED, INITIAL_MULLIGAN_RESOLVED |
| `setup` | Índigo | SETUP_ACTIVE_PLACED, SETUP_CONFIRMED |
| `info` | Pizarra | Otros eventos no categorizados |

**Conjugación**: Usa el helper `buildAction(playerId, myPlayerId, selfMsg, otherMsg)` para elegir entre conjugación en **vos** ("Colocaste", "Robaste", "Usaste") para el jugador local, o **tercera persona** ("El oponente colocó", "El oponente robó") para el oponente.

**Tipos de eventos traducidos** (~45+ tipos):
- Setup: colocación de activo/banca, confirmación.
- Coin flip: Cara/Cruz, quién comienza.
- Turnos: cambio de fase, número de turno.
- Energía: unión, descarte (propia o del defensor).
- Evoluciones: nombre del Pokémon y desde cuál evolucionó.
- Herramientas: unión de herramienta.
- Ataques: nombre del ataque usado.
- Daño: cálculo con breakdown (debilidad ×2, resistencia -20, confusión).
- Knockout y premios: quién debilitó, reemplazo, cartas de premio tomadas.
- Retirada, entrenadores, curaciones, habilidades, búsqueda en mazo, estadio, muerte súbita, victoria.
- Mulligan: revelación, decisión, robo extra.

### Interfaz de usuario
- Botón flotante en el borde derecho (`fixed right-0 top-1/2`) con orientación vertical y contador de eventos.
- Drawer que se desliza desde la derecha (`w-80`), con transición CSS `translate-x`.
- Cada entrada de evento tiene **borde izquierdo de color** según su categoría y un fondo tenue del mismo color.
- Separadores de turno con líneas horizontales y texto "Turno N".
- Scroll automático al final cuando llegan nuevos eventos.
- Se cierra con Escape.

## Dependencias
- `GameEventDto` — modelo del backend (`game-action.models.ts`).
- `formatGameEvent`, `LogEntry` — desde `game-event-formatter.ts`.
