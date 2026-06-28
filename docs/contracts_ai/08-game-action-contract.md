# Game Action Contract

## Goal

Define the canonical action input/output format.

All game mutations must enter through:

```
GameEngine.applyAction(matchId, playerId, action)
```

## Backend location

```
engine/action/
  GameAction.java
  GameActionType.java
  ActionResult.java
  GameError.java
engine/handlers/
  GameHandler.java (interface)
  AttachEnergyHandler.java
  DeclareAttackHandler.java
  EndTurnHandler.java
  EvolvePokemonHandler.java
  HandlerHelper.java
  PlayTrainerHandler.java
  PutBasicOnBenchHandler.java
  RetreatActiveHandler.java
  (handlers internos, no expuestos al frontend:)
  DrawCardHandler.java              (draw automático, invocado por TurnManager)
  ChooseNewActiveAfterKnockoutHandler.java  (parte del pipeline de ataque)
  TakePrizeCardHandler.java          (consecuencia automática del KO)
controllers/matches/GameActionController.java
dtos/matches/GameActionResponse.java
```

## Frontend location

```
shared/models/game-action.models.ts
features/match/services/game-action-dispatcher.service.ts
```

## GameAction (engine model)

```java
public class GameAction {
    private GameActionType type;
    private UUID playerId;
    private Map<String, Object> payload;
    private String clientRequestId;
}
```

No existen clases tipadas separadas para payloads (AttachEnergyPayload, DeclareAttackPayload, etc. no existen en el código real). El payload es `Map<String, Object>` genérico.

## GameActionRequest (REST DTO)

```json
{
  "type": "ATTACH_ENERGY",
  "playerId": "player-1",
  "payload": {
    "handIndex": 2,
    "targetPokemonInstanceId": "card-instance-100"
  },
  "clientRequestId": "client-req-001"
}
```

Payload fields reference cards by `handIndex` (position in hand) rather than `cardInstanceId`, since the client knows its hand order but not the server's internal instance IDs for unrevealed cards.

## GameActionResponse (REST DTO)

```java
public record GameActionResponse(
    boolean success,
    String clientRequestId,
    Object publicState,
    Object privateState,
    List<GameEventDto> events,
    ErrorDto error
) {
    public record GameEventDto(String type, String message, Object payload) {}
    public record ErrorDto(String code, String message, Object details) {}
}
```

### Success response

```json
{
  "success": true,
  "clientRequestId": "client-req-001",
  "publicState": { ... },
  "privateState": { ... },
  "events": [
    {
      "type": "ENERGY_ATTACHED",
      "message": "Santi attached Fire Energy to Slugma.",
      "payload": { "playerId": "player-1", "targetPokemonInstanceId": "ci-30" }
    }
  ],
  "error": null
}
```

### Error response

```json
{
  "success": false,
  "clientRequestId": "client-req-001",
  "publicState": null,
  "privateState": null,
  "events": [],
  "error": {
    "code": "ENERGY_ALREADY_ATTACHED",
    "message": "No puedes unir más de 1 Energía por turno.",
    "details": {
      "phase": "MAIN",
      "hasAttachedEnergy": true
    }
  }
}
```

## ActionResult (engine internal)

```java
public class ActionResult {
    private boolean success;
    private String clientRequestId;
    private Object publicState;
    private Object privateState;
    private List<GameEvent> events;    // GameEvent tipados, no strings
    private GameError error;
}
```

## GameHandler (engine handler interface)

```java
public interface GameHandler {
    void handle(EngineContext ctx, GameAction action);
}
```

Handlers mutan `GameState` directamente via `EngineContext`. Retornan `void`.

## Action: PUT_BASIC_ON_BENCH

```json
{
  "type": "PUT_BASIC_ON_BENCH",
  "playerId": "player-1",
  "payload": { "handIndex": 0 },
  "clientRequestId": "client-req-002"
}
```

## Action: ATTACH_ENERGY

```json
{
  "type": "ATTACH_ENERGY",
  "playerId": "player-1",
  "payload": {
    "handIndex": 2,
    "targetPokemonInstanceId": "card-instance-100"
  },
  "clientRequestId": "client-req-003"
}
```

## Action: DECLARE_ATTACK

```json
{
  "type": "DECLARE_ATTACK",
  "playerId": "player-1",
  "payload": {
    "attackIndex": 0,
    "targetPokemonInstanceId": "card-instance-300"
  },
  "clientRequestId": "client-req-004"
}
```

