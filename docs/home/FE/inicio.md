# Pantalla de Inicio (Frontend)

## Estructura de Rutas

### `app.routes.ts` (rutas globales)

| Ruta            | Componente/Módulo          | Guard            | Descripción                            |
|-----------------|---------------------------|------------------|----------------------------------------|
| `""`            | Redirige a `/welcome`     | —                | Raíz de la aplicación                  |
| `/welcome`      | `SplashComponent`         | —                | Pantalla de bienvenida/splash          |
| `/home`         | `homeRoutes`              | `authGuard`      | Home principal (lazy loading)          |
| `/auth/*`       | `authRoutes`              | `alreadyAuthGuard` | Login y registro                     |
| `/profile`      | `profileRoutes`           | —                | Perfil detallado del jugador           |
| `/cards`        | `cardRoutes`              | `authGuard`      | Catálogo de cartas                     |
| `/decks`        | `deckRoutes`              | `authGuard`      | Mazos del jugador                      |
| `/lobby`        | `lobbyRoutes`             | `authGuard`      | Sala de partidas                       |
| `/match/*`      | `matchRoutes`             | `authGuard`      | Partida en curso                       |
| `/ranking`      | `rankingRoutes`           | `authGuard`      | Ranking global                         |
| `/history`      | `historyRoutes`           | `authGuard`      | Historial de partidas                  |
| `/rules`        | `rulesRoutes`             | `authGuard`      | Reglas del juego                       |
| `/sandbox`      | `sandboxRoutes`           | —                | Área de pruebas                        |
| `/quick-match`  | `QuickMatchPage`          | `authGuard`      | Acceso directo a partida rápida        |
| `**`            | Redirige a `/home`        | —                | Ruta comodín (fallback)                |

### `routes.ts` del módulo Home

```typescript
export const homeRoutes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/home-page/home-page').then((m) => m.HomePage),
  },
];
```

La ruta `/home` carga el `HomePage` con lazy loading y está protegida por `authGuard`: si el usuario no está autenticado, es redirigido al login.

---

## Flujo de Navegación

```
/ (raíz)
  └── redirige a /welcome
        └── SplashComponent
              ├── Usuario autenticado  → /home
              └── Usuario no autenticado → /auth/login
/home
  └── HomePage
        ├── JUGAR          → /lobby
        ├── MIS MAZOS      → /decks
        ├── CATALOGO       → /cards
        ├── REGLAS         → /rules
        ├── MI PERFIL      → /profile
        ├── Ranking (toggle) → overlay con RankingPage
        ├── Historial (toggle) → overlay con HistoryPage
        └── Cerrar sesión  → /auth/login
```

---

## Pantalla de Bienvenida (Splash)

**Componente:** `SplashComponent`
- **Selector:** `app-splash`
- **Ruta:** `/welcome`
- **Archivos:** `splash.component.ts`, `splash.component.html`, `splash.component.css`

### Propiedades de entrada (`@Input`)

| Propiedad     | Valor por defecto                          | Descripción                          |
|---------------|--------------------------------------------|--------------------------------------|
| `bgImage`     | `assets/images/bg-lavender-town.jpeg`      | Imagen de fondo                      |
| `gameVersion` | `VERSION GRUPO-08`                         | Texto de versión mostrado en pantalla|
| `footerText`  | Texto de copyright de Nintendo y Pokémon   | Texto del pie de página              |

### Comportamiento

1. Muestra el logo de Pokémon TCG con un efecto de brillo animado (`splash__logo-shine`), los símbolos XY, la versión y el texto "PRESIONA CUALQUIER TECLA" con parpadeo.
2. El fondo tiene un efecto de líneas de scan (simulando una pantalla CRT) y la imagen de Lavender Town con filtro de brillo.
3. Al hacer clic o presionar cualquier tecla:
   - Activa una transición visual (`transition-flash`) de 450ms que simula un destello blanco.
   - Si el usuario está autenticado (`auth.isAuthenticated()`), navega a `/home`.
   - Si no, navega a `/auth/login`.
4. La propiedad `transitioning` evita múltiples navegaciones simultáneas.

### Estilos destacados

- Usa la tipografía **Press Start 2P** (estilo retro pixelado).
- Animación `border-glow` en el logo con brillo pulsante púrpura (`#9b6dff`).
- Animación `key-blink` para el texto "PRESIONA CUALQUIER TECLA".
- Animación `transition-fade` para el destello de transición entre pantallas.
- Soporte para `prefers-reduced-motion`.

---

## Pantalla de Home

