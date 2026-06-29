# Mulligan Help Panel (Panel de Ayuda de Mulligan)

## Ubicación
`FE/src/app/features/match/components/mulligan-help-panel/mulligan-help-panel.component.ts`

## Propósito
Panel deslizable informativo que explica las reglas del **mulligan** en el contexto de la partida. Se muestra durante la fase de setup inicial para que los jugadores entiendan el proceso.

## Arquitectura
Componente `standalone` con estado mínimo:
- **`isOpen`** — señal booleana que controla si el panel está visible.

### Activación
- Botón vertical "Mulligan" en el borde derecho de la pantalla, con orientación `writing-mode:vertical-lr`.
- Al hacer clic, se despliega un panel desde la derecha con transición CSS (`translate-x-0` / `translate-x-full`, duración 300ms).

## Contenido del panel
El panel tiene tres secciones informativas separadas por divisores:

### 1. ¿Qué es Mulligan?
- Texto: "Si al repartir las 7 cartas iniciales no tenés ningún Pokémon Básico en tu mano, debés mostrar esas cartas, barajarlas de nuevo en tu mazo y robar 7 cartas nuevas."
- Título en azul (`text-blue-400`).

### 2. Consecuencia
- Texto: "Por cada Mulligan que hagas, tu oponente puede robar 1 carta extra de su mazo antes de comenzar la partida. Esto aplica también si ambos hacen Mulligan."
- Título en rojo (`text-red-400`).

### 3. Límite
- Texto: "No hay límite de Mulligans. Seguís repitiendo el proceso hasta tener al menos un Pokémon Básico en tu mano inicial."
- Título en azul (`text-blue-400`).

### Footer
- Nota: "Reglas oficiales del Juego de Cartas Coleccionables Pokémon."

## Visual
- Panel lateral derecho (anchura responsive: `xl:w-[320px] w-[280px] max-w-[90vw]`).
- Usa variables CSS `--pk-*` para theming.
- Botón de cierre (✕) en el encabezado.
- Altura completa de la ventana (`inset-y-0`).
- Scroll interno para contenido largo.

## Dependencias
- Ninguna — componente puramente informativo sin inyección de servicios.
