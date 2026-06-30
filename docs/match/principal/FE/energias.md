# Energías

## Vista General

Las cartas de Energía se adjuntan a los Pokémon para permitirles atacar y retirarse. El manejo de energías en el frontend involucra la visualización en los slots de Pokémon, el pipe de iconos, la unión mediante drag & drop o clic, y un diálogo de selección de energías para descartar.

## EnergyIconPipe

`FE/src/app/shared/pipes/energy-icon.pipe.ts`

Pipe Angular que transforma un tipo de energía en la ruta de su icono SVG:

```typescript
transform(type: string): string {
    const upper = type?.toUpperCase() ?? '';
    if (ENERGY_TYPES.has(upper)) {
        return `assets/icons/energy/energy-${upper.toLowerCase()}.svg`;
    }
    // Fallback: buscar en la definición de carta
    const def = this.cardRepo.getFromCache(type);
    if (def?.types?.length) {
        return `assets/icons/energy/energy-${def.types[0].toLowerCase()}.svg`;
    }
    return 'assets/icons/energy/energy-colorless.svg';
}
```

Los tipos reconocidos son: GRASS, FIRE, WATER, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, METAL, FAIRY, COLORLESS.

## Visualización en PokemonSlotComponent

`FE/src/app/features/match/components/pokemon-slot/pokemon-slot.component.ts`

### Slot de Banca (vista compacta)

Las energías se muestran como círculos de colores agrupados por tipo, con contador numérico si hay más de una:

```html
@for (group of groupedEnergies(); track group.type) {
    <div class="group/energy relative">
        <div class="w-3.5 h-3.5 rounded-full"
             [style.background-color]="group.color">
            @if (group.count > 1) {
                <span>...</span>
            }
        </div>
        <!-- Tooltip: "FIRE x2" -->
    </div>
}
```

Los colores se definen en `ENERGY_COLORS`:
```typescript
const ENERGY_COLORS: Record<string, string> = {
    FIRE: '#ef4444', WATER: '#3b82f6', GRASS: '#4ade80',
    LIGHTNING: '#facc15', PSYCHIC: '#a855f7', FIGHTING: '#f97316',
    DARKNESS: '#6b21a8', METAL: '#9ca3af', FAIRY: '#f472b6',
    DRAGON: '#f59e0b', COLORLESS: '#e5e7eb',
};
```

### Slot Activo (vista expandida)

Las energías se muestran como cuadrados de 48×48px con el icono SVG correspondiente, cada una clickeable individualmente:

```html
@for (energy of pokemon().attachedCards; track $index) {
    <div class="group/energy">
        <div class="w-12 h-12 rounded-md border ... cursor-pointer"
             (click)="onEnergyClick($index)">
            <img [src]="energy | energyIcon" ... />
        </div>
    </div>
}
```

### groupedEnergies (señal computada)

Agrupa las energías por tipo para la vista compacta:

```typescript
readonly groupedEnergies = computed(() => {
    const energies = this.pokemon().attachedCards;
    const groups = new Map<string, number>();
    for (const energy of energies) {
        groups.set(key, (groups.get(key) ?? 0) + 1);
    }
    // Retorna array de { type, color, count }
});
```

## Unión de Energía

### Desde el Clic en la Mano

Cuando el jugador hace clic en una carta de Energía en `HandZoneComponent`, `MatchPage.onHandCardClicked()` detecta `supertype === 'ENERGY'` y:

1. Verifica que la partida esté en estado `ACTIVE`, fase `MAIN`, y sea el turno del jugador.
2. Obtiene todos los Pokémon del jugador como objetivos válidos (`allPokemonInstanceIds`).
3. Activa el modo de selección `SELECT_TARGET_POKEMON` mediante `enterSelectTargetPokemon`.
4. Cuando el jugador hace clic en un Pokémon válido, se llama a `dispatcher.attachEnergy()`.

### Desde Drag & Drop

En `MatchPage.onEnergyDropped()` (también manejado en `PlayerAreaComponent.onActiveDrop()`):

1. Se verifica estado y turno.
2. Se elimina la carta de la mano (`optimisticallyRemoveCardFromHand`).
3. Se envía `ATTACH_ENERGY` al backend.

### canAttachEnergy

La señal `canAttachEnergy` en `MatchStateService` controla si el jugador puede unir energía este turno:

- Solo en fase `MAIN`, durante el turno del jugador.
- Solo una vez por turno: `hasAttachedEnergy` debe ser `false`.

### HandZone - Filtro "Energías"

La zona de mano tiene un filtro que permite mostrar solo cartas de tipo `ENERGY` mediante el botón "Energías" en `activeFilter`.

## EnergySelectorDialogComponent

`FE/src/app/features/match/components/energy-selector-dialog/energy-selector-dialog.component.ts`

Diálogo modal para seleccionar energías a descartar de un Pokémon rival (cuando un ataque o entrenador lo requiere).

**Inputs:**
- `pokemonInstanceId: string` — Pokémon objetivo.
- `energies: EnergyOption[]` — Lista de energías disponibles (`{ instanceId, cardId }`).
- `count: number` — Cantidad de energías a seleccionar.

**Outputs:**
- `confirmed: string[]` — Array de `instanceId` seleccionados.
- `cancelled: void` — Cancelación.

**Funcionamiento:**

1. Muestra cada energía como una imagen checkboxeable.
2. El usuario debe seleccionar exactamente `count` energías.
3. El botón "Confirmar" se habilita solo cuando `selectedIds().size === count()`.
4. Las casillas se deshabilitan cuando ya se alcanzó el límite de selección.

## Efecto Flash al Unir Energía

Cuando el backend notifica `ENERGY_ATTACHED`, `MatchStateService.addEvent()`:

1. Reproduce el sonido de unión de energía.
2. Resuelve la definición de la carta de energía.
3. Agrega un flash de color a `_energyAttachFlashes` para el Pokémon objetivo.
4. El flash se muestra en `PokemonSlotComponent` como una capa overlay con el color de la energía:
   ```html
   @if (energyFlash(); as type) {
       <div class="absolute inset-0 rounded-lg pointer-events-none z-10 animate-energy-flash"
            [style.background-color]="ENERGY_COLORS[type] ?? '#d1d5db'"></div>
   }
   ```
5. El flash desaparece después de 1000ms.

## Game Event Formatter

Eventos relacionados con energía:
- `ENERGY_ATTACHED`: "Uniste Energía a un Pokémon (ahora N energía(s))"
- `ENERGY_DISCARDED`: "Descartaste energía(s) propia(s)" / "Descartaste energía(s) del defensor"

## MatchStateService - activePokemonEnergyTypes

Calcula los tipos de energía adjuntos al Pokémon activo, resolviendo tanto tipos conocidos como definiciones de cartas (para energías especiales como "Double Colorless Energy").

## HandZone - energyBorder

Cada carta de energía en la mano tiene un borde de color según su tipo, calculado en `energyBorder()`.
