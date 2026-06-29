# Sistema de Evoluciones

## Resumen

El sistema de evolución permite que los Pokémon básicos se transformen en versiones más poderosas durante la partida. Una evolución requiere que el Pokémon haya estado en juego al menos un turno (salvo excepciones como cartas de entrenador que permiten evolución inmediata). El sistema valida la cadena evolutiva, la carta utilizada y el momento de la jugada.

## Arquitectura

El sistema es relativamente simple comparado con otros subsistemas. Se compone de:

```
EvolvePokemonHandler (punto de entrada)
  │
  ▼
GameEngine ──→ valida y aplica la evolución
  │
  ▼
PokemonInPlay (modelo que representa al Pokémon en juego)
```

---

## `EvolvePokemonHandler`

**Ubicación:** `engine/handlers/EvolvePokemonHandler.java`

Maneja el evento `EVOLVE_POKEMON` enviado por un jugador para evolucionar uno de sus Pokémon en juego (activo o banca).

### Flujo de Evolución:

1. **Recepción del evento**: el jugador envía `cardId` de la carta de evolución y `targetPokemonId` del Pokémon a evolucionar
2. **Validación básica**:
   - Es el turno del jugador
   - El Pokémon objetivo está en juego (activo o banca)
   - El Pokémon objetivo es del jugador
   - No se evolucionó ya este turno (regla: 1 evolución por turno, salvo habilidades)
3. **Validación de cadena evolutiva**:
   - La carta de evolución debe coincidir con el nombre del Pokémon objetivo
   - Ej: `Charizard` evoluciona de `Charmeleon`, que evoluciona de `Charmander`
4. **Validación de turnos**:
   - El Pokémon objetivo debe haber estado en juego desde el turno anterior
   - Excepción: cartas como "Evolución Instantánea" saltan este requisito
5. **Aplicación de la evolución**:
   - Remueve la carta anterior de `PlayerState`
   - Coloca la nueva carta evolucionada en la misma posición
   - **Conserva**: energía adjunta, condiciones especiales, daño acumulado, herramientas
   - **Actualiza**: PS máximos, ataques disponibles, habilidades
6. **Limpieza**: la carta evolucionada anterior vuelve al mazo

### Métodos Clave:

| Método | Descripción |
|---|---|
| `handleEvent(GameEvent event)` | Procesa el evento de evolución |
| `validateEvolveChain(PokemonInPlay current, Card evolution)` | Verifica que la evolución sea válida |
| `applyEvolution(PlayerState player, PokemonInPlay target, Card evolutionCard)` | Aplica la evolución al estado |
| `isEvolutionLegal(PokemonInPlay target)` | Verifica reglas de tiempo (1 turno en juego) |

---

## `PokemonInPlay`

**Ubicación:** `engine/model/PokemonInPlay.java`

Representa un Pokémon que está actualmente en el campo de juego (activo o banca). Es la estructura de datos central que se modifica durante la evolución.

### Campos Clave:

| Campo | Descripción |
|---|---|
| `card` | La carta de Pokémon actual (cambia al evolucionar) |
| `attachedEnergy` | Lista de cartas de energía adjuntas (se conservan) |
| `currentHp` | PS actuales (se conserva el daño, pero cambia el máximo) |
| `maxHp` | PS máximos (se actualiza al evolucionar) |
| `specialConditions` | Condiciones especiales activas (se conservan) |
| `attachedTool` | Herramienta adjunta (se conserva si es compatible) |
| `turnsInPlay` | Contador de turnos desde que entró en juego |
| `evolvedFrom` | Referencia a la carta pre-evolución |

### Métodos Clave:

| Método | Descripción |
|---|---|
| `getAvailableAttacks()` | Devuelve los ataques de la carta actual |
| `getType()` | Tipo de Pokémon (fuego, agua, etc.) |
| `hasEnergy(Cost cost)` | Verifica si tiene energía para un costo dado |

---

## Validación de Cadena Evolutiva

El sistema valida la cadena evolutiva según los datos de las cartas:

1. `PokemonCard.getEvolvesFrom()` → indica de qué Pokémon básico evoluciona
2. La carta de evolución debe tener `evolvesFrom = nombreDelPokemonObjetivo`
3. Ejemplo de cadena válida: `Charmander → Charmeleon (evolvesFrom=Charmander) → Charizard (evolvesFrom=Charmeleon)`

---

## Reglas Especiales

### Evolución en el primer turno
- Por regla oficial, no se puede evolucionar en el primer turno (salvo que una carta lo permita)
- El sistema valida `turnsInPlay > 0` antes de permitir evolución

### Evolución múltiple en un turno
- Por defecto, solo 1 evolución por turno
- Habilidades como `Evolucionario` (Eevee) pueden permitir múltiples evoluciones
- Cartas de entrenador como `Rapid Evolution` pueden saltar la regla

### Mega Evolución
- Mecánica especial donde la evolución ocurre durante el turno
- Requiere la Mega-Gema correspondiente adjunta
- Termina el turno después de la evolución

### Evolución de Pokémon en Banca
- Se permite evolucionar Pokémon en banca (no solo el activo)
- Sigue las mismas reglas que evolución del activo
- La energía y condiciones se conservan igualmente

---

## Integración con Habilidades

El sistema de evolución se integra con `AbilityRegistry` a través de hooks:

- **`HandleEvolutionHook`** (no mencionado en hooks estándar pero implícito): algunas habilidades pueden dispararse al evolucionar
- Ejemplo: habilidades que curan al Pokémon al evolucionar
- Verificación de habilidades que pueden estar desactivadas (ej: `ForestsCurseHook` puede afectar evoluciones)

---

## Resumen Visual del Flujo de Evolución

```
[Jugador envía EVOLVE_POKEMON]
  │
  ▼
EvolvePokemonHandler.handleEvent()
  │
  ├── ¿Es turno del jugador? ─── NO ──→ Rechazar
  │
  ├── ¿Pokémon objetivo en juego? ─── NO ──→ Rechazar
  │
  ├── ¿Cadena evolutiva válida? ─── NO ──→ Rechazar
  │   │
  │   └── ¿Pokémon objetivo = evolvesFrom de la carta? ─── NO ──→ Rechazar
  │
  ├── ¿TurnsInPlay > 0? ─── NO ──→ Rechazar (excepto cartas especiales)
  │
  ├── ¿Ya evolucionó este turno? ─── SÍ ──→ Rechazar (excepto habilidades)
  │
  └── Aplicar evolución:
      ├── Remover carta anterior
      ├── Colocar nueva carta evolucionada
      ├── Conservar: energía, condiciones, daño, herramientas
      └── Actualizar: ataques, PS máximos, habilidades
```
