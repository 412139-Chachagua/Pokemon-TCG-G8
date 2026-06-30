# Pantalla de Reglas (Rules Page)

## Ubicación en el proyecto

- **Componente**: `FE/src/app/features/rules/pages/rules-page/rules-page.ts`
- **Template**: `FE/src/app/features/rules/pages/rules-page/rules-page.html`
- **Estilos**: `FE/src/app/features/rules/pages/rules-page/rules-page.css`
- **Contenido**: `FE/src/app/features/rules/pages/rules-page/rules-content.ts`
- **Ruta**: `FE/src/app/features/rules/routes.ts`

La ruta se define con lazy loading: `loadComponent(() => import(...).then(m => m.RulesPage))`.

---

## Estructura general

La página se compone de tres grandes bloques:

1. **Sidebar (índice)** — navegación entre secciones principales.
2. **Sub‑navegación** — enlaces a subsecciones dentro de la sección activa.
3. **Cuerpo de contenido** — HTML renderizado con las reglas.

El layout usa `display: flex` con un `aside` lateral fijo (sticky) y el contenido expansivo al lado. Un botón toggle permite ocultar/mostrar la sidebar.

### Sidebar

- Lista vertical de botones, uno por cada sección en `SECTIONS`.
- El botón activo se resalta con la clase `.active` (color `--pk-accent`).
- Al hacer clic se llama a `selectSection(i)`, que actualiza `activeIndex` y reinicia `activeSubIndex` a 0.
- El contenedor tiene `max-height: calc(100dvh - 9rem)` con `overflow-y: auto` para que sea scrolleable independientemente.
- Al cerrar la sidebar (`sidebarOpen === false`), la clase `.sidebar-closed` colapsa el ancho a 0 y oculta el contenido con transición de 0.3s.

### Sub‑navegación

- Se muestra dentro del `pk-panel__header` únicamente si la sección activa tiene subsecciones.
- Son botones tipo chip que al hacer clic ejecutan `scrollToSub(sub.id, j)`, que hace `scrollIntoView({ behavior: 'smooth', block: 'start' })` hacia el elemento HTML con ese `id`.
- El botón activo también se resalta (`.sub-nav__item.active`).

### Cuerpo de contenido

- Un `<div #contentBody class="pk-panel__body prose">` que se llena con `innerHTML` desde `activeContent()`.
- `activeContent` es un `computed` que retorna `sections[activeIndex()].content`.
- Un `effect()` observa cambios en `activeContent` y asigna el HTML al elemento del DOM.

### Footer

- Texto de copyright: "© 2026 Nintendo. Todos los derechos reservados. Pokémon® es una marca registrada de Nintendo."

---

## Contenido de las reglas (`rules-content.ts`)

El archivo exporta la constante `SECTIONS`, un array de `GuideSection[]`. Cada sección tiene:

| Campo         | Descripción                                              |
|---------------|----------------------------------------------------------|
| `id`          | Identificador para anclajes HTML                         |
| `title`       | Título mostrado en la sidebar                            |
| `content`     | HTML string con el contenido completo de la sección      |
| `subsections` | Array de `{ id, title }` para la sub‑navegación          |

### Secciones cubiertas

| # | Título | Subsecciones | Descripción |
|---|--------|--------------|-------------|
| 0 | ¡Conviértete en Maestro Pokémon! | — | Introducción al juego, qué es un Entrenador, cómo se usan las barajas. |
| 1 | Conceptos básicos de JCC Pokémon | Cómo ganar, Tipos de Energía | Las 3 formas de ganar, tabla con los 11 tipos de Energía y sus características. |
| 2 | Partes de una carta Pokémon | Anatomía de una carta Pokémon, Anatomía de una carta de Entrenador, Los 3 tipos de cartas | Descripción detallada de cada elemento de una carta Pokémon y de Entrenador, más los 3 tipos (Pokémon, Energía, Entrenador). |
| 3 | Zonas de JCC Pokémon | Pokémon Activo, Banca, Cartas de Premio, Pila de Descartes | Descripción del tablero: Activo, Banca, Mano, Baraja, Premio, Descartes. |
| 4 | Cómo jugar | Cómo iniciar una partida, Partes de un turno, Acciones en cada turno, El ataque, Paso entre turno y turno, Condiciones Especiales | La sección más grande. Cubre preparación, estructura del turno (robo, acciones A‑F, ataque), cálculo de daño, condiciones especiales (Dormido, Quemado, Confundido, Paralizado, Envenenado). |
| 5 | Ligas Pokémon | — | Información sobre torneos y ligas oficiales. |
| 6 | Reglas avanzadas | Mulligan, ¿Qué se considera un ataque?, Todos los detalles del ataque, Robar más cartas de las disponibles, Ambos jugadores ganan al mismo tiempo, ¿Qué forma parte del nombre de un Pokémon? | Mulligan, definición de ataque vs habilidad, pasos detallados del ataque, muerte súbita, reglas de nombre de cartas. |
| 7 | Creación de barajas | Reglas obligatorias, Indicaciones para principiantes | Reglas de construcción de baraja (60 cartas, máx 4 copias, etc.) y recomendaciones. |
| 8 | Apéndice A: Pokémon-EX | Reglas especiales | Pokémon‑EX: nombre, 2 premios al caer. |
| 9 | Apéndice B: Pokémon Megaevolución | Reglas especiales | MEGA evolución, fin de turno al megaevolucionar, 2 premios. |
| 10 | Apéndice C: Pokémon Recreados | Reglas importantes | Fósiles, reglas de juego. |
| 11 | Apéndice D: Cartas de Entrenador de AS TÁCTICO | — | Límite de 1 carta AS TÁCTICO por baraja. |
| 12 | Apéndice E: Cartas del Equipo Plasma | Reglas importantes | Identificación, nombre, evolución. |
| 13 | Glosario | — | Definiciones de todos los términos del juego (Ataque, Banca, Condiciones Especiales, etc.). |

---

## Enfoque de estilos

- Fondo: imagen de fondo (`--rules-bg` con `url(assets/images/pokemon-center.png)`) con superposición oscura (`linear-gradient(rgba(0,0,0,0.65))`).
- Paleta de colores mediante variables CSS (`--pk-text`, `--pk-accent`, `--pk-panel`, etc.).
- Tipografía responsive con `clamp()` para tamaños de fuente.
- El contenido `.prose` incluye estilos para `h2`, `h3`, párrafos, listas, tablas (con wrapper `.table-scroll` para overflow-x), blockquotes, código inline y `<hr>`.
- Transiciones suaves (0.3s) en sidebar, toggle, hover de items, etc.
- Responsive: a `max-width: 768px` la sidebar se vuelve horizontal (ocupa 100% de ancho), el toggle desaparece y el layout apila verticalmente.
- Animación de entrada: `pk-fade-in` de 0.35s.

---

## Propósito

La página sirve como **referencia oficial** (en castellano, edición XY) para que los jugadores consulten las reglas del juego directamente desde la aplicación. Es completamente estática (renderiza HTML desde constantes), no requiere llamadas a backend, y cubre desde conceptos básicos hasta reglas avanzadas y apéndices con cartas especiales. La navegación con sidebar e índice de subsecciones permite saltar rápidamente al tema deseado.
