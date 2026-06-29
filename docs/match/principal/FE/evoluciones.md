# Evoluciones

## Vista General

La evolución de Pokémon se dispara desde la mano del jugador cuando este posee una carta de evolución (stage > BASIC) que coincida con el nombre `evolvesFrom` de un Pokémon en el campo. Hay dos mecanismos para iniciar una evolución: clic en la carta desde **HandZoneComponent** o arrastrar la carta directamente sobre un Pokémon en el campo mediante drag & drop.

## HandZoneComponent como Origen

`FE/src/app/features/match/components/hand-zone/hand-zone.component.ts`

Cuando el jugador hace clic en una carta de la mano, **MatchPage** recibe el evento y procesa la lógica en `onHandCardClicked()`.

### Detección de Carta de Evolución

```typescript
if (supertype === 'POKEMON') {
    const cardDef = this.cardRepo.getFromCache(card.cardId);
    if (cardDef?.stage && cardDef.stage !== 'BASIC') {
        // Es una evolución → buscar objetivos elegibles
        const eligibleIds = this.getEligiblePokemonInstanceIds(handIndex);
```

### getEligiblePokemonInstanceIds

Este método (línea 1278 de MatchPage) determina qué Pokémon en el campo pueden evolucionar con la carta seleccionada:

1. Obtiene la definición de la carta en la mano y verifica que tenga `evolvesFrom` y `stage`.
2. **No se permite evolución en el primer turno** del jugador: verifica `firstTurnCompleted`.
3. Para cada Pokémon propio (activo y banca):
   - Compara el nombre del Pokémon en campo con `cardDef.evolvesFrom`.
   - Verifica que ese Pokémon **no haya evolucionado este turno** (`evolvedThisTurn`).
   - Verifica que el Pokémon **no haya entrado al campo este turno** (`enteredTurnNumber !== turnNumber`).

### Inicio de Selección

Si hay objetivos elegibles, se llama a:
```typescript
this.interactionService.enterSelectTargetPokemon(handIndex, eligibleIds);
```

Esto activa el modo de selección `SELECT_TARGET_POKEMON`, que resalta los Pokémon válidos en el campo (`isHighlighted` en `PokemonSlotComponent`). Si no hay objetivos elegibles, se muestra un toast informativo.

### Sin Objetivos Elegibles

Si no hay objetivos, se muestran mensajes según el caso:
- Primer turno: "No podés evolucionar en tu primer turno"
- Pokémon ya evolucionó este turno: "Este Pokémon no puede evolucionar este turno"
- Sin Pokémon con el nombre base: "No hay Pokémon disponibles para evolucionar"

## Drag & Drop (Evolución desde la Mano)

En `MatchPage.onEvolutionDropped()`:

1. Se verifica que la partida esté en estado `ACTIVE`, fase `MAIN`, y que sea el turno del jugador.
2. Se obtiene la carta de la mano según `handIndex`.
3. Se verifica que la carta sea `POKEMON` y tenga `stage` distinto de `BASIC`.
4. Se verifica que el jugador ya completó su primer turno.
5. Se busca el Pokémon objetivo (activo o banca) por `targetInstanceId`.
6. Se compara el nombre del Pokémon objetivo con `cardDef.evolvesFrom`.
7. Se verifica que no haya evolucionado este turno y que no haya entrado este turno.
8. Si todo está bien: se elimina la carta de la mano (`optimisticallyRemoveCardFromHand`), se reproduce el sonido de evolución, y se envía la acción `EVOLVE_POKEMON` al backend.

## Feedback Visual (PokemonSlotComponent)

Cuando el backend confirma la evolución y actualiza el estado, el `PublicPokemonSlotModel` recibe `evolvedThisTurn: true`. Esto activa en `PokemonSlotComponent`:

```html
@if (pokemon().evolvedThisTurn === true) {
    <div class="absolute inset-0 rounded-lg pointer-events-none z-[6] bg-violet-600 animate-evo-flash"></div>
    @for (pos of EVO_PARTICLE_POSITIONS; track $index) {
        <div class="absolute z-[5] pointer-events-none w-2 h-2 rounded-full animate-evo-particle"
             [class.bg-violet-400]="isOwn()"
             [class.bg-fuchsia-400]="!isOwn()"
             ...
```

- **Evo flash**: Un destello violeta que cubre toda la carta.
- **Partículas**: 46 partículas violeta/fucsia animadas (`animate-evo-particle`) en posiciones predefinidas con delay escalonado.
- **Badge "EVO"**: Un badge en la esquina superior derecha con el texto "EVO".
- **Borde violeta**: El borde del slot cambia a `violet-700` (`evolvedThisTurn === true`).

## Game Event Formatter

El evento `POKEMON_EVOLVED` se formatea como:
- "Evolucionaste a [nombre]" / "El oponente evolucionó a [nombre]"
- Incluye "(desde [nombre base])" si está disponible.

## Restricciones

| Regla | Dónde se valida |
|-------|-----------------|
| No evolucionar en el primer turno | `MatchPage.getEligiblePokemonInstanceIds()` y `onEvolutionDropped()` |
| No evolucionar dos veces al mismo Pokémon | `evolvedThisTurn === true` |
| No evolucionar Pokémon que entraron este turno | `enteredTurnNumber !== turnNumber` |
| Evolución específica: carta debe tener `evolvesFrom` que coincida | `cardDef.evolvesFrom === targetDef.name` |

## Comunicación con el Backend

| Acción | Método en `GameActionDispatcherService` |
|--------|----------------------------------------|
| Evolucionar | `evolvePokemon(matchId, playerId, handIndex, targetPokemonInstanceId)` |
