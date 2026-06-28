## ADDED Requirements

### Requirement: Sistema de resolución de efectos de Entrenador

El sistema SHALL interpretar `TrainerCardDefinition.effectCode` y ejecutar la lógica correspondiente mediante resolvers implementados con patrón Strategy.

- `TrainerEffectRegistry` SHALL mapear `effectCode: String` → `EffectType: Enum` y `EffectType` → `TrainerEffectResolver`
- `PlayTrainerHandler` SHALL delegar en `TrainerEffectRegistry.resolve()` en lugar de solo mover al descarte
- Si el `effectCode` no está registrado, SHALL registrar un error en el contexto y rechazar la acción
- SHALL publicar evento `TRAINER_EFFECT_RESOLVED` al completar exitosamente

#### Scenario: Carta de Entrenador con effectCode conocido ejecuta efecto
- **WHEN** un jugador juega una carta de Entrenador cuyo `effectCode` está registrado en `TrainerEffectRegistry`
- **THEN** el resolver correspondiente ejecuta el efecto
- **AND** la carta se mueve de la mano al descarte
- **AND** se publica evento `TRAINER_EFFECT_RESOLVED`

#### Scenario: Carta de Entrenador con effectCode desconocido es rechazada
- **WHEN** un jugador juega una carta de Entrenador cuyo `effectCode` no está registrado
- **THEN** la acción es rechazada con error `UNKNOWN_EFFECT_CODE`
- **AND** la carta permanece en la mano

#### Scenario: Límite de Partidario por turno se respeta
- **WHEN** un jugador juega un Partidario con efecto
- **THEN** el flag `hasPlayedSupporter` se marca como en la implementación actual

### Requirement: Efecto DRAW_CARDS

El resolver para `DRAW_CARDS` SHALL robar N cartas del mazo del jugador a su mano, donde N está definido por el `effectCode`.

- `EffectType.DRAW_CARDS` SHALL leer el parámetro `count` (int) desde el payload o desde una configuración asociada al `effectCode`
- SHALL publicar evento `CARDS_DRAWN` con la cantidad de cartas robadas
- Si el mazo tiene menos cartas que `count`, SHALL robar las que queden (sin error)

#### Scenario: DRAW_CARDS roba N cartas
- **WHEN** se ejecuta un efecto `DRAW_CARDS` con `count: 7`
- **THEN** se roban 7 cartas del mazo a la mano
- **AND** se publica evento `CARDS_DRAWN` con `count: 7`

#### Scenario: DRAW_CARDS con mazo insuficiente
- **WHEN** se ejecuta un efecto `DRAW_CARDS` pero el mazo tiene menos cartas que `count`
- **THEN** se roban todas las cartas restantes del mazo
- **AND** no se produce error ni derrota (la regla de derrota por mazo vacío solo aplica al robo inicial del turno)

### Requirement: Efecto HEAL

El resolver para `HEAL` SHALL remover N contadores de daño de un Pokémon objetivo.

- SHALL recibir `targetPokemonInstanceId: UUID` en el payload
- SHALL validar que el Pokémon objetivo existe en el campo del jugador
- SHALL no reducir los contadores de daño por debajo de 0
- SHALL publicar evento `POKEMON_HEALED`

#### Scenario: HEAL remueve contadores de daño
- **WHEN** se ejecuta un efecto `HEAL` sobre un Pokémon con 5 contadores de daño y `count: 3`
- **THEN** el Pokémon queda con 2 contadores de daño
- **AND** se publica evento `POKEMON_HEALED` con `countersRemoved: 3`

#### Scenario: HEAL no reduce por debajo de 0
- **WHEN** se ejecuta un efecto `HEAL` sobre un Pokémon con 1 contador de daño y `count: 3`
- **THEN** el Pokémon queda con 0 contadores de daño

### Requirement: Efecto SEARCH_BASIC_POKEMON

El resolver para `SEARCH_BASIC_POKEMON` SHALL buscar un Pokémon Básico en el mazo del jugador y colocarlo en la Banca.

- SHALL recibir `targetCardIndex: int` (índice en el mazo donde está el Pokémon a buscar) o `searchResultIndex: int` (índice de la carta seleccionada de una búsqueda)
- SHALL validar que la carta seleccionada es un Pokémon Básico
- SHALL validar que hay espacio en la Banca (menos de 5)
- SHALL publicar evento `POKEMON_SEARCHED`

