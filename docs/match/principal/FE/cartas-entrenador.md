# Cartas de Entrenador

## Vista General

Las cartas de Entrenador se juegan desde la mano durante la fase `MAIN` del turno del jugador. Pueden tener distintos efectos según su subtipo: objetos (ITEM), partidarios (SUPPORTER), estadios (STADIUM), herramientas (POKEMON_TOOL), y cartas ACE SPEC.

## HandZoneComponent

`FE/src/app/features/match/components/hand-zone/hand-zone.component.ts`

### Filtro por Tipo

La mano tiene botones de filtro: "Todas", "Pokémon", "Entrenadores", "Energías". Al seleccionar "Entrenadores", solo se muestran las cartas de supertype `TRAINER`.

### Badge de Subtipo

Cada carta de entrenador muestra un badge con su subtipo:

| Subtipo | Badge | Color de fondo |
|---------|-------|----------------|
| ITEM | "ITEM" | Azul (`#3B82F6`) |
| SUPPORTER | "SUPPORTER" | Ámbar (`#F59E0B`) |
| STADIUM | "STADIUM" | Verde (`#10B981`) |
| POKEMON_TOOL | "POKEMON TOOL" | Púrpura (`#8B5CF6`) |
| ACE_SPEC | "ACE SPEC" | Según el primero encontrado |

### Restricciones Visuales

- **Partidario ya jugado**: Si `hasPlayedSupporter` es `true`, las cartas SUPPORTER aparecen atenuadas y no interactuables (`isSupporterDisabled`).
- **Estadio ya jugado**: Similar para `hasPlayedStadium` con cartas STADIUM.
- **Energía ya unida**: Si `canAttachEnergy` es `false`, las cartas ENERGY se atenúan.

## Flujo de Juego de Entrenador

### Clic en Carta de Entrenador

En `MatchPage.onHandCardClicked()`, cuando `supertype === 'TRAINER'`:

1. **Herramientas Pokémon (ATTACH_TOOL)**: Si la carta tiene `effectCode === 'ATTACH_TOOL'` o subtipo `POKEMON_TOOL`, se activa el modo de selección de objetivo. Los objetivos son los Pokémon que **no** tienen herramienta equipada (`pokemonWithoutToolInstanceIds`). Se guarda `_pendingTrainerPlay`.

2. **Super Potion (HEAL_60_DISCARD_1)**: Muestra un toast indicando que se arrastre la carta al Pokémon objetivo. Requiere seleccionar una energía para descartar adicionalmente.

3. **Cassius**: Verifica que haya al menos un Pokémon en banca. Activa selección de objetivo con sub-flujo para elegir nuevo activo.

4. **Professor Sycamore / Shauna (DISCARD_HAND_DRAW_7 / SHUFFLE_DRAW_5)**: Muestra un modal de confirmación (`_pendingTrainerConfirm`) preguntando "¿Querés aplicar [nombre]?", con una descripción del efecto.

5. **Cartas con efecto por nombre**: `getTrainerEffectCode()` mapea nombres conocidos a códigos de efecto:
   - `'Evosoda'` → `'EVOSODA'`
   - `'Great Ball'` → `'GREAT_BALL'`
   - `'Max Revive'` → `'MAX_REVIVE'`
   - `"Professor's Letter"` → `'PROFESSORS_LETTER'`
   - `'Red Card'` → `'RED_CARD'`
   - `'Roller Skates'` → `'COIN_FLIP_DRAW_3'`
   - `'Super Potion'` → `'HEAL_60_DISCARD_1'`
   - `'Cassius'` → `'CASSIUS'`
   - `'Professor Sycamore'` → `'DISCARD_HAND_DRAW_7'`
   - `'Shauna'` → `'SHUFFLE_DRAW_5'`
   - `'Team Flare Grunt'` → `'TEAM_FLARE_GRUNT'`

6. **Evosoda**: Activa selección de objetivo. Verifica que el jugador no esté en su primer turno, que el Pokémon objetivo no haya evolucionado ni entrado este turno.

