# SPEC 2B — STADIUM XY1 Frontend (2 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `FE/src/app/features/match/pages/match-page/match-page.ts` | Agregar zona de estadio activo al template |
| `FE/src/app/features/match/components/hand-zone/hand-zone.component.ts` | Mostrar badge "ESTADIO" en verde |
| `FE/src/app/features/match/utils/game-event-formatter.ts` | Mejorar mensajes de eventos STADIUM_PLAYED y STADIUM_REMOVED |
| `FE/src/app/shared/models/game-state.models.ts` | Agregar `stadiumCardInstanceId` si no existe en PublicGameStateModel |
| `FE/src/app/shared/models/game-action.models.ts` | Agregar tipo de accion `ATTACH_TOOL` si no existe |

## Archivos a crear

| Archivo | Proposito |
|---------|-----------|
| `FE/src/app/features/match/components/stadium-zone/stadium-zone.component.ts` | Componente para mostrar el estadio activo en el campo |
| `FE/src/app/features/match/components/stadium-zone/stadium-zone.component.html` | Template del componente |

## Detalle

### Vista detalle de carta
```
STADIUM se ve con badge verde "ESTADIO" y sus rulesText como descripcion.
```

### Zona de estadio (stadium-zone.component — NUEVO)
```
- Slot visible en el campo de juego (entre areas de ambos jugadores)
- Si hay estadio activo:
  - Muestra miniatura de la carta
  - Muestra nombre del estadio
  - Muestra efecto abreviado
- Si no hay estadio:
  - Muestra slot vacio con texto "Sin estadio en juego"
- Se actualiza automaticamente via eventos:
  - STADIUM_PLAYED -> mostrar nueva carta
  - STADIUM_REMOVED -> limpiar slot
```

### Match page (match-page.ts)
```
- Agregar <app-stadium-zone> en el template
- Pasar stadiumCardInstanceId desde el estado publico de la partida
- Escuchar eventos STADIUM_PLAYED / STADIUM_REMOVED
```

### Hand zone (hand-zone.component.ts)
```
- Cartas con subtype STADIUM: badge "ESTADIO" en verde
```

### Game event formatter (game-event-formatter.ts)
```typescript
case 'STADIUM_PLAYED':
  return 'Se jugo un Estadio: {{cardName}}';
case 'STADIUM_REMOVED':
  return 'El Estadio fue reemplazado/removido';
```
