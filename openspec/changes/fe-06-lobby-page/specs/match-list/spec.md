## ADDED Requirements

### Requirement: MatchListComponent

`MatchListComponent` SHALL display a list of waiting matches.

**Outputs:**
- `matchSelected: EventEmitter<string>` (emits matchId when user clicks "Usar este")

**Local signals (private):**
- `matches: Signal<{ id: string; status: string }[]>` (placeholder data, no BE endpoint yet)

**Layout:**
```
┌──────────────────────────────────────────────────────┐
│  Partidas disponibles            [↻ Actualizar]      │
├──────────────────────────────────────────────────────┤
│  match-abc123   |  Esperando jugador   [Usar este]   │
│  match-def456   |  Esperando jugador   [Usar este]   │
├──────────────────────────────────────────────────────┤
│  No hay partidas disponibles.                        │
└──────────────────────────────────────────────────────┘
```

**Behavior:**
- `[Usar este]` button SHALL emit the matchId via `matchSelected`
- `[↻ Actualizar]` SHALL be a stub: log "Refresh not implemented" to console (no BE endpoint yet)
- When `matches` is empty, SHALL show "No hay partidas disponibles."
- Initial `matches` data: empty array (stub)
- `ChangeDetectionStrategy.OnPush`

#### Scenario: MatchListComponent emits matchSelected
- WHEN user clicks "Usar este" on a match row
- THEN `matchSelected` SHALL emit with that match's ID

#### Scenario: MatchListComponent shows empty state
- WHEN `matches` array is empty
- THEN the component SHALL display "No hay partidas disponibles."
