# Setup Inicial (Mulligan y Colocación de Pokémon)

## Vista General

El setup inicial es la primera fase de una partida, manejada por el componente **SetupOverlayComponent**. Se muestra cuando el estado público de la partida (`matchState.publicState().status`) es `'SETUP'`. El overlay ocupa toda la pantalla con un fondo temático que cambia según la hora del día (mañana, tarde, noche).

## Componentes Involucrados

### SetupOverlayComponent

`FE/src/app/features/match/components/setup-overlay/setup-overlay.component.ts`

Es el componente principal de la configuración inicial. Recibe como inputs el estado del jugador, confirmaciones del oponente, y los estados de mulligan pendientes.

**Inputs:**
- `myPlayerState: PublicPlayerStateModel | null` — Estado público del jugador local.
- `opponentSetupConfirmed: boolean` — Indica si el oponente ya confirmó su setup.
- `mySetupConfirmed: boolean` — Indica si el jugador local ya confirmó.
- `mulliganDrawPending: boolean` — Si hay un robo de mulligan pendiente.
- `initialMulliganPending: boolean` — Si el mulligan inicial está pendiente.
- `cardDefs: Map<string, CardDetailResponse | null>` — Definiciones de cartas para mostrar nombres.

**Outputs:**
- `activeDropped: string` — Se emite cuando se arrastra una carta al slot activo.
- `benchDropped: { cardInstanceId, benchIndex }` — Se emite al arrastrar a la banca.
- `fieldCardRemoved: string` — Se emite al remover una carta del campo.
- `confirmSetup: void` — Se emite al presionar "Confirmar".

**Flujo de interacción mediante Drag & Drop:**

1. La mano del jugador se muestra en la parte inferior del overlay.
2. Solo los Pokémon Básicos (`isBasicPokemon()`) pueden ser arrastrados. Las cartas que no son básicas se muestran con opacidad reducida y un icono de prohibido.
3. El jugador arrastra cartas desde la mano hacia:
   - La zona **Activo** (drop zone individual, centrada)
   - La zona **Banca** (5 slots en fila)
4. Las zonas drop usan `cdkDropList` de Angular CDK. Al soltar una carta, se emiten los eventos `activeDropped` o `benchDropped`.
5. Cada carta colocada muestra un botón "✕" al hacer hover para removerla (emite `fieldCardRemoved`).
6. Al hacer clic en una carta, se abre el `CardPreviewService` para ver el detalle.

### WaitingPanelComponent

`FE/src/app/features/match/components/waiting-panel/waiting-panel.component.ts`

Se muestra cuando el jugador se une a una partida y espera a que el oponente se conecte. Aparece cuando `matchState.publicState()` es `null` (aún no hay estado de partida). Incluye:

- Título "Buscando jugador..."
- Una animación CSS de Pokébola girando
- Nombre del jugador y nombre del mazo
- Botón "Cancelar partida"

### MulliganHelpPanelComponent

`FE/src/app/features/match/components/mulligan-help-panel/mulligan-help-panel.component.ts`

Panel lateral deslizable (desde la derecha) que explica las reglas del mulligan. Se abre/cierra con un botón en el borde derecho.

## Flujo de Mulligan

El mulligan se maneja con señales reactivas en el `SetupOverlayComponent`:

### Detección del mulligan del oponente

```typescript
readonly opponentResolvingMulligan = computed(() => {
    const pub = this.matchState.publicState();
    if (!pub?.pendingInitialMulliganPlayers?.length) return false;
    const myId = this.matchState.myPlayerId();
    if (!myId) return false;
    return !pub.pendingInitialMulliganPlayers.includes(myId);
});
```

Si el array `pendingInitialMulliganPlayers` existe pero no incluye al jugador local, significa que el oponente está resolviendo su mulligan.

### Cartas reveladas por mulligan

- `opponentMulliganRevealedCards` — Array de grupos de cartas que el oponente reveló (cada grupo es un array de cardIds por ronda de mulligan).
- `myMulliganRevealedCards` — Cartas que el jugador local reveló.

### Modal de cartas de mulligan

El overlay tiene un botón "+ Ver cartas" que abre un modal (`_showMulliganModal`) mostrando todas las cartas reveladas durante el mulligan, tanto del jugador como del oponente. También hay un modal de reglas (`_showRulesModal`) con la explicación completa del mulligan en español.

### Botón de confirmación

El botón "Confirmar" se habilita solo cuando `canConfirm()` devuelve `true`:
- El jugador tiene un Pokémon activo colocado
- El jugador no ha confirmado ya (`setupConfirmed === false`)
- No hay mulligan draw pendiente
- No hay mulligan inicial pendiente
- El oponente no está resolviendo mulligan

## Flujo Completo

1. La partida entra en estado `SETUP`.
2. `MatchPage` renderiza `<app-setup-overlay>` con los inputs correspondientes.
3. Cada jugador arrastra Pokémon Básicos de su mano a los slots activo y banca.
4. Si la mano inicial no tiene Pokémon Básicos, se dispara `showInitialMulliganDialog()` (en `MatchPage`), mostrando un diálogo que ofrece "Hacer mulligan".
5. Al hacer mulligan, el backend baraja y reparte 7 nuevas cartas; el oponente puede robar cartas extra.
6. El jugador puede ver en el overlay cuántas cartas fueron descartadas por mulligan (de ambos lados).
7. Una vez colocados los Pokémon, el jugador presiona "Confirmar" y el estado del oponente se actualiza (`opponentSetupConfirmed`).
8. Cuando ambos confirman, el estado cambia a `ACTIVE` y se muestra la animación de lanzamiento de moneda (coin flip) para determinar quién empieza.

## Comunicación con el Backend

Todas las acciones de setup se envían a través de `GameActionDispatcherService`:

| Acción | Método |
|--------|--------|
| Colocar activo | `placeActive(matchId, playerId, cardInstanceId)` |
| Colocar banca | `placeBench(matchId, playerId, cardInstanceId, benchIndex)` |
| Remover activo | `removeActive(matchId, playerId)` |
| Remover banca | `removeBench(matchId, playerId, cardInstanceId)` |
| Confirmar setup | `confirmSetup(matchId, playerId)` |
| Resolver mulligan inicial | `resolveInitialMulligan(matchId, playerId, decision)` |
| Resolver robo de mulligan | `resolveMulliganDraw(matchId, playerId, drawCards)` |
