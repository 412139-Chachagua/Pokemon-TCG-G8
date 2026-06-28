# Habilidades de Pokémon en el TCG

## ¿Qué son las habilidades?

En el juego de cartas Pokémon (Pokémon TCG), algunos Pokémon tienen **habilidades especiales** que les dan efectos únicos. No son ataques, así que se pueden usar junto con atacar en el mismo turno. Pueden ser usadas tanto por el Pokémon Activo como por los Pokémon en la Banca.

Hay dos tipos de habilidades:

- **Habilidades activas**: el jugador elige cuándo usarlas, normalmente una vez por turno. Ejemplos: Lanzallamas Místico (Delphox), Shuriken de Agua (Greninja), Impulsar (Swellow).
- **Habilidades pasivas**: están siempre activas mientras el Pokémon está en juego. El jugador no hace nada para activarlas. Ejemplos: Escudo Espinoso (Chesnaught), Pelaje Recio (Furfrou), Velo Dulce (Slurpuff).

---

## Habilidades del set XY1 (146 cartas)

En la expansión **XY1 (XY Unlimited)** hay **11 habilidades únicas** distribuidas en varias cartas:

| Habilidad | Pokémon | Tipo | Efecto |
|---|---|---|---|
| **Impulsar** (*Drive Off*) | Swellow | Activa | Obliga al rival a cambiar su Pokémon Activo por uno de su Banca. |
| **Estallido Destino** (*Destiny Burst*) | Voltorb | Pasiva | Si este Pokémon es noqueado siendo Activo, lanzá una moneda. Si sale cara, poné 5 contadores de daño en el Pokémon atacante. |
| **Escudo Espinoso** (*Spiky Shield*) | Chesnaught | Pasiva | Si este Pokémon recibe daño siendo Activo, poné 3 contadores de daño en el Pokémon atacante (incluso si este Pokémon queda noqueado). |
| **Shuriken de Agua** (*Water Shuriken*) | Greninja | Activa | Una vez por turno, podés descartar una Energía Agua de tu mano. Si lo hacés, poné 3 contadores de daño en 1 Pokémon del rival. |
| **Llamas Místicas** (*Mystical Fire*) | Delphox | Activa | Una vez por turno, robá cartas hasta tener 6 cartas en tu mano. |
| **Evolución Invertida** (*Upside-Down Evolution*) | Inkay | Activa | Una vez por turno, si este Pokémon está Confundido, podés buscar en tu mazo una carta que evolucione de este Pokémon y ponerla sobre él (cuenta como evolución). |
| **Transferencia Hada** (*Fairy Transfer*) | Aromatisse | Activa | Todas las veces que quieras por turno, podés mover una Energía Hada de un Pokémon tuyo a otro. |
| **Pelaje Recio** (*Fur Coat*) | Furfrou | Pasiva | Cualquier daño recibido por este Pokémon se reduce en 20 (después de aplicar Debilidad y Resistencia). |
| **Maldición del Bosque** (*Forest's Curse*) | Trevenant | Pasiva | Mientras este Pokémon sea tu Pokémon Activo, el rival no puede jugar cartas de Objeto de su mano. |
| **Cambio de Postura** (*Stance Change*) | Aegislash (x2) | Activa | Una vez por turno, podés cambiar este Pokémon por un Aegislash de tu mano (las cartas unidas, contadores de daño y condiciones se pierden). |
| **Velo Dulce** (*Sweet Veil*) | Slurpuff | Pasiva | Cada uno de tus Pokémon que tenga Energía Hada no puede ser afectado por Condiciones Especiales. |

---

## Cómo se implementó en el código

### Arquitectura general

El sistema de habilidades se divide en dos grandes grupos:

1. **Resolvers** (habilidades activas): clases que implementan `AbilityResolver` y están registradas en `AbilityRegistry`. Cuando el jugador hace clic en el botón ⚡, el frontend envía una acción `USE_ABILITY` al backend, que busca el resolver correspondiente y lo ejecuta.

2. **Hooks** (habilidades pasivas): clases con métodos estáticos que se llaman desde puntos específicos del motor de juego (al calcular daño, al noquear, al terminar el turno, etc.). No necesitan que el jugador las active.

### Las 6 habilidades activas (con resolver)

Están registradas en `GameEngineConfig.java`:

| Habilidad | Resolver | Qué espera del frontend |
|---|---|---|
| Llamas Místicas | `MysticalFireResolver` | Solo `pokemonInstanceId` + `abilityName` |
| Shuriken de Agua | `WaterShurikenResolver` | `pokemonInstanceId` + `abilityName` + `energyCardInstanceId` + `targetPokemonInstanceId` |
| Transferencia Hada | `FairyTransferResolver` | `pokemonInstanceId` + `abilityName` + `sourceEnergyInstanceId` + `targetPokemonInstanceId` |
| Impulsar | `DriveOffResolver` | `pokemonInstanceId` + `abilityName` + `targetPokemonInstanceId` |
| Cambio de Postura | `StanceChangeResolver` | Solo `pokemonInstanceId` + `abilityName` |
| Evolución Invertida | `UpsideDownEvolutionResolver` | Solo `pokemonInstanceId` + `abilityName` |

### Las 5 habilidades pasivas (hook)

Se llaman automáticamente desde el motor:

| Habilidad | Dónde se llama | Efecto |
|---|---|---|
| Escudo Espinoso | `PostDamageEffectStep.java` | Después de aplicar daño, si el defensor tiene esta habilidad, pone 3 contadores en el atacante |
| Estallido Destino | `KnockoutCheckStep.java` | Cuando un Pokémon es noqueado, si tiene esta habilidad, lanza moneda y posiblemente pone 5 contadores |
| Pelaje Recio | `DamageCalculator.java` | Reduce el daño recibido en 20 |
| Maldición del Bosque | `RuleValidator.java` | Bloquea cartas de Objeto si el Pokémon activo rival tiene esta habilidad |
| Velo Dulce | `ApplyConditionEffect.java` y `StatusEffectManager.java` | Impide que se apliquen condiciones especiales si el Pokémon tiene Energía Hada y hay un Slurpuff en juego |

---

### Velo Dulce: protección en tiempo real

Velo Dulce es la habilidad más compleja porque es condicional: un Pokémon solo está protegido si:

1. **Ese Pokémon tiene al menos una Energía Hada** unida.
2. **Algún Pokémon del jugador** (Activo o Banca) tiene la habilidad Velo Dulce (Slurpuff).

Cuando se cumplen ambas condiciones, el Pokémon no puede recibir Condiciones Especiales (Dormido, Quemado, Confundido, Paralizado, Envenenado). Y si ya tenía condiciones, se eliminan automáticamente.

Agregamos un método `syncImmunity()` que se ejecuta después de:

- **Evolucionar un Pokémon** (por carta de Entrenador o por habilidad)
- **Unir una Energía** a un Pokémon
- **Poner un Pokémon en Banca**

Esto asegura que si Slurpuff entra al campo o se une Energía Hada a un Pokémon envenenado, la condición se cura inmediatamente, no al final del turno.

---

### Supresión de habilidades

Algunos ataques tienen un efecto que **suprime las habilidades del Pokémon rival** por un turno. El backend ya tenía el flag `abilitiesSuppressedNextTurn` en `PokemonInPlay` y el `AbilitySuppressionEffect` que lo seteaba, pero **nunca se verificaba**.

Se agregó el chequeo en dos lugares:

1. `UseAbilityHandler.java` — al ejecutar una habilidad activa, si el Pokémon tiene habilidades suprimidas, se rechaza la acción.
2. `RuleValidator.java` — en la validación previa, mismo chequeo.

Además, se agregó el reseteo de este flag al inicio del turno (en `TurnManager.java`), para ambos jugadores, igual que se resetean los otros flags como `preventAllDamageNextTurn`.

---

### Cómo saber si una habilidad es "usable" desde el frontend

El backend ahora envía un campo extra en la respuesta de la API de cartas: **`isActivable`**. Cuando el frontend pide el detalle de una carta, cada habilidad viene con:

```json
{
  "name": "Shuriken de Agua",
  "text": "Una vez durante tu turno...",
  "type": "ABILITY",
  "isActivable": true
}
```

- `isActivable: true` → la habilidad tiene un resolver registrado. El frontend muestra el botón ⚡.
- `isActivable: false` → es pasiva. Solo se muestra un badge ✦ con tooltip informativo.

Esto evita que aparezca el botón ⚡ en habilidades como Pelaje Recio o Escudo Espinoso, que no se pueden activar manualmente.

---

### Cómo se ve en el frontend

#### Habilidades activas (botón ⚡)

En cada Pokémon del jugador que tenga habilidades activas aparece un botón **⚡** en la esquina superior derecha de la carta. Al pasar el mouse, un tooltip muestra el nombre y texto de la habilidad.

Al hacer clic en ⚡:

1. **Sin selección** (Llamas Místicas, Cambio de Postura, Evolución Invertida): la habilidad se ejecuta inmediatamente.
2. **Con selección de Pokémon rival** (Impulsar): se activa el modo de selección, se resaltan los Pokémon de la Banca rival, y al hacer clic en uno se ejecuta.
3. **Con selección de energía + Pokémon** (Shuriken de Agua, Transferencia Hada): primero se elige la energía (de la mano o equipada), luego el Pokémon destino, y se ejecuta.

#### Habilidades pasivas (badge ✦)

En los Pokémon con habilidades pasivas aparece un badge **✦** con un tooltip que explica el efecto. No se puede hacer clic.

---

### Resumen de archivos modificados

#### Backend (Java)

| Archivo | Ruta | Cambio |
|---|---|---|
| `CardAbilityResponse.java` | `NUEVO: dtos/cards/` | DTO con `isActivable` para la respuesta al frontend |
| `CardDetailResponse.java` | `dtos/cards/` | Cambió `List<AbilityDto>` por `List<CardAbilityResponse>` |
| `CardMapper.java` | `mappers/cards/` | Inyecta `AbilityRegistry`, mapea `isActivable` |
| `SweetVeilHook.java` | `engine/ability/hooks/` | Agregado `syncImmunity()`, refactor de helpers |
| `ApplyConditionEffect.java` | `engine/attack/effects/` | Check Sweet Veil antes de aplicar condición |
| `StatusEffectManager.java` | `engine/attack/` | Sweet Veil en `processBetweenTurnStatuses()` |
| `EvolvePokemonHandler.java` | `engine/handlers/` | `syncImmunity()` post-evolución manual |
| `EvolveDirectResolver.java` | `engine/trainer/resolvers/` | `syncImmunity()` post-evolución con carta |
| `UpsideDownEvolutionResolver.java` | `engine/ability/resolvers/` | `syncImmunity()` post-evolución con habilidad |
| `AttachEnergyHandler.java` | `engine/handlers/` | `syncImmunity()` post-unir energía |
| `UseAbilityHandler.java` | `engine/handlers/` | Check `abilitiesSuppressedNextTurn` |
| `RuleValidator.java` | `engine/rules/` | Check `abilitiesSuppressedNextTurn` |
| `TurnManager.java` | `engine/turn/` | Reset `abilitiesSuppressedNextTurn` para ambos jugadores |

#### Frontend (TypeScript/Angular)

| Archivo | Ruta | Cambio |
|---|---|---|
| `card.models.ts` | `shared/models/` | Agregado `CardAbilityResponse` + campo en `CardDetailResponse` |
| `pokemon-slot.component.ts` | `match/components/pokemon-slot/` | Botón ⚡ + badge ✦ + tooltips con texto |
| `bench-zone.component.ts` | `match/components/bench-zone/` | Re-emite `abilityClicked` |
| `player-area.component.ts` | `match/components/player-area/` | Re-emite `abilityClicked` |
| `match-page.ts` | `match/pages/match-page/` | Handler, señales, lógica por habilidad |
| `game-action-dispatcher.service.ts` | `match/services/` | Helper `useAbility()` |

---

## Flujo de una habilidad activa paso a paso

1. El backend envía el detalle de la carta con `abilities[].isActivable` en `true`.
2. El `PokemonSlotComponent` detecta que tiene habilidades activables y muestra el botón ⚡.
3. El jugador hace clic en ⚡.
4. El evento llega a `match-page.ts` → `onPokemonAbilityClicked()`.
5. Dependiendo de la habilidad:
   - **If sin target**: llama `dispatcher.useAbility()` directo.
   - **If con target**: activa `MatchInteractionService.enterSelectTargetPokemon()` con los targets válidos.
6. El jugador selecciona el target haciendo clic.
7. `onPokemonClicked()` detecta que hay una habilidad pendiente y llama a `onAbilityTargetSelected()`.
8. Se envía la acción `USE_ABILITY` al backend con `pokemonInstanceId`, `abilityName` y los parámetros extra.
9. El `UseAbilityHandler` valida y ejecuta el resolver.
10. Se emite un evento `ABILITY_USED` que el frontend muestra en el log.
