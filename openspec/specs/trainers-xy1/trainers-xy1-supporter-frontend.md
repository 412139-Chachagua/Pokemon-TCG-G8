# SPEC 3B — SUPPORTER XY1 Frontend (4 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `FE/src/app/features/match/pages/match-page/match-page.ts` | Validar hasPlayedSupporter al jugar Supporter |
| `FE/src/app/features/match/components/hand-zone/hand-zone.component.ts` | Mostrar badge "SUPPORTER" en naranja; deshabilitar si ya se jugo uno |
| `FE/src/app/features/match/utils/game-event-formatter.ts` | Mejorar mensajes TRAINER_PLAYED para Supporter |
| `FE/src/app/shared/models/ui-state.models.ts` | Agregar selection mode para Supporter si es necesario |

## Vistas

### Detalle de carta
```
SUPPORTER se ve con badge naranja "SUPPORTER" y rulesText.
```

### Mano durante el juego (hand-zone.component.ts)
```
- Badge "SUPPORTER" en naranja (#F59E0B)
- Si ya se jugo un Supporter este turno (hasPlayedSupporter):
  - Mostrar la carta grisada/deshabilitada
  - No permitir click
  - Mostrar tooltip "Ya jugaste un partidario este turno"
- Si NO se ha jugado Supporter:
  - Al clickear -> PLAY_TRAINER con handIndex
```

### Match page (match-page.ts)
```
Al recibir evento TRAINER_PLAYED con effectType SUPPORTER:
- Actualizar flag hasPlayedSupporter = true
- Bloquear interaccion con otras cartas SUPPORTER en mano

Al empezar turno (TURN_STARTED):
- Resetear hasPlayedSupporter = false
```

### Game event formatter (game-event-formatter.ts)
```typescript
case 'TRAINER_PLAYED':
  if (payload.trainerSubtype === 'SUPPORTER') {
    return 'Jugaste un Partidario: {{cardName}}';
  } else if (payload.trainerSubtype === 'ITEM') {
    return 'Jugaste un objeto: {{cardName}}';
  }
  return 'Jugaste una carta de Entrenador';
```

## Card models — cambios

```typescript
export type TrainerSubtype = 'ITEM' | 'SUPPORTER' | 'STADIUM' | 'ACE_SPEC' | 'POKEMON_TOOL';
```

## UI State

```typescript
export type SelectionMode = 'NONE' | 'SELECT_BENCH_SLOT' | 'SELECT_TARGET_POKEMON'
  | 'SELECT_ATTACK' | 'SELECT_RETREAT_TARGET' | 'SELECT_ENERGIES_TO_DISCARD'
  | 'SETUP_ACTIVE' | 'SETUP_BENCH' | 'SELECT_ENERGY_FOR_SUPER_POTION';
```