The attacker is implicitly the Active Pokémon of the requesting player.

## Action: RETREAT_ACTIVE

```json
{
  "type": "RETREAT_ACTIVE",
  "playerId": "player-1",
  "payload": { "benchIndex": 0 },
  "clientRequestId": "client-req-005"
}
```

- `benchIndex`: position on bench (0-4) for the Pokémon to become Active.
- Energy discard for retreat cost is automatic: the backend discards the first N attached Energies that satisfy the cost from the retreating Active Pokémon. The frontend only specifies the target bench slot.

## Action: EVOLVE_POKEMON

```json
{
  "type": "EVOLVE_POKEMON",
  "playerId": "player-1",
  "payload": {
    "handIndex": 2,
    "targetPokemonInstanceId": "card-instance-100"
  },
  "clientRequestId": "client-req-006"
}
```

## Action: PLAY_TRAINER

```json
{
  "type": "PLAY_TRAINER",
  "playerId": "player-1",
  "payload": { "handIndex": 4 },
  "clientRequestId": "client-req-007"
}
```

## Action: END_TURN

```json
{
  "type": "END_TURN",
  "playerId": "player-1",
  "payload": {},
  "clientRequestId": "client-req-008"
}
```

## Acciones internas del engine (no expuestas al frontend)

Las siguientes acciones existen en el engine como handlers internos. No son acciones que el jugador envíe explícitamente — son ejecutadas automáticamente por `TurnManager` o por el pipeline de ataque:

- **DRAW_CARD**: ejecutado por `TurnManager` al entrar a DRAW phase. Automático y obligatorio. El primer jugador no roba en su primer turno. Si el mazo está vacío → derrota por DECK_OUT.
- **CHOOSE_KNOCKOUT_REPLACEMENT**: ejecutado como parte del pipeline de ataque (paso posterior al KO). El dueño del Pokémon KO'd elige un reemplazo de su Banca.
- **TAKE_PRIZE_CARD**: ejecutado automáticamente tras un KO. El jugador toma 1 Prize (o 2 si el KO'd es Pokémon-EX). Si no quedan más Prizes → victoria.

## Herramienta Pokémon (Pokémon Tool)

Las cartas de Herramienta Pokémon son un subtipo de Entrenador (Objeto). Se juegan via `PLAY_TRAINER` con payload que incluye `targetPokemonInstanceId`:

```json
{
  "type": "PLAY_TRAINER",
  "playerId": "player-1",
  "payload": {
    "handIndex": 4,
    "targetPokemonInstanceId": "ci-30"
  },
  "clientRequestId": "client-req-013"
}
```

Reglas:
- Solo se puede equipar **1 Herramienta por Pokémon** (verificar `toolCardInstanceId == null` antes de asignar)
- Se juega durante MAIN phase
- Cuenta como Objeto (no como Partidario ni Estadio)
- Permanece unida al Pokémon hasta que este es descartado (KO, etc.)
- Si el Pokémon va a Banca o evoluciona, la Herramienta **permanece**

## Habilidades de Pokémon (Implementado)

Las Habilidades (Abilities) son efectos especiales que algunos Pokémon poseen. No son ataques. Se pueden usar durante MAIN phase, tanto del Pokémon Activo como de la Banca, y no consumen el turno ni el ataque.

```json
{
  "type": "USE_ABILITY",
  "playerId": "player-1",
  "payload": {
    "pokemonInstanceId": "ci-30",
    "abilityName": "Water Shuriken"
  },
  "clientRequestId": "client-req-012"
}
```

Reglas:
- Se pueden usar todas las Habilidades que se deseen por turno, salvo que el texto de la habilidad indique lo contrario.
- Las Habilidades no son ataques — el Pokémon puede atacar en el mismo turno en que usó una Habilidad.
- El Pokémon debe estar en juego (Activo o Banca).
- La Habilidad debe existir en la definición del Pokémon.
- La Habilidad no puede haber sido usada ya este turno.
- Si el Pokémon está ASLEEP o PARALYZED, no puede usar Habilidades.

## Action rules

- playerId must match the authenticated/guest session.
- Only the current player can act.
- Every valid action must:
  - validate rules
  - mutate state
  - persist state
  - publish WebSocket events
- Handlers return `void`; they mutate `GameState` directly via `EngineContext`.
