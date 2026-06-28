# REST API Contract

## Goal

Define REST endpoints and JSON formats.

## Backend location

```
controllers/matches/
  MatchController.java
  GameActionController.java
controllers/cards/
  CardController.java
controllers/decks/
  DeckController.java
controllers/users/
  UserController.java
controllers/players/
  PlayerController.java
controllers/ranking/
  RankingController.java
```

## Frontend location

```
core/api/
```

## General error format

```json
{
  "timestamp": "2026-05-06T15:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INSUFFICIENT_ENERGY",
  "message": "No puedes atacar: te falta 1 Energía para usar este ataque.",
  "path": "/api/matches/9a747f90-b50e-49df-9d8a-456c9796aa11/actions",
  "details": {
    "required": ["FIRE", "COLORLESS"],
    "attached": ["FIRE"]
  }
}
```

`GlobalExceptionHandler` retorna `ErrorApi` con los campos: `timestamp`, `status`, `error`, `code`, `message`, `path`, `details`.

## MVP endpoints

### Users
- `POST /api/users/register`
- `POST /api/users/login`
- `GET /api/users`
- `GET /api/users/{id}`

### Players
- `GET /api/players`
- `GET /api/players/{id}`
- `PUT /api/players/{id}`

### Matches
- `POST /api/matches` (soporta 1 o 2 jugadores directo)
- `POST /api/matches/{id}/join`
- `GET /api/matches/{id}/state?playerId={playerId}` (query param obligatorio)
- `POST /api/matches/{id}/actions`
- `GET /api/cards` (con filtros: query, supertype, setCode; paginación: page, size)
- `GET /api/cards/{id}`
- `POST /api/cards/sync`
- `POST /api/decks`
- `GET /api/decks/{id}`
- `PUT /api/decks/{id}`
- `DELETE /api/decks/{id}`
- `GET /api/decks?playerId={id}`
- `POST /api/decks/{id}/validate`
- `POST /api/decks/validate`
- `GET /api/ranking`
- `GET /api/players/{id}/stats`

## POST /api/users/register

Request:
```json
{
  "email": "santi@example.com",
  "password": "secret123",
  "displayName": "Santi"
}
```

Response (201 Created):
```json
{
  "id": "user-uuid",
  "email": "santi@example.com",
  "displayName": "Santi",
  "playerId": "player-uuid"
}
```

Crea un User + Player automáticamente.

## POST /api/users/login

Request:
```json
{
  "email": "santi@example.com",
  "password": "secret123"
}
```

Response (200):
```json
{
  "id": "user-uuid",
  "email": "santi@example.com",
  "displayName": "Santi",
  "playerId": "player-uuid"
}
```

