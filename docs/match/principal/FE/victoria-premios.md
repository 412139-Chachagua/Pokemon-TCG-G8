# Victoria y Cartas de Premio

## Vista General

Las cartas de premio son un mecanismo central de victoria en Pokémon TCG: cada vez que el jugador debilita un Pokémon del oponente, toma una carta de premio. El primero en tomar todas sus cartas de premio (o cumplir otras condiciones de victoria) gana la partida. El frontend maneja esto mediante **PrizeZoneComponent** (visualización de premios), **VictoryOverlayComponent** (pantalla de fin de partida), y **MatchSummaryComponent** (resumen post-partida).

## PrizeZoneComponent

`FE/src/app/features/match/components/prize-zone/prize-zone.component.ts`

Componente que muestra las cartas de premio en una cuadrícula vertical u horizontal.

### Estructura

```html
<div class="grid justify-items-center h-full"
     [style.grid-template-rows]="'repeat(' + totalPrizeCount() + ', 1fr)'">
    @for (slot of slots; track $index) {
        @if (slot) {
            <img [src]="cardBackUrl" alt="Premio" ... />
        } @else {
            <div></div>  <!-- Slot vacío (premio ya tomado) -->
        }
    }
</div>
```

### Inputs
- `prizeCount: number` — Cantidad de premios restantes (obligatorio).
- `isOwn: boolean` — Si es del jugador o del oponente (obligatorio).
- `totalPrizeCount: number` — Total de premios iniciales (default 6).
- `columns: 1 | 2 | 3` — Número de columnas (default 2, pero en `PlayerAreaComponent` se usa 1).

### Lógica de Slots

```typescript
get slots(): boolean[] {
    const total = this.totalPrizeCount();
    const remaining = this.prizeCount();
    return Array.from({ length: total }, (_, i) => i < remaining);
}
```

Genera un array de booleanos donde `true` = premio aún visible, `false` = slot vacío (premio ya tomado).

### Ubicación

En `PlayerAreaComponent`, la zona de premios se muestra a la izquierda del área del jugador. En `OpponentAreaComponent`, se muestra del lado del oponente. Cada jugador ve sus propios premios y los del oponente (los del oponente también se muestran como carta reverso).

## Toma Automática de Premios

Cuando un Pokémon es debilitado, el backend establece `pendingPrizeOwnerPlayerId` en el estado público. El frontend detecta esto y automáticamente envía la acción `TAKE_PRIZE_CARD`.

### Effect de Toma de Premios

En `MatchPage` (constructor, línea 1379):

```typescript
effect(() => {
    const shouldTake = this.needsPrizeTake();  // ¿Soy yo quien debe tomar premio?
    const { status, inProgress } = untracked(() => ({
        status: this.matchState.publicState()?.status,
        inProgress: this.interactionService.actionInProgress(),
    }));
    if (shouldTake && this.matchId) {
        if (myId && status === 'ACTIVE' && !inProgress) {
            untracked(() => this.dispatcher.dispatchAction(matchId, myId, 'TAKE_PRIZE_CARD', {}));
        }
    }
});
```

**Importante**: Se usa `untracked()` para evitar loops infinitos, ya que `dispatchAction` internamente lee señales que causarían que el effect se re-dispare.

### needsPrizeTake

```typescript
readonly needsPrizeTake = computed(() => {
    const state = this.matchState.publicState();
    const myId = this.matchState.myPlayerId();
    if (!state || !myId) return false;
    return state.pendingPrizeOwnerPlayerId != null && state.pendingPrizeOwnerPlayerId === myId;
});
```

### Toast de Premio

Cuando se completa la toma (`PRIZE_TAKEN`), `MatchStateService` muestra:
```typescript
if (event.type === 'PRIZE_TAKEN') {
    const count = (event.payload?.['prizeCount'] as number) ?? 1;
    const owner = event.payload?.['playerId'];
    if (owner === this.myPlayerId()) {
        this.toastService.show(`Tomaste ${count} carta de premio`, 'reward', 3000);
    }
}
```

## VictoryOverlayComponent

`FE/src/app/features/match/components/victory-overlay/victory-overlay.component.ts`

Se muestra cuando `state.status === 'FINISHED'`. Es un modal centrado que indica el resultado de la partida.

### Inputs
- `winnerPlayerId: string | null` — ID del ganador.
- `myPlayerId: string | null` — ID del jugador local.
- `opponentName: string` — Nombre del oponente para mostrar en el mensaje.