**Componente:** `HomePage`
- **Selector:** `app-home-page`
- **Ruta:** `/home` (protegida por `authGuard`)
- **Archivos:** `home-page.ts`, `home-page.html`, `home-page.css`

### Layout

La pantalla se divide en dos columnas mediante CSS Grid (`grid-template-columns: 300px 1fr`):

#### Columna izquierda (`hub__left`)

1. **Panel de saludo:** Muestra un saludo según la hora del día (Buenos días / Buenas tardes / Buenas noches) y el nombre del entrenador (desde `auth.player().displayName` o "Entrenador" por defecto). Incluye un botón "MI PERFIL" que navega a `/profile`.
2. **Tarjetas de menú:** Cuatro botones principales (JUGAR, MIS MAZOS, CATALOGO, REGLAS) que navegan a sus respectivas rutas.
3. **Fila de pie:** Botón "Cerrar sesión" y copyright de Nintendo/Pokémon.

#### Columna derecha (`hub__right`)

1. **Toolbar superior:** Dos botones para toggle:
   - **Historial** (icono de documento): muestra/oculta `HistoryPage` en un overlay.
   - **Ranking** (icono de globo): muestra/oculta `RankingPage` en un overlay.
2. Los overlays se muestran en un panel con scroll y son mutuamente excluyentes (solo uno visible a la vez).

### Imagen de fondo

El fondo del home cambia según la hora del día:
- `state-morning.png` (6:00 - 11:59)
- `state-afternoon.png` (12:00 - 17:59)
- `state-night.png` (18:00 - 5:59)

La imagen se aplica como fondo con filtro de brillo y saturación reducidos.

### Responsive

En pantallas de hasta 768px, el layout cambia a una sola columna y la columna derecha (overlays) se muestra primero.

---

## Widget de Perfil (MyProfile)

**Componente:** `MyProfileComponent`
- **Selector:** `app-my-profile` (standalone)
- **Archivos:** `my-profile.component.ts`, `my-profile.component.html`, `my-profile.component.css`

Actualmente este componente **no se usa directamente en la pantalla de Home**; está disponible como componente standalone. Muestra una tarjeta visual con estilo de "Ficha de Entrenador" que incluye:

- **ID del entrenador** (desde `TrainerService.trainerId`)
- **Nombre** (desde `AuthService.player().displayName`)
- **Tiempo de juego** (contador en vivo actualizado cada segundo, formato `HH:MM:SS`, persistido mediante `TrainerService`)
- **Fecha de inicio de aventura** (desde `TrainerService.startDate`)
- **Avatar** (desde `AvatarService.resolve()`, con fallback a iniciales si no hay imagen o hay error de carga)

---

## Servicio de Audio

**Servicio:** `AudioService`
- **Provider:** `providedIn: 'root'`
- **Archivo:** `audio.service.ts`

### Categorías de audio

| Categoría  | Archivo                       | Loop | Volumen default | Rutas donde aplica          |
|------------|-------------------------------|------|-----------------|-----------------------------|
| `splash`   | `title-screen-song.mp3`       | Sí   | 0.2             | `/welcome`                  |
| `general`  | `menu-ost_A.mp3`              | Sí   | 100%            | `/home`, `/decks`, `/cards`, etc. |
| `board`    | `board-song.mp3`              | Sí   | 40%             | `/match/*`                  |
| `silent`   | —                             | —    | —               | `/auth/*`, `/sandbox`       |

### Comportamiento

1. **Cambio automático de música:** Escucha los eventos `NavigationEnd` del router y reproduce la categoría correspondiente a la ruta actual.
2. **Volumen persistente:** Los volúmenes de general, board y SFX se guardan en `localStorage` con claves `pokemon_general_volume`, `pokemon_board_volume`, `pokemon_sfx_volume`.
3. **Autoplay handler:** Como los navegadores bloquean el autoplay, el servicio escucha el primer clic o tecla del usuario para habilitar la reproducción.
4. **Efectos de sonido (SFX):**
   - `click-sound.mp3` — se reproduce en cada clic en la página.
   - Sonidos por tipo de Pokémon (`playTypeSound()`): WATER, FIRE, GRASS, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, FAIRY, COLORLESS, METAL.
   - Sonidos de juego: evolución, Pokémon activo, primer inicio, KO, energía, banca (con variantes rotativas).
5. **Persistencia de sesión:** El tiempo de juego (`playtime`) se actualiza en tiempo real y se guarda al destruir el componente mediante `TrainerService.savePlaytime()`.