Error (401):
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid credentials"
}
```

## GET /api/users

Response:
```json
{
  "items": [
    {
      "id": "user-uuid",
      "email": "santi@example.com",
      "displayName": "Santi",
      "playerId": "player-uuid"
    }
  ]
}
```

## GET /api/users/{id}

Response:
```json
{
  "id": "user-uuid",
  "email": "santi@example.com",
  "displayName": "Santi",
  "playerId": "player-uuid"
}
```

## GET /api/players

Response:
```json
{
  "items": [
    {
      "id": "player-uuid",
      "displayName": "Santi",
      "userId": "user-uuid",
      "createdAt": "2026-05-06T15:30:00Z"
    }
  ]
}
```

## GET /api/players/{id}

Response:
```json
{
  "id": "player-uuid",
  "displayName": "Santi",
  "userId": "user-uuid",
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## PUT /api/players/{id}

Request:
```json
{
  "displayName": "Santi New"
}
```

Response:
```json
{
  "id": "player-uuid",
  "displayName": "Santi New",
  "userId": "user-uuid",
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## POST /api/matches

Soporta dos modos:

1 jugador (espera join):
```json
{
  "player1Id": "player-uuid",
  "player1Name": "Santi",
  "player1DeckId": "seed-fire-deck"
}
```

2 jugadores directo (ACTIVE inmediato):
```json
{
  "player1Id": "player-uuid",
  "player1Name": "Santi",
  "player1DeckId": "seed-fire-deck",
  "player2Id": "player-2-uuid",
  "player2Name": "Lucas",
  "player2DeckId": "seed-water-deck"
}
```

El campo también acepta los alias `playerName` y `deckId` para compatibilidad con el formato anterior.

Response (1 jugador - WAITING):
```json
{
  "id": "match-uuid",
  "status": "WAITING",
  "currentPhase": null,
  "turnNumber": 0,
  "currentPlayerId": null,
  "firstPlayerId": null,
  "winnerPlayerId": null,
  "finishReason": null,
  "players": [
    { "playerId": "player-uuid", "side": "PLAYER_ONE", "displayName": "Santi" }
  ],
  "createdAt": "2026-05-06T15:30:00Z"
}
```

Response (2 jugadores - ACTIVE):
```json
{
  "id": "match-uuid",
  "status": "ACTIVE",
  "currentPhase": "DRAW",
  "turnNumber": 1,
  "currentPlayerId": "player-1-uuid",
  "firstPlayerId": "player-1-uuid",
  "winnerPlayerId": null,
  "finishReason": null,
  "players": [
    { "playerId": "player-1-uuid", "side": "PLAYER_ONE", "displayName": "Santi" },
    { "playerId": "player-2-uuid", "side": "PLAYER_TWO", "displayName": "Lucas" }
  ],
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## POST /api/matches/{id}/join

Request:
```json
{
  "playerId": "player-2-uuid",
  "playerName": "Lucas",
  "deckId": "seed-water-deck"
}
```

Response:
```json
{
  "id": "match-uuid",
  "status": "ACTIVE",
  "currentPhase": "DRAW",
  "turnNumber": 1,
  "currentPlayerId": "player-1-uuid",
  "firstPlayerId": "player-1-uuid",
  "winnerPlayerId": null,
  "finishReason": null,
  "players": [
    { "playerId": "player-1-uuid", "side": "PLAYER_ONE", "displayName": "Santi" },
    { "playerId": "player-2-uuid", "side": "PLAYER_TWO", "displayName": "Lucas" }
  ],
  "createdAt": "2026-05-06T15:30:00Z"
}
```

## GET /api/matches/{id}/state

Query param: `playerId` (obligatorio)

Response: `MatchStateResponse` con `matchId`, `publicState` (PublicGameState) y `privateState` (PrivatePlayerState).

```json
{
  "matchId": "match-uuid",
  "publicState": {
    "matchId": "match-uuid",
    "status": "ACTIVE",
    "phase": "MAIN",
    "turnNumber": 3,
    "currentPlayerId": "player-1-uuid",
    "firstPlayerId": "player-1-uuid",
    "players": [
      {
        "playerId": "player-1-uuid",
        "side": "PLAYER_ONE",
        "activePokemon": { "instanceId": "ci-30", "cardId": "xy1-10", "damageCounters": 2, "specialConditions": [], "attachedCards": ["energy-fire-basic"] },
        "bench": [],
        "prizes": ["FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN", "FACE_DOWN"]
      }
    ]
  },
  "privateState": {
    "playerId": "player-1-uuid",
    "hand": [ { "instanceId": "ci-10", "cardId": "xy1-10", "name": "Slugma", "supertype": "POKEMON" } ],
    "deckCount": 35,
    "discardCount": 0,
    "prizes": [ { "slot": 0, "known": false, "cardId": null } ]
  }
}
```

## POST /api/matches/{id}/actions

Request:
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

Response: `GameActionResponse` con `events` como `List<GameEventDto>` (no String[]).

```json
{
  "success": true,
  "clientRequestId": "client-req-003",
  "publicState": {},
  "privateState": {},
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

## GET /api/cards

Request query example:
```
/api/cards?query=slugma&setCode=xy1&supertype=POKEMON&page=0&size=20
```

Response:
```json
{
  "items": [
    {
      "id": "xy1-10",
      "name": "Slugma",
      "supertype": "POKEMON",
      "setCode": "xy1",
      "number": "10",
      "imageSmallUrl": "https://example/slugma.png"
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

## GET /api/cards/{id}

Response: `CardDetailResponse` con información completa de la carta. Para Pokémon, incluye la lista de habilidades:

```json
{
  "id": "xy1-85",
  "name": "Aegislash",
  "supertype": "POKEMON",
  "subtypes": ["Stage 2"],
  "setCode": "xy1",
  "number": "85",
  "imageSmallUrl": "https://images.pokemontcg.io/xy1/85.png",
  "imageLargeUrl": "https://images.pokemontcg.io/xy1/85_hires.png",
  "rulesText": [],
  "hp": 140,
  "stage": "STAGE_2",
  "evolvesFrom": "Doublade",
  "types": ["METAL"],
  "attacks": [...],
  "weaknesses": [...],
  "resistances": [...],
  "retreatCost": ["COLORLESS", "COLORLESS", "COLORLESS"],
  "isEx": false,
  "isMega": false,
  "abilities": [
    {
      "name": "Stance Change",
      "text": "Once during your turn, if this Pokémon is a Stance Change Pokémon, you may switch this Pokémon with an Aegislash from your hand.",
      "type": "POKEMON_POWER"
    }
  ]
}
```

## POST /api/cards/sync

No request body.

Response:
```json
{
  "success": true,
  "message": "Sync completed.",
  "newCards": 147,
  "updatedCards": 0
}
```

## POST /api/decks

Request:
```json
{
  "name": "My Fire Deck",
  "playerId": "player-1",
  "cards": [
    { "cardId": "xy1-10", "quantity": 4 },
    { "cardId": "energy-fire-basic", "quantity": 18 }
  ]
}
```

Response: `DeckResponse` completo con id, name, ownerPlayerId, source, totalCards, valid, cards, validation.

## GET /api/decks/{id}

Response: `DeckResponse` completo.

## PUT /api/decks/{id}

Request: `UpdateDeckRequest` con `name` y `cards`.

Response: `DeckResponse` actualizado.

## DELETE /api/decks/{id}

Response: `204 No Content`.

## GET /api/decks

Query param: `playerId`

Response:
```json
{
  "items": [
    {
      "id": "seed-fire-deck",
      "name": "Seed Fire Deck",
      "valid": true,
      "totalCards": 60
    }
  ]
}
```

## POST /api/decks/{id}/validate

Response:
```json
{
  "valid": false,
  "errors": [
    {
      "code": "DECK_SIZE_INVALID",
      "message": "El mazo debe tener exactamente 60 cartas.",
      "details": {
        "currentSize": 55,
        "requiredSize": 60
      }
    }
  ]
}
```

## POST /api/decks/validate

Valida cartas sin necesidad de un deck persistido. Request: `ValidateDeckRequest` con `{ cards: [{ cardId, quantity }] }`.

Response: `DeckValidationResponse`.

## GET /api/ranking

Response (200):
```json
{
  "items": [
    {
      "rank": 1,
      "playerId": "player-uuid",
      "displayName": "Santi",
      "totalWins": 10,
      "totalLosses": 3,
      "winRate": 0.77,
      "currentWinStreak": 5,
      "maxWinStreak": 5
    }
  ]
}
```

Ordenado por `totalWins` descendente, luego `maxWinStreak` descendente.

## GET /api/players/{id}/stats

Response (200):
```json
{
  "playerId": "player-uuid",
  "displayName": "Santi",
  "totalWins": 10,
  "totalLosses": 3,
  "currentWinStreak": 5,
  "maxWinStreak": 5
}
```

Para jugadores sin partidas: todos los campos numéricos en 0.

## Post-MVP endpoints

Not yet implemented:
- `/api/chat`
