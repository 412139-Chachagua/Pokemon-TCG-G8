# Legend Panel y Card Preview Overlay

## Ubicación
- Panel de leyenda: `FE/src/app/features/match/components/legend-panel/legend-panel.component.ts`
- Vista previa de carta: `FE/src/app/features/match/components/card-preview-overlay/card-preview-overlay.component.ts`
- Servicio de preview: `FE/src/app/features/match/services/card-preview.service.ts`

---

## LegendPanelComponent

### Propósito
Panel desplegable que muestra la **leyenda de tipos de energía** con sus colores correspondientes. Ayuda al jugador a identificar los tipos de energía en las cartas de la mano y las fichas adheridas a los Pokémon.

### Funcionamiento
- Botón "Leyenda" en la parte superior central (`fixed top-0 left-1/2 -translate-x-1/2`).
- Al hacer clic, el panel se despliega hacia abajo con transición CSS (`translate-y-0` / `-translate-y-full`, duración 300ms).
- Se cierra con el botón ✕ o con la tecla Escape.
- El panel tiene `pointer-events-none` para que los clics pasen a través del contenido (excepto el botón de cierre que tiene `pointer-events-auto`).

### Contenido
- Nota aclaratoria: "Estos colores indican el tipo de energía en los bordes de las cartas en tu mano y las fichas de energía adheridas a los Pokémon."
- Grilla de 2 columnas con los 11 tipos de energía del juego:

| Tipo | Color | Nombre |
|---|---|---|
| FIRE | `#ef4444` | Fuego |
| WATER | `#3b82f6` | Agua |
| GRASS | `#22c55e` | Planta |
| LIGHTNING | `#eab308` | Rayo |
| PSYCHIC | `#a855f7` | Psíquico |
| FIGHTING | `#d97706` | Lucha |
| DARKNESS | `#4a044e` | Siniestro |
| METAL | `#9ca3af` | Metal |
| FAIRY | `#f472b6` | Hada |
| DRAGON | `#f59e0b` | Dragón |
| COLORLESS | `#d1d5db` | Incolora |

- Cada entrada muestra un círculo de color de 14px y el nombre del tipo.

---

## CardPreviewOverlayComponent

### Propósito
Overlay modal que muestra una **vista previa ampliada** de una carta. Se activa al hacer clic en el botón "+" de una carta en el `DiscardViewerComponent` u otros componentes que invoquen `CardPreviewService.open()`.

### Servicio `CardPreviewService`
```typescript
interface CardPreviewData {
  cardId: string;
  name: string;
}
```
- **Estado**: señal `previewCard` (`CardPreviewData | null`).
- **Métodos**: `open(data)` establece la carta a previsualizar; `close()` la limpia.

### Funcionamiento
- Escucha la señal `previewSvc.previewCard()`.
- Cuando tiene un valor, muestra un overlay `fixed inset-0 z-[300] bg-black/70`.
- La imagen de la carta se renderiza con el pipe `cardImage:'large'` para tamaño completo.
- Contenedor responsivo: `max-h-[85vh] max-w-[85vw]`.
- Botón de cierre circular en la esquina superior derecha (blanco con borde, se vuelve rojo al hover).
- Se cierra con:
  - Clic en el botón ✕
  - Clic en el backdrop (solo si el clic es directamente en el `.fixed`)
  - Tecla Escape (via `@HostListener('document:keydown.escape')`)

### Dependencias
- `CardImagePipe` — resolución de imagen en tamaño `'large'`.
- `CardPreviewService` — estado compartido de la carta a previsualizar.
