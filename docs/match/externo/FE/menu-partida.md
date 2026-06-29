# Menú de Partida

## Ubicación
`FE/src/app/features/match/components/match-menu/match-menu.component.ts`

## Propósito
Panel modal que se abre durante la partida para acceder a opciones de juego: reanudar, ajustar volumen, abandonar partida.

## Activación
El componente se muestra condicionalmente mediante la señal `isOpen` (input `boolean`). La apertura/cierre se controla desde el padre (`MatchPageComponent`), típicamente con la tecla Escape o un botón en la interfaz.

El menú también responde a atajos de teclado vía `host` bindings:
- `ArrowDown` / `ArrowUp` — navegar entre ítems.
- `ArrowRight` / `ArrowLeft` — ajustar sliders.
- `Enter` — ejecutar acción del ítem seleccionado.
- Escape (document:keydown) — cerrar menú.
- Tecla `V` — reanudar partida.
- Tecla `S` — abrir confirmación de abandono.

## Opciones del menú

| ID | Etiqueta | Tipo | Acción |
|---|---|---|---|
| `resume` | Volver a la partida | `action` | Emite `closeMenu` |
| `music` | Música de fondo | `slider` | Ajusta volumen vía `AudioService.setBoardVolume()` |
| `sfx` | Efectos de sonido | `slider` | Ajusta volumen vía `AudioService.setSfxVolume()` |
| `concede` | Abandonar partida | `danger` | Muestra diálogo de confirmación |

Los separadores visuales (`concede_separator`, `shortcuts_separator`) no son interactuables.

## Diálogo de confirmación de abandono
Cuando se selecciona "Abandonar partida", se muestra un modal secundario (z-index 110) con:
- Ícono de advertencia.
- Mensaje: "¿Estás seguro? Se contará como derrota."
- Botón "Cancelar" — cierra la confirmación.
- Botón "Abandonar" — emite `concede.emit(matchId)` para que el padre maneje la rendición.

## Visual
- Overlay semitransparente (`bg-black/70`).
- Panel central con borde y fondo usando variables CSS `--pk-*`.
- Encabezado con avatar del jugador y nombre.
- Los ítems tipo `slider` muestran un `<input type="range">` con el valor actual en porcentaje.
- Los ítems tipo `action` o `danger` muestran la tecla de atajo.
- Footer con instrucción "Presioná Escape para cerrar".

## Outputs
- `closeMenu` — se emite al cerrar el menú (click en ✕, clic en "Volver", tecla V, clic en backdrop).
- `concede` — se emite con el `matchId` cuando se confirma el abandono.

## Dependencias
- `AuthService` — nombre y avatar del jugador.
- `AudioService` — control de volumen (`boardVolume`, `sfxVolume`).
- `AvatarService` — resolución de URL de avatar.
