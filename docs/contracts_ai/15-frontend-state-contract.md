# Frontend State Contract

## Goal

Define frontend responsibilities.

Frontend is presentation and interaction only.

## PENDIENTE DE REVISIÓN

Este contrato es principalmente sobre frontend. No se ha analizado el frontend en profundidad durante este cambio. Sin embargo, los responses del backend han cambiado significativamente:

- `publicState` ahora es `PublicGameState` con `PublicPlayerState` y `PublicPokemonSlot` (clases separadas)
- `privateState` ahora es `PrivatePlayerState` con `PrivateHandCard` y `PrizeSlot`
- Los eventos ya no son `string[]` sino `GameEventDto[]` con `{type, message, payload}`
- `attachedCards` se expone como `String[]` de `cardDefinitionIds`, no `attachedEnergyCount`
- `prizes` se expone como `String[]` con "FACE_DOWN"
- Se agregó `firstPlayerId` en `publicState`

Se recomienda revisar y actualizar este contrato cuando se analice el frontend.

## Frontend location

```
features/match/
shared/models/
core/api/
core/websocket/
```

## Frontend may store

- publicState: MatchPublicView
- privateState: PlayerPrivateView
- events: string[]
- connectionStatus: "CONNECTED" | "DISCONNECTED" | "RECONNECTING"
- selectedCardInstanceId: string | null
- selectedTargetInstanceId: string | null
- lastError: ApiError | null

## Frontend must not store as truth

- calculated damage
- victory decision
- legal move decision
- opponent hidden card identities
- deck order

## Match page model example

```json
{
  "publicState": {
    "matchId": "9a747f90-b50e-49df-9d8a-456c9796aa11",
    "status": "ACTIVE",
    "phase": "MAIN",
    "currentPlayerId": "player-1"
  },
  "privateState": {
    "playerId": "player-1",
    "hand": []
  },
  "events": [],
  "connectionStatus": "CONNECTED",
  "selectedCardInstanceId": null,
  "selectedTargetInstanceId": null,
  "lastError": null
}
```

## Action panel rules

Buttons are enabled based on server state.

Frontend may use simple checks for UX, but backend remains authoritative.

Example: Attach Energy button may be disabled if:
- not player's turn
- phase is not MAIN
- hasAttachedEnergy is true

Even if frontend enables a wrong button, backend must reject invalid action.

## GameAction dispatch example

`dispatchAttachEnergy(handIndex: number, targetPokemonInstanceId: string): void`

must send:
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

## Required match UI components

MVP:
- match-page
- board
- player-area
- opponent-area
- active-pokemon-slot
- bench-zone
- hand-zone
- action-panel
- game-log

Later:
- prize-zone
- discard-zone
- deck-zone
- stadium-zone
- drag-drop-targets

## Visual privacy

Reglas de privacidad que el frontend debe respetar (verificadas contra el backend):

| Regla | Estado |
|-------|--------|
| Mano del oponente: solo count, nunca identidades | ✅ IMPLEMENTADO |
| Mazo del oponente: solo count, nunca orden | ✅ IMPLEMENTADO |
| Premios del oponente: solo count, nunca contenido | ✅ IMPLEMENTADO |
| Private state recibido solo del propio jugador | ✅ IMPLEMENTADO |

Opponent hand is exposed only as a count in the public state view. Never render opponent hand card names unless a specific rule reveals them.

```json
{
  "handCount": 6
}
```

Never render opponent hand card names unless a specific rule reveals them.
