## ADDED Requirements

### Requirement: VictoryOverlayComponent

`VictoryOverlayComponent` SHALL display match result when the game finishes.

**Inputs:** `winnerPlayerId: string | null`, `myPlayerId: string | null`

**Visibility:** SHALL be shown by MatchPage when `matchState.publicState()?.status === 'FINISHED'`

**Display:**
```
┌──────────────────────────────────────┐
│          Fin de partida              │
│   ¡Ganaste! / El oponente ganó.      │
│                                      │
│         [Volver al lobby]            │
└──────────────────────────────────────┘
```

**Behavior:**
- SHALL show "¡Ganaste!" when `winnerPlayerId === myPlayerId`
- SHALL show "El oponente ganó." otherwise
- `[Volver al lobby]` SHALL emit `returnToLobby` — MatchPage calls `matchState.reset()` + navigates
- SHALL use `position: fixed` overlay with semi-transparent backdrop
- SHALL NOT use emojis in the template (consistente con existing codebase)
- SHALL use `standalone: true`, `ChangeDetectionStrategy.OnPush`, `input()/output()`, inline template

**Outputs:** `returnToLobby: void`

**Contract references:** `06-game-state-contract.md` (MatchStatus enum, FINISHED status), `15-frontend-state-contract.md` (frontend state model)

#### Scenario: VictoryOverlay shows winner
- WHEN `winnerPlayerId` equals `myPlayerId`
- THEN displayed text SHALL be "¡Ganaste!"
- WHEN they differ
- THEN displayed text SHALL be "El oponente ganó."

#### Scenario: VictoryOverlay emits returnToLobby
- WHEN user clicks "[Volver al lobby]"
- THEN `returnToLobby` SHALL emit
