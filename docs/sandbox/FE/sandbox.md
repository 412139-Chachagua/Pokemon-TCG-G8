# Pantalla Sandbox

## Ubicación en el proyecto

- **Componente**: `FE/src/app/features/sandbox/pages/sandbox-page/sandbox-page.ts`
- **Ruta**: `FE/src/app/features/sandbox/routes.ts`

La ruta se define con lazy loading: `loadComponent(() => import(...).then(m => m.SandboxPage))`.

---

## ¿Qué hace?

La Sandbox Page es un **entorno de pruebas y desarrollo** que renderiza todos los componentes principales del juego con datos mockeados (hardcodeados). No hay lógica de juego real: solo se visualizan los componentes con estado ficticio para verificar que se renderizan correctamente.

---

## Componentes que prueba

La página importa y usa los siguientes componentes del módulo `match`:

| Componente | Rol |
|---|---|
| `MatchHeaderComponent` | Cabecera del match: información general del estado de la partida |
| `PlayerAreaComponent` | Área del jugador local (Activo + Banca) |
| `OpponentAreaComponent` | Área del oponente (Activo + Banca) |
| `HandZoneComponent` | Zona de la mano del jugador con las cartas disponibles |
| `GameLogComponent` | Log de eventos ocurridos durante la partida |
| `ActionPanelComponent` | Panel de acciones del jugador |

---

## Datos mockeados

El componente define internamente datos ficticios con comentarios explicativos:

### Pokémon del jugador local (`MY_PLAYER_STATE`)

- **Activo**: Charizard (`base1-4`) con 2 contadores de daño y 3 energías (2 Fuego, 1 Incolora).
- **Banca**: Hitmonchan (`base1-17`, 0 daño, Energía Lucha) y Gyarados (`base1-6`, 0 daño, Paralizado, Energía Agua).
- **Premios**: 4 obtenidos de 6 — muestra `['▰', '▰', '▰', '▰', '', '']`.

### Pokémon del oponente (`OPPONENT_PLAYER_STATE`)

- **Activo**: Mewtwo (`base1-12`) con 1 contador de daño, Confundido, 2 energías Psíquicas.
- **Banca**: Raichu (`base1-11`, 3 daño, Energía Rayo) y Ninetales (`base1-10`, 0 daño, Dormido, sin energías).
- **Premios**: 5 obtenidos de 6.

### Estado global (`MOCK_PUBLIC_STATE`)

- `matchId`: `'sandbox-match'`
- `status`: `'ACTIVE'`, `phase`: `'MAIN'`
- `turnNumber`: 3
- `currentPlayerId` y `firstPlayerId` apuntan al jugador local.

### Mano del jugador (`MOCK_HAND`)

5 cartas de ejemplo: Hitmonchan (Pokémon), Fire Energy (Energía), Energy Removal (Entrenador), Potion (Entrenador), Fighting Energy (Energía).

### Eventos del log (`MOCK_EVENTS`)

16 eventos simulados que cubren distintos tipos de acciones del juego:

- `GAME_START`, `DRAW`, `ENERGY_ATTACH`, `DAMAGE_APPLIED`, `SWITCH`
- `HEAL`, `PRIZE_TAKEN`, `STATUS_APPLIED`, `ENERGY_REMOVAL`, `BENCH_POKEMON`

### Flags de selección

- `validTargets`: array vacío (`EMPTY_TARGETS`).
- `selectionMode`: `'NONE'` — no hay selección activa.

---

## Interactividad y controles

El sandbox **no tiene lógica interactiva**. Las interacciones disponibles son:

1. **Banner superior** con estilo `bg-amber-600/20` y borde `border-amber-500/40`, que muestra:
   - Texto: `🧪 SANDBOX — datos mockeados · sin lógica`
   - Enlace "Salir" que redirige a `/lobby` mediante `routerLink`.

2. **Precarga de cartas** (`ngOnInit`): llama a `cardRepo.preload(mockCardIds)` con 13 IDs de cartas reales de la base Base Set 1 (`base1-*`). Esto permite que las imágenes carguen del CDN de Pokémon TCG aunque el backend no esté corriendo (las definiciones como HP/ataques no se mostrarán sin backend).

---

## Layout

- `display: flex`, dirección `column`, con gap de 1rem y padding de 1.5rem.
- Las áreas de jugador y oponente se muestran lado a lado (`flex-row`) usando dos contenedores `flex-1`.
- La mano se renderiza debajo, seguida del panel de acciones (que solo muestra "Esperando oponente" por no tener estado real) y el log de eventos.

---

## Propósito

El sandbox está diseñado para **desarrollo y testing visual**. Permite:
- Verificar que todos los componentes del match se renderizan sin errores.
- Probar combinaciones de estado (condiciones especiales, daño, energías, premios) sin necesidad de backend.
- Validar la precarga de imágenes de cartas desde el CDN.
- Depurar el layout y estilos de las áreas de juego en un entorno controlado.
