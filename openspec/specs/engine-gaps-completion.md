# Engine gaps — TurnManager, handlers y validación

Cubre las tareas de implementación pendientes identificadas en los task files de Engine que no son tests.

---

## TurnManager (`engine/turn/TurnManager.java`)

### advancePhase(GameState)

Agregar método que avanza la fase secuencialmente: `DRAW → MAIN → ATTACK → BETWEEN_TURNS`. No existe actualmente.

```
public void advancePhase(GameState state)
```

- SHALL avanzar `state.phase` al siguiente valor en el ciclo DRAW→MAIN→ATTACK→BETWEEN_TURNS
- SHALL no hacer nada si la fase actual es BETWEEN_TURNS (el cambio de turno lo maneja `endTurn`)
- SHALL ser invocado por los handlers que necesiten avanzar fase (ej: tras atacar)

### Reset evolvedThisTurn en startTurn

En `startTurn(GameState)`, SHALL resetear `evolvedThisTurn = false` en todos los PokemonInPlay del active y bench del jugador activo.

### Auto-resolve DRAW phase

Al entrar a la fase DRAW al iniciar un turno, TurnManager SHALL auto-resolver el robo de carta (ejecutar una acción DRAW_CARD automática) sin requerir acción del jugador.

- Si el deck del jugador activo no está vacío, SHALL robar 1 carta del deck a la mano
- Si el deck está vacío, SHALL generar evento de deck-out y verificar victoria
- SHALL marcar `hasDrawnForTurn = true`

---

## Handlers — Casos faltantes

### Auto-end-turn tras ataque (DeclareAttackHandler)

Al finalizar un ataque exitosamente (sin importar si hubo KO o no), `DeclareAttackHandler` SHALL:

- Llamar a `TurnManager.advancePhase(state)` para pasar a BETWEEN_TURNS
- No requerir que el jugador envíe una acción END_TURN explícita tras atacar

### Limpiar specialConditions al retirar (RetreatActiveHandler)

Al ejecutar un retreat, SHALL limpiar `specialConditions` del Pokémon que se retira del Active al Bench.

### Limpiar specialConditions al evolucionar (EvolvePokemonHandler)

Al evolucionar un Pokémon, SHALL limpiar `specialConditions` del Pokémon evolucionado.

---

## Jakarta validation en DTOs

Agregar validación Jakarta (`jakarta.validation.constraints`) en los siguientes DTOs de entrada:

### CreateMatchRequest

| Campo | Validación |
|-------|-----------|
| `player1Name` | `@NotBlank` |
| `player1Id` | `@NotNull` |
| `player1DeckId` | `@NotNull` |

### JoinMatchRequest

| Campo | Validación |
|-------|-----------|
| `playerId` | `@NotNull` |
| `playerName` | `@NotBlank` |
| `deckId` | `@NotNull` |

### GameActionRequest

| Campo | Validación |
|-------|-----------|
| `type` | `@NotBlank` |
| `playerId` | `@NotNull` |

SHALL activar `@Valid` en los controllers correspondientes (`MatchController`, `GameActionController`) y verificar que `GlobalExceptionHandler` captura `MethodArgumentNotValidException` retornando 400 con mensaje descriptivo.

---

## Archivos afectados

- `BE/src/main/java/.../engine/turn/TurnManager.java`
- `BE/src/main/java/.../engine/handlers/DeclareAttackHandler.java`
- `BE/src/main/java/.../engine/handlers/RetreatActiveHandler.java`
- `BE/src/main/java/.../engine/handlers/EvolvePokemonHandler.java`
- `BE/src/main/java/.../dtos/matches/CreateMatchRequest.java`
- `BE/src/main/java/.../dtos/matches/JoinMatchRequest.java`
- `BE/src/main/java/.../dtos/matches/GameActionRequest.java`
- `BE/src/main/java/.../controllers/matches/MatchController.java`
- `BE/src/main/java/.../controllers/matches/GameActionController.java`
- `BE/src/main/java/.../advice/GlobalExceptionHandler.java`
