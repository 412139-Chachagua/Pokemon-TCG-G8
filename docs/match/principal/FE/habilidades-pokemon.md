# Habilidades de Pokémon

## Vista General

Las habilidades son efectos especiales que poseen algunos Pokémon. Se dividen en dos categorías: **habilidades activables** (el jugador decide cuándo usarlas) y **habilidades pasivas** (efectos continuos que no requieren activación). El frontend maneja ambas mediante **PokemonSlotComponent** (visualización) y **MatchPage** (lógica de activación).

## Visualización en PokemonSlotComponent

`FE/src/app/features/match/components/pokemon-slot/pokemon-slot.component.ts`

### Habilidades Activables

Cuando un Pokémon tiene habilidades con `isActivable === true`, se muestra un botón con forma de rayo (`⚡`) en la esquina superior derecha de la carta:

```html
@if (canUseAbility()) {
    <div class="relative group z-20">
        <button
            class="absolute top-0.5 right-7 w-5 h-5 flex items-center justify-center text-xs bg-purple-600/80 rounded-full text-white cursor-pointer hover:bg-purple-500"
            (click)="$event.stopPropagation(); onAbilityClick()"
            title="Usar habilidad"
        >
            <svg>...icono rayo...</svg>
        </button>
        <!-- Tooltip con nombre y texto de la habilidad -->
    </div>
}
```

**Condiciones para mostrar el botón (`canUseAbility`):**
```typescript
readonly canUseAbility = computed(() => {
    if (!this.isOwn()) return false;          // Solo Pokémon propios
    if (!this.matchState.isMyTurn()) return false; // Solo durante el turno del jugador
    if (this.matchState.currentPhase() !== 'MAIN') return false; // Solo en fase MAIN
    return this.usableAbilities().length > 0;  // Debe tener al menos una habilidad activable
});
```

### Habilidades Pasivas

Las habilidades pasivas (`isActivable === false`) se muestran con un icono de estrella (`✦`) y tooltip informativo:

```html
@if (hasPassiveAbility()) {
    <div class="relative group z-20">
        <span class="absolute top-0.5 right-0.5 w-4 h-4 flex items-center justify-center text-[0.5rem] bg-slate-600/80 rounded-full text-white">✦</span>
        <!-- Tooltip con nombre y texto -->
    </div>
}
```

### Tooltips de Habilidad

Ambos tipos de habilidad muestran un tooltip al hacer hover, con:
- Nombre de la habilidad (negrita)
- Texto de la habilidad (descripción del efecto)

El tooltip se posiciona debajo del icono y tiene un fondo negro semitransparente.

## Lógica de Activación en MatchPage

### Barra de Habilidad del Pokémon Activo

En `MatchPage`, el Pokémon activo también muestra información de habilidad en la barra de ataque (`activeAbilityInfo`):

```typescript
readonly activeAbilityInfo = computed(() => {
    const active = this.matchState.myActivePokemon();
    const cardDef = this.cardRepo.getFromCache(active.cardId);
    const ability = cardDef?.abilities?.[0];  // Primera habilidad
    const canUse = ability.isActivable
        && this.matchState.isMyTurn()
        && this.matchState.currentPhase() === 'MAIN'
        && !active.specialConditions?.includes('ASLEEP')
        && !active.specialConditions?.includes('PARALYZED');
    return { name, text, isActivable, canUse };
});
```

El botón de habilidad en la barra tiene un diseño especial con gradiente y clip-path.

### Flujo de Activación

Cuando el jugador hace clic en el botón de habilidad (desde `PokemonSlotComponent` o desde la barra de acción):

```typescript
protected onPokemonAbilityClicked(event: { instanceId: string }): void {
    const pokemon = me?.activePokemon?.instanceId === event.instanceId ? me.activePokemon : me?.bench.find(p => p?.instanceId === event.instanceId);
    const activable = cardDef?.abilities?.filter(a => a.isActivable) ?? [];
    if (activable.length === 0) return;
    this.startAbilityResolution(event.instanceId, activable[0].name, activable[0].text);
}
```

### Resolución de Habilidades Específicas

```typescript
private startAbilityResolution(pokemonInstanceId, abilityName, abilityText): void {
    this._pendingAbility.set({ pokemonInstanceId, abilityName });

    switch (abilityName) {
        case 'Mystical Fire':
        case 'Stance Change':
        case "Upside-Down Evolution":
            // Efecto inmediato, sin selección de objetivo adicional
            this.dispatcher.useAbility(matchId, myId, pokemonInstanceId, abilityName).subscribe();
            break;

        case 'Drive Off':
            // Seleccionar Pokémon de la banca del oponente para hacer switch
            this.interactionService.enterSelectTargetPokemon(-1, this.getOpponentBenchInstanceIds());
            break;

        case 'Water Shuriken':
            // Buscar energía Water en la mano, luego seleccionar objetivo
            this._pendingAbilityFlow.set({ step: 'select_target', ... });
            this.interactionService.enterSelectTargetPokemon(-1, this.opponentPokemonInstanceIds());
            break;

        case "Fairy Transfer":
            // Mover energía Fairy entre Pokémon propios
            // Fase 1: seleccionar Pokémon fuente
            // Fase 2: seleccionar Pokémon destino
            this._abilityMoveEnergyPending.set(true);
            break;
    }
}
```

### Flujo de Selección de Objetivo para Habilidades

Cuando el jugador selecciona un Pokémon objetivo para una habilidad:

1. **Drive Off**: Se envía `USE_ABILITY` con `targetPokemonInstanceId`.
2. **Water Shuriken**: Se envía `USE_ABILITY` con `energyCardInstanceId` y `targetPokemonInstanceId`.
3. **Fairy Transfer** (flujo de dos pasos):
   - Paso 1: `_abilityMoveEnergyPending` — el jugador selecciona un Pokémon propio que tenga energía Fairy.
   - Paso 2: `_abilityMoveBenchPending` — el jugador selecciona un Pokémon propio destino (distinto del fuente).
   - Al confirmar el destino, se envía `USE_ABILITY` con `sourceEnergyInstanceId` y `targetPokemonInstanceId`.

### Cancelación

Si el jugador presiona Escape o cancela, se llama a `onCancelAbility()` que limpia todas las señales relacionadas con habilidades.

## Game Event Formatter

Eventos relacionados con habilidades:
- `ABILITY_USED`: Mensaje personalizado según el payload:
  - Si tiene `bonus`: "Tu Pokémon hará +N de daño el próximo turno"
  - Si tiene `preventEffects`: "El Pokémon se protegió: no recibirá daño el próximo turno"
  - Si tiene `targetPlayerId` (sin `abilityName`): "¡No podés jugar Partidarios el próximo turno!"
  - Si tiene `pokemonInstanceId` (sin `abilityName`): "El Pokémon no puede atacar el próximo turno"
  - Si tiene `abilityName`: "Usaste [habilidad]" / "El oponente usó [habilidad]"
- `ABILITY_BLOCKED`: "Habilidad bloqueada: [nombre]"

## Comunicación con el Backend

| Acción | Método |
|--------|--------|
| Usar habilidad | `useAbility(matchId, playerId, pokemonInstanceId, abilityName, extraPayload?)` |