### Outputs
- `returnToLobby: void` — Evento para volver al lobby.

### Lógica de Resultado

```typescript
protected readonly isWinner = computed(() => {
    return this.winnerPlayerId() !== null && this.winnerPlayerId() === this.myPlayerId();
});
```

### Mensajes

- Victoria: "¡Has ganado! [oponente] ha sido derrotado." (texto verde)
- Derrota: "¡Has perdido! [oponente] ha ganado la partida." (texto rojo)

### Botón "Volver al lobby"

Incluye protección contra doble clic (`lobbyClicked` flag) y navega a `/lobby`.

### Estilos

- Overlay semitransparente oscuro.
- Tarjeta centrada con fondo oscuro (`#1e293b`) y borde.
- Botón azul con hover.
- Diseño responsive: max-width 400px, 90% del viewport en móviles.

## MatchSummaryComponent

`FE/src/app/features/match/components/match-summary/match-summary.component.ts`

Componente actualmente es un esqueleto (template en HTML externo, 8 líneas de TypeScript). Está preparado para mostrar un resumen detallado post-partida con estadísticas, pero no tiene lógica implementada aún. Se encuentra importado pero no se usa activamente en el template de `MatchPage`.

## Manejo de KO y Reemplazo

Cuando un Pokémon es debilitado, el estado público recibe:
- `pendingKOReplacement: boolean`
- `knockedOutPlayerId: string`

### Reemplazo del Jugador Local

Cuando `needsKOReplacement()` es `true`, se muestra:
1. Un modal informativo (`ActionOverlayComponent`) con el mensaje "Tu Pokémon activo fue debilitado. Seleccioná un Pokémon del banco para reemplazarlo."
2. Los Pokémon en banca se resaltan como seleccionables.
3. Al hacer clic en uno, se envía `CHOOSE_KO_REPLACEMENT`.

### Reemplazo del Oponente

Cuando `opponentNeedsKOReplacement()` es `true`, se muestra un modal de espera.

### Reemplazo Automático

Si el jugador debilitado no tiene Pokémon en banca, la partida puede terminar (si no hay reemplazo posible).

## Efectos Visuales de KO

En `MatchStateService`, cuando ocurre `KNOCKOUT_OCCURRED`:

1. Se reproduce el sonido de KO.
2. Se activa `_koTrigger` para que `MatchPage` muestre una pantalla oscurecida (`_koDarken`) con animación durante 2.5 segundos.
3. Para el jugador local que perdió un Pokémon: toast "¡Debilitaron a tu Pokémon!".
4. Para el oponente: toast "¡Debilitaste a un Pokémon del oponente!".

## Animación de Flying Card

Cuando se juega una carta (entrenador, energía), una animación muestra la carta volando desde la mano hacia el campo:

```typescript
private showFlyingCard(handIndex: number): void {
    const card = hand?.[handIndex];
    const url = cardDef?.imageSmallUrl ?? cardDef?.imageLargeUrl;
    this._flyingCard.set({ name: cardDef?.name ?? card.name, imageUrl: url });
    setTimeout(() => this._flyingCard.set(null), 900);
}
```

Se renderiza como una carta flotante animada en la parte inferior central de la pantalla.

## Game Event Formatter

Eventos relacionados con victoria y premios:
- `PRIZE_TAKEN`: "Tomaste N carta(s) de premio" / "El oponente tomó N carta(s) de premio"
- `KNOCKOUT_OCCURRED`: "¡Debilitaron a tu Pokémon!" / "¡Debilitaste a un Pokémon del oponente!"
- `KO_REPLACEMENT_REQUIRED`: "Debés reemplazar tu Pokémon debilitado" / "El oponente debe reemplazar su Pokémon debilitado"
- `KO_REPLACEMENT_DONE`: "Reemplazaste tu Pokémon activo" / "El oponente reemplazó su Pokémon activo"
- `VICTORY_DECIDED`: "¡La partida terminó!"
- `SUDDEN_DEATH_STARTED`: "¡Muerte súbita! Cada jugador tiene 1 carta de premio."

## Conceder Partida

El menú de partida (`MatchMenuComponent`) tiene una opción "Conceder" que llama a:
```typescript
protected onConcede(matchId: string | undefined): void {
    this.matchApi.concedeMatch(matchId, myId).subscribe({
        next: () => this.onReturnToLobby(),
        error: () => this.onReturnToLobby(),
    });
}
```

Al conceder, el backend marca la partida como `FINISHED` con `finishReason: 'CONCEDE'`.
