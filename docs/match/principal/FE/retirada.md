# Retirada (Retreat)

## Vista General

La retirada permite al jugador cambiar su Pokémon activo por uno de la banca, pagando el costo de energía de retirada. Este proceso se maneja desde **MatchPage**, **ActionPanelComponent**, **PlayerAreaComponent** y **PokemonSlotComponent**.

## Botón de Retirada

En `MatchPage`, el botón de retirada se muestra en la barra de acciones junto a los botones de ataque y habilidad. Se renderiza cuando `retreatInfo()` devuelve un valor no nulo.

### retreatInfo (señal computada)

```typescript
readonly retreatInfo = computed(() => {
    const active = this.matchState.myActivePokemon();
    if (!active) return null;
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    if (!cardDef?.retreatCost) return null;
    const attached = this.matchState.activePokemonEnergyTypes();

    // ¿Ya se retiró este turno?
    if (state?.hasRetreated) return { cost: cardDef.retreatCost, canRetreat: false };

    // Fairy Garden: si el Estadio está activo y el Pokémon tiene Fairy, retirada gratis
    if (stadiumDef?.name === 'Fairy Garden' && attached.includes('FAIRY'))
        return { cost: [], canRetreat: true };

    // Condiciones que bloquean retirada
    const blockedByCondition = active.specialConditions?.includes('ASLEEP')
        || active.specialConditions?.includes('PARALYZED');

    const canRetreat = !blockedByCondition && this.checkCost(cardDef.retreatCost, attached);
    return { cost: cardDef.retreatCost, canRetreat };
});
```

**Factores que afectan la retirada:**
- **Costo de energía**: Se verifica con `checkCost()` contra las energías adjuntas.
- **Una retirada por turno**: `state.hasRetreated`.
- **Condiciones bloqueantes**: Dormido (`ASLEEP`) y Paralizado (`PARALYZED`) impiden retirarse.
- **Fairy Garden**: Estadio que permite retirada gratis si el Pokémon tiene energía Fairy.
- **Primer turno**: No se puede retirar en el primer turno (no se muestra la barra de ataque/retirada).

### Botón Renderizado

```html
@if (retreatInfo(); as retreat) {
    <button
        class="..."
        [disabled]="!retreat.canRetreat"
        (click)="onRetreatInitiated()"
    >
        <span>Retirar</span>
        @for (costItem of retreat.cost; track $index) {
            <img [src]="costItem | energyIcon" ... />
        }
    </button>
}
```

El botón muestra los iconos de energía necesarios para la retirada. Si no se puede retirar, se muestra atenuado.

## Inicio de la Retirada

Cuando el jugador hace clic en "Retirar":

```typescript
protected onRetreatInitiated(): void {
    if (!this.matchId) return;
    const myId = this.matchState.myPlayerId();
    if (!myId) return;
    this.interactionService.enterSelectRetreatTarget(this.benchInstanceIds());
}
```

### EnterSelectRetreatTarget

En `MatchInteractionService`:

```typescript
enterSelectRetreatTarget(validTargets: string[]): void {
    this._selection.set({
        mode: 'SELECT_RETREAT_TARGET',
        selectedHandIndex: null,
        selectedInstanceId: null,
        validTargets,  // IDs de los Pokémon en banca
    });
}
```

Esto activa el modo `SELECT_RETREAT_TARGET`, que:
1. **Resalta los Pokémon en banca** del jugador como seleccionables (`isHighlighted`).
2. El jugador hace clic en uno de los Pokémon de banca.

### Manejo de la Selección

En `MatchPage.onPokemonClicked()`, cuando el modo es `SELECT_RETREAT_TARGET`:

```typescript
if (mode === 'SELECT_RETREAT_TARGET') {
    const targetInstanceId = event.instanceId;
    const bench = this.myPlayerState()?.bench ?? [];
    const benchIndex = bench.findIndex(p => p?.instanceId === targetInstanceId);
    if (benchIndex < 0) return;

    this.dispatcher.retreatActive(this.matchId, myId, benchIndex);
}
```

## Visualización del Cambio (PlayerAreaComponent)

`FE/src/app/features/match/components/player-area/player-area.component.ts`

### Arrastre de Cartas (Drop en Activo)

Cuando se arrastra una carta de energía o Pokémon desde la mano hacia el slot activo:

```typescript
protected onActiveDrop(event: CdkDragDrop<unknown>): void {
    if (data.supertype === 'ENERGY') {
        this.energyDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    } else if (data.supertype === 'POKEMON') {
        this.evolutionDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    } else if (data.supertype === 'TRAINER') {
        this.trainerDropped.emit({ handIndex: data.handIndex, targetInstanceId: active.instanceId });
    }
}
```

### Reorganización del Área del Jugador

El `PlayerAreaComponent` organiza visualmente:
- Zona de premios (izquierda, columna vertical)
- Banca (centro, 5 slots)
- Activo (derecha, slot grande)
- Borde azul para el área propia

## Feedback Visual del Switch

Cuando el backend notifica `RETREAT_EXECUTED` o `SWITCH_EXECUTED`, en `MatchStateService.addEvent()`:

1. Se identifica el `oldActivePokemonInstanceId`.
2. Se activa `_activeSlotFlash` con el `playerId` del dueño.
3. Se reproduce el sonido de Pokémon activo (`AudioService.playActivePokemonSound()`).
4. El flash se limpia después de 1000ms.

En `PokemonSlotComponent`:

```html
@if (slotFlash()) {
    <div class="absolute inset-0 ... animate-active-flash">
        <div class="w-3/4 h-3/4 bg-red-600/60 rounded-full"></div>
    </div>
}
```

## ActionPanelComponent

`FE/src/app/features/match/components/action-panel/action-panel.component.ts`

Aunque el botón de retirada está en `MatchPage` (no en `ActionPanelComponent`), el `ActionPanelComponent` tiene un botón de "Finalizar turno" que aparece durante la fase `MAIN`, y un botón "Cancelar" cuando hay una selección activa. La retirada no está en este panel porque se maneja directamente en la barra de ataque/retirada de `MatchPage`.

## Comunicación con el Backend

| Acción | Método |
|--------|--------|
| Retirar activo | `retreatActive(matchId, playerId, benchIndex, energyCardInstanceIdsToDiscard?)` |

El payload adicional `energyCardInstanceIdsToDiscard` es opcional y se usa cuando se deben descartar energías específicas para pagar el costo de retirada.
