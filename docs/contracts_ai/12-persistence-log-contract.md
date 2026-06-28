# Persistence and Log Contract

## Goal

Define what must be persisted after every relevant action.

The persisted state must be sufficient to reconstruct the full match.

## Backend location

```
repositories/entities/
  MatchEntity.java
  MatchPlayerEntity.java
  MatchStateEntity.java
  MatchLogEntity.java
repositories/jpa/
  MatchJpaRepository.java
  MatchPlayerJpaRepository.java
  MatchStateJpaRepository.java
  MatchLogJpaRepository.java
engine/ports/StatePersisterPort.java
engine/ports/impl/StatePersisterAdapter.java
```

No existe paquete `persistence/`. Todo está en `repositories/entities/` y `repositories/jpa/`.

## State persistence rule

After every valid action, persist the complete `GameState` as a JSON column via `MatchStateEntity`.

## MatchEntity

Ubicación: `repositories/entities/MatchEntity.java`

```
@Id private UUID id
@Column status: String
@Column currentPhase: String
@Column turnNumber: Integer
@Column currentPlayerId: UUID
@Column firstPlayerId: UUID
@Column winnerPlayerId: UUID
@Column finishReason: String
@Column createdAt: Instant
@Column updatedAt: Instant
@Column finishedAt: Instant
@Column latestStateVersion: Long

@OneToMany → MatchPlayerEntity (cascade ALL)
@OneToMany → MatchStateEntity (cascade ALL)
@OneToMany → MatchLogEntity (cascade ALL)
```

No tiene `@Convert(converter = GameStateConverter.class)` en una columna `state`. Tiene columnas desnormalizadas para el estado actual del match.

## MatchPlayerEntity

Tabla `match_players`:

```
@Id private UUID id
@ManyToOne matchId
@Column playerId: UUID
@Column playerKind: String (ej: "HUMAN")
@Column side: String (PLAYER_ONE / PLAYER_TWO)
@Column deckId: UUID
@Column displayName: String
@Column joinedAt: Instant
```

## MatchStateEntity

Tabla `match_states`:

```
@Id private UUID id
@ManyToOne matchId
@Column version: Long
@Column serializedState: String (JSON)
@Column createdAt: Instant
```

`StatePersisterAdapter.saveState()` crea un nuevo `MatchStateEntity` con versión incremental.
`StatePersisterAdapter.loadState()` busca el último estado por `findTopByMatchIdOrderByVersionDesc`.

## MatchLogEntity

Tabla `match_logs`:

```
@Id private UUID id
@ManyToOne matchId
@Column version: Long
@Column turnNumber: Integer
@Column playerId: UUID
@Column actionType: String
@Column eventType: String
@Column result: String (SUCCESS / ERROR)
@Column message: String
@Column payload: String (JSON, default "{}")
@Column createdAt: Instant
```

Existe `MatchLogJpaRepository` para persistir los logs.

## StatePersisterAdapter

Reside en `engine/ports/impl/StatePersisterAdapter.java`. Implementa `StatePersisterPort`:

- `saveState`: crea un nuevo `MatchStateEntity` con versión incremental y guarda via `MatchStateJpaRepository`
- `loadState`: busca el último estado via `findTopByMatchIdOrderByVersionDesc`, deserializa el JSON con `ObjectMapper`

Usa `MatchJpaRepository` y `MatchStateJpaRepository`, no `MatchRepository`.

## StatePersisterPort

```java
public interface StatePersisterPort {
    void saveState(UUID matchId, GameState state);
    Optional<GameState> loadState(UUID matchId);
}
```

## GameStateConverter

No existe. No hay `@Convert` en `MatchEntity`. La serialización/deserialización se maneja directamente en `StatePersisterAdapter` con `ObjectMapper`.

## MatchRepository

No existe. Reemplazado por `MatchJpaRepository` y `MatchStateJpaRepository`.

## Event log

- `MatchLogEntity` SÍ existe con eventos tipados
- `GameEvent` / `GameEventType` SÍ existen
- El log se persiste en la tabla `match_logs`
- Los eventos incluyen tipo (eventType), resultado, mensaje y payload JSON

## Event persistence flow

1. `GameEngine.applyAction()` ejecuta la acción
2. Los `GameEvent` generados se pasan a `EventPublisherPort.publishEvents()`
3. `MatchWebSocketPublisher` implementa el port y persiste los eventos como `MatchLogEntity`
4. También publica los eventos via WebSocket a los clientes

## Immutability rule

Cada mutación del estado de juego crea un nuevo `MatchStateEntity` con versión incremental. El estado anterior no se modifica, permitiendo reconstrucción histórica.