7. **Great Ball**: Abre un visor de mazo (`_showDeckViewer`) mostrando las primeras 7 cartas, modo selección única de Pokémon.

8. **Professor's Letter**: Abre visor de mazo con filtro de energía, modo selección múltiple (hasta 2).

9. **Max Revive**: Abre el visor de descarte para seleccionar un Pokémon de la pila de descarte.

10. **Team Flare Grunt**: Activa selección de objetivo sobre Pokémon del oponente.

11. **Otros**: Si no necesita selección de objetivo, envía la acción directamente.

### Arrastre de Entrenador

Cuando se arrastra una carta de entrenador sobre un Pokémon (en `PlayerAreaComponent.onActiveDrop()` o en `MatchPage.onTrainerDropped()`):

1. Se verifica estado `ACTIVE`, fase `MAIN`, turno del jugador.
2. Se determina si es herramienta (`ATTACH_TOOL` o `POKEMON_TOOL`) o Evosoda.
3. Para Evosoda se aplican las mismas restricciones que por clic.
4. Se elimina la carta de la mano y se envía la acción.

### Diálogo de Confirmación para Sycamore/Shauna

Se muestra un panel inferior (`_pendingTrainerConfirm`) que pregunta:
- Para Professor Sycamore: "Se descartará toda tu mano."
- Para Shauna: "Se mezclará tu mano en el mazo y robarás 5 cartas."
- Opciones "Sí" / "No".

## StadiumZoneComponent

`FE/src/app/features/match/components/stadium-zone/stadium-zone.component.ts`

Muestra la carta de estadio activa en el centro del tablero. El componente:

- Se renderiza en la posición central entre las áreas de ambos jugadores.
- Muestra la imagen de la carta de estadio.
- Al hacer hover, se escala la imagen (efecto zoom).
- El nombre se obtiene resolviendo `cardRepo.getFromCache(stadiumCardDefinitionId)`.
- Se puede hacer clic en el botón "+" para previsualizar la carta.

### Inputs
- `stadiumCardInstanceId: string | null`
- `stadiumCardDefinitionId: string | null`

### Efectos de Estadio en el Tablero

En `MatchStateService`, cuando se recibe `STADIUM_PLAYED`:
1. Se resuelve la definición de la carta.
2. Se actualiza `_stadiumBg` con la imagen de fondo correspondiente.
3. Cuando se remueve el estadio (`STADIUM_REMOVED`), se limpia `_stadiumBg`.

### Restricción Visual en HandZone

Las cartas de tipo `STADIUM` se atenúan si ya se jugó un estadio este turno (`hasPlayedStadium`).

## Flujo de Herramientas (ATTACH_TOOL)

1. El jugador hace clic en una carta con `effectCode === 'ATTACH_TOOL'`.
2. Se activa `SELECT_TARGET_POKEMON` con objetivos = Pokémon sin herramienta.
3. Al seleccionar un objetivo, se envía `ATTACH_TOOL` al backend.
4. El componente `PokemonSlotComponent` muestra la herramienta equipada con un icono y nombre.

### Visualización de Herramientas en PokemonSlotComponent

```html
@if (pokemon().attachedToolCardInstanceId) {
    <img src="assets/icons/tool.svg" alt="Tool" ... />
    <span>{{ toolCardDef()?.name }}</span>
}
```

La definición de la herramienta se resuelve mediante `toolCardDef`:
```typescript
readonly toolCardDef = computed(() => {
    const defId = this.pokemon().attachedToolCardDefinitionId;
    return defId ? this.cardRepo.getFromCache(defId) : null;
});
```

## Comunicación con el Backend

| Acción | Método |
|--------|--------|
| Jugar entrenador | `playTrainer(matchId, playerId, handIndex, extraPayload?)` |
| Adjuntar herramienta | `attachTool(matchId, playerId, handIndex, targetPokemonInstanceId)` |
