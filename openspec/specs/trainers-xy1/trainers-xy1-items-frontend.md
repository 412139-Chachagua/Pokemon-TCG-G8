# SPEC 1B — ITEMS XY1 Frontend (7 cartas)

## Archivos a modificar

| Archivo | Cambio |
|---------|--------|
| `FE/src/app/shared/models/card.models.ts` | Agregar `POKEMON_TOOL` al type `TrainerSubtype` |
| `FE/src/app/shared/components/pokemon-card/pokemon-card.component.ts` | Agregar template condicional para supertype TRAINER con badge de subtipo |
| `FE/src/app/features/match/pages/match-page/match-page.ts` | Manejar interacciones de ITEM (PLAY_TRAINER para items no-tool) |
| `FE/src/app/features/match/components/hand-zone/hand-zone.component.ts` | Mostrar badge "ITEM" en vez de "TRAINER" generico |

## Vistas

### Catalogo (card-view.component.ts)
```
Sin cambios: ya muestra imagen + nombre + supertype
Los items se ven igual que otros trainers en la grilla
```

### Detalle de carta (pokemon-card.component.ts)
```
Cuando supertype === 'TRAINER':
- Mostrar badge de subtipo:
  - ITEM -> azul (#3B82F6)
  - SUPPORTER -> naranja (#F59E0B)
  - STADIUM -> verde (#10B981)
- NO mostrar secciones de: HP, tipos, ataques, debilidades, resistencias, retreat
- Mostrar rulesText[] como parrafos estilizados con icono de regla
- Mostrar set code, number, rareza
```

### Mano durante el juego (hand-zone.component.ts)
```
- Items: badge "ITEM" en azul
- Al clickear: ejecutar PLAY_TRAINER con handIndex
- No son arrastrables (ya implementado)
```

## Card models — cambios

```typescript
export type TrainerSubtype = 'ITEM' | 'SUPPORTER' | 'STADIUM' | 'ACE_SPEC' | 'POKEMON_TOOL';
```
