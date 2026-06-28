# AI Proposal Spec: persona2-setup-manager

## Change name

persona2-setup-manager

---

# Depends on

OpenCode MUST read and obey:

- `openspec/specs/engine-persona2-master-spec.md`
- `BE/docs/contracts_ai/07-setup-flow-contract.md`
- `BE/docs/contracts_ai/06-game-state-contract.md`
- `docs/Reglas_Pokemon_TCG.md`

Los contratos del proyecto son la fuente de verdad.
Si este spec entra en conflicto con los contratos, los contratos ganan.

---

# Purpose

Implementar la lógica real de `engine/setup/SetupManager`.

Este spec cubre únicamente:
- inicialización de decks como CardInstance
- shuffle inicial
- reparto de mano inicial
- resolución de mulligan
- selección automática de Active inicial
- asignación de prizes
- construcción del GameState inicial

---

# Existing ports — do not recreate

Los siguientes ports ya existen y NO deben ser recreados ni modificados:

- `engine/ports/CardLookupPort.java`
- `engine/ports/RandomizerPort.java`

---

# Ownership

Persona 2 owns exclusively:

- `engine/setup/SetupManager`

NO modificar ninguna clase fuera de este ownership.

---

# Non-goals

NO implementar:

- turn logic
- RuleValidator
- AttackResolver
- handlers
- combat
- damage
- victory resolution
- status conditions
- persistence
- REST endpoints
- DeckLoadPort
- DeckLoadAdapter

---

# Dependencies allowed

`SetupManager` puede depender únicamente de:

- `CardLookupPort`
- `RandomizerPort`
- `engine/model/*`
- `engine/action/*`
- enums del engine ya definidos por Persona 1

NO usar:
- repositorios
- services
- Spring
- JPA
- `java.util.Random`
- `ThreadLocalRandom`
- `Collections.shuffle`

Las dependencias se inyectan por constructor.

---

# Constructor

`SetupManager` recibe por constructor:

- `RandomizerPort`
- `CardLookupPort`

---

# CardInstance creation

Cada carta del deck de dominio (`DeckCard`) debe convertirse en
una `CardInstance` usando las convenciones definidas por Persona 1:

- `instanceId`: `UUID.randomUUID()`
- `cardDefinitionId`: valor de `DeckCard.getCardId()`

NO asumir constructores ni factories no existentes.
Usar únicamente la API pública de `CardInstance` ya definida por Persona 1.

---

# Shuffle flow

Todos los shuffles usan exclusivamente `RandomizerPort`.

NO usar `Collections.shuffle`, `Random` ni `ThreadLocalRandom`.

---

# Initial hand

Cada jugador recibe 7 cartas iniciales tomadas desde el tope del deck
ya mezclado.

---

# Basic Pokémon detection

Para detectar si una carta es Pokémon BASIC:

1. Llamar `CardLookupPort.getCardById(instance.getCardDefinitionId())`
2. Verificar que el resultado sea instancia de `PokemonCardDefinition`
3. Verificar que `stage` sea `"BASIC"`

Solo usar campos ya expuestos por la jerarquía `CardDefinition` existente.

---

# Mulligan rules

Si un jugador no tiene ningún Pokémon BASIC en mano:

1. Devolver las 7 cartas al deck.
2. Mezclar de nuevo via `RandomizerPort`.
3. Repartir nuevas 7 cartas.
4. Incrementar `mulliganCount` del jugador en `PlayerState`.
5. Repetir hasta que el jugador tenga al menos un Pokémon BASIC en mano.

Si ambos jugadores declaran mulligan, el proceso se resuelve de forma
independiente para cada uno.

Al finalizar el setup, cada jugador roba tantas cartas extra como
mulligans declaró el oponente. Estas cartas se agregan directamente
a la mano.

Invariante: al salir del mulligan, ambos jugadores tienen garantizado
al menos un Pokémon BASIC en mano. Si esto no se cumple, el setup
no puede continuar.

---

# Active Pokémon selection

Simplificación V1 oficial según contrato 07:

- Elegir automáticamente el primer `PokemonCardDefinition` con
  `stage == "BASIC"` encontrado en la mano de cada jugador.
- Luego mover automáticamente al Bench hasta 5 Pokémon BASIC adicionales encontrados en la mano.
- Mover esa carta de la mano al campo `activePokemon` del `PlayerState`.

NO implementar selección manual de Active inicial.
Esta simplificación es explícita y permanente para V1.

---

# Bench behavior

Después de seleccionar el Active inicial,
SetupManager debe colocar automáticamente en Bench
los Pokémon BASIC restantes encontrados en la mano.

Se agregan hasta un máximo de 5 Pokémon.

Cada Pokémon movido al Bench debe eliminarse de la mano.

Esta automatización forma parte de la simplificación MVP
definida por el contrato 07.

---

# Prize cards

Luego de completar el mulligan y seleccionar el Active:

- Tomar las primeras 6 cartas del deck restante de cada jugador.
- Asignarlas al campo `prizes` del `PlayerState`.

Invariante: ambos jugadores terminan con exactamente 6 prizes.

---

# First player selection

El primer jugador se determina usando `RandomizerPort.nextInt(2)`:

- 0 → playerOneId es el primer jugador
- 1 → playerTwoId es el primer jugador

Esto es responsabilidad de `SetupManager` según contrato 07.

---

# Expected GameState

El `GameState` retornado debe tener:

- `status`: `ACTIVE`
- `phase`: `DRAW`
- `turnNumber`: `1`
- `currentPlayerId`: el jugador que ganó el coin flip
- `firstPlayerId`: el mismo valor que `currentPlayerId`
- `turnFlags`: todos los flags en `false`
- ambos `PlayerState` con Active válido, 6 prizes, deck y mano válidos

---

# Required tests

Casos mínimos obligatorios:

- setup exitoso: ambos jugadores tienen Active, 6 prizes y mano
- ambos jugadores reciben exactamente 7 cartas iniciales
- mulligan simple: jugador sin Básico recibe nueva mano,
  oponente roba 1 carta extra
- mulligan múltiple: jugador declara 2 mulligans,
  oponente roba 2 cartas extra
- shuffle usa RandomizerPort exclusivamente
- GameState resultante tiene status ACTIVE y phase DRAW
- currentPlayerId es válido y coincide con firstPlayerId

---

# Verification requirements

Debe:

- compilar con `mvn compile`
- pasar `mvn test`
- preservar `ApplicationTests.contextLoads`
- no modificar clases fuera del ownership de Persona 2
- no introducir Spring, JPA ni aleatoriedad directa

---

# Scope control

Este spec implementa únicamente el flujo de setup inicial.

No implementar:
- gameplay
- turns
- attacks
- combat
- handlers
- validaciones de acciones