#### Scenario: SEARCH_BASIC_POKEMON coloca en Banca
- **WHEN** se ejecuta un efecto `SEARCH_BASIC_POKEMON` y se selecciona un Pokémon Básico del mazo
- **THEN** el Pokémon se coloca en la Banca
- **AND** el mazo se baraja
- **AND** se publica evento `POKEMON_SEARCHED`

#### Scenario: SEARCH_BASIC_POKEMON con Banca llena
- **WHEN** se ejecuta un efecto `SEARCH_BASIC_POKEMON` pero la Banca tiene 5 Pokémon
- **THEN** la acción es rechazada con error `BENCH_FULL`

### Requirement: Efecto DISCARD_AND_DRAW

El resolver para `DISCARD_AND_DRAW` SHALL descartar N cartas de la mano y luego robar M cartas del mazo.

- SHALL leer `discardCount` y `drawCount` desde el payload o configuración del `effectCode`
- SHALL descartar primero, luego robar

#### Scenario: DISCARD_AND_DRAW descarta y roba
- **WHEN** se ejecuta un efecto `DISCARD_AND_DRAW` con `discardCount: 1` y `drawCount: 3`
- **THEN** se descarta 1 carta de la mano
- **AND** se roban 3 cartas del mazo a la mano

### Requirement: Efecto SWITCH_POKEMON

El resolver para `SWITCH_POKEMON` SHALL cambiar el Pokémon Activo por uno de la Banca.

- SHALL recibir `targetPokemonInstanceId: UUID` del Pokémon en Banca a pasar a Activo
- SHALL validar que el Pokémon objetivo está en la Banca
- SHALL validar que la Banca no está vacía
- SHALL limpiar condiciones especiales solo si la regla de la carta lo indica (en general Switch las mantiene, pero algunos efectos las limpian)

#### Scenario: SWITCH_POKEMON cambia Activo por Banca
- **WHEN** se ejecuta un efecto `SWITCH_POKEMON` con un Pokémon de la Banca como target
- **THEN** ese Pokémon pasa a ser Activo
- **AND** el anterior Activo pasa a la Banca
- **AND** se publica evento de cambio

### Requirement: Efecto SHUFFLE_HAND_INTO_DECK

El resolver para `SHUFFLE_HAND_INTO_DECK` SHALL poner todas las cartas de la mano en el mazo, barajar, y luego robar N cartas.

- SHALL recibir `drawCount: int` del payload o configuración del `effectCode`

#### Scenario: SHUFFLE_HAND_INTO_DECK vacía la mano y roba
- **WHEN** se ejecuta un efecto `SHUFFLE_HAND_INTO_DECK` con `drawCount: 7`
- **THEN** todas las cartas de la mano se ponen en el mazo
- **AND** el mazo se baraja
- **AND** se roban 7 cartas

### Requirement: Efecto DAMAGE_MODIFY

El resolver para `DAMAGE_MODIFY` SHALL modificar el daño que hace o recibe un Pokémon hasta el final del turno.

- SHALL recibir `targetPokemonInstanceId: UUID` y `modifierValue: int`
- SHALL almacenar el modificador en un mapa temporal en `GameState` o `TurnFlags` para ser consultado durante el cálculo de daño en `AttackResolver`

#### Scenario: DAMAGE_MODIFY se consulta en el ataque
- **WHEN** un Pokémon tiene un modificador `DAMAGE_MODIFY` activo y ataca
- **THEN** el modificador se aplica al calcular el daño en `AttackResolver`

### Requirement: Efecto CONDITION_REMOVE

El resolver para `CONDITION_REMOVE` SHALL remover una o todas las Condiciones Especiales de un Pokémon objetivo.

- SHALL recibir `targetPokemonInstanceId: UUID`
- Si el `effectCode` especifica una condición concreta, SHALL remover solo esa
- Si el `effectCode` es de curación total, SHALL remover todas

#### Scenario: CONDITION_REMOVE quita condición
- **WHEN** se ejecuta un efecto `CONDITION_REMOVE` sobre un Pokémon Quemado
- **THEN** la condición Quemado se elimina del Pokémon

### Requirement: Efecto REVIVE

El resolver para `REVIVE` SHALL tomar un Pokémon Básico de la pila de descartes y colocarlo en la Banca.

- SHALL recibir `targetCardIndex: int` (índice en la pila de descartes)
- SHALL validar que la carta seleccionada es un Pokémon Básico
- SHALL validar que hay espacio en la Banca

#### Scenario: REVIVE recupera de descartes
- **WHEN** se ejecuta un efecto `REVIVE` seleccionando un Pokémon Básico de la pila de descartes
- **THEN** el Pokémon se coloca en la Banca
- **AND** se remueve de la pila de descartes
