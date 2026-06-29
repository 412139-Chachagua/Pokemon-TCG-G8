# Plan de Testing Unitario — División de Tareas

**Objetivo:** Alcanzar **95% de cobertura total** en el backend y **95% en las 3 clases críticas** (RuleValidator, DamageCalculator, StatusEffectManager) mediante tests unitarios con JUnit + Mockito.

**Equipo (6 personas):**
- 412285-Cotaimich
- 412337-DelLungo
- 412139-Chachagua
- tobiaszairusandivara
- Eduardo-Mendoza-412488
- 412070-BelattiGarcia

---

## Reglas para evitar conflictos

1. Cada persona trabaja **exclusivamente** sobre los archivos asignados en su columna.
2. Nadie modifica un archivo que no esté en su asignación.
3. Los tests nuevos se crean en el paquete espejo dentro de `BE/src/test/java/ar/edu/utn/frc/tup/piii/`.
4. Los nombres de métodos de test deben estar **en inglés** (ej. `shouldRejectAttackOnFirstTurn`, `calculate_withWeakness_appliesMultiplier`).
5. Seguir el estilo existente: `@ExtendWith(MockitoExtension.class)`, mocks con `@Mock`, assertions con JUnit 5 + Mockito.

---

## División de archivos por persona

| Persona | Archivos que crea/modifica (únicos) | Rol |
|---------|--------------------------------------|-----|
| **412285-Cotaimich** | `engine/rules/RuleValidatorTest.java` **(expansión)** | Llevar RuleValidator de ~30% a **95%** |
| **412337-DelLungo** | `engine/attack/TextEffectParserTest.java` **(nuevo)** + `engine/attack/AttackEffectTypeTest.java` **(nuevo)** + `engine/SpecialConditionTest.java` **(nuevo)** + `engine/PlayerSideTest.java` **(nuevo)** + `engine/MatchStatusTest.java` **(nuevo)** + `engine/model/CardInstanceTest.java` **(nuevo)** + `exceptions/ExceptionTests.java` **(nuevo)** | Testear TextEffectParser + enums/modelos chicos + excepciones |
| **412139-Chachagua** | `engine/energy/EnergyServiceTest.java` **(nuevo)** + `engine/model/ModelTest.java` **(nuevo)** | Testear paquete energy + modelos faltantes (PublicDiscardCard, DeckValidationError) |
| **tobiaszairusandivara** | `engine/ability/AbilitySystemTest.java` **(nuevo)** | Testear todo el paquete ability (hooks + resolvers + registry) |
| **Eduardo-Mendoza-412488** | `engine/attack/AttackPipelineTest.java` **(nuevo)** + `engine/attack/DamageCalculatorTest.java` **(expansión)** + `engine/match/states/MatchStatesTest.java` **(nuevo)** + `engine/model/TurnFlagsTest.java` **(expansión)** + `engine/handlers/HandlerHelperTest.java` **(expansión)** | Testear pipeline de ataque + DamageCalculator a 95% + match states + bordes en handlers y TurnFlags |
| **412070-BelattiGarcia** | `engine/attack/effects/RemainingEffectsTest.java` **(nuevo)** + `engine/attack/StatusEffectManagerTest.java` **(expansión)** + `engine/model/PlayerStateTest.java` **(expansión)** + `engine/model/GameStateTest.java` **(expansión)** | Testear 25 efectos de ataque + StatusEffectManager a 95% + bordes en PlayerState y GameState |

**Nota:** Ningún archivo aparece en más de una fila. No hay conflictos.

---

## Detalle por persona

---

### 412285-Cotaimich — RuleValidatorTest.java (expansión)

**Archivo a modificar:** `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/rules/RuleValidatorTest.java`

**Qué tiene que hacer:** Agregar tests unitarios para todos los métodos de `RuleValidator` que aún no están cubiertos. Actualmente solo se testean `validateEvolve`, `validateAttack` y `validateKOReplacement`. Faltan ~16 métodos.

**Métodos a testear (nombres de test en inglés):**

- `validateAttachEnergy`: verificar que rechaza si `canPlay()` es false, si ya adjuntó energía en el turno, si el handIndex es inválido, si la carta no es Energy, si el target Pokémon no existe, y que acepta cuando todo es válido.
- `validatePutBasicOnBench`: verificar que rechaza si `canPlaceBasic()` es false, si handIndex es inválido, si la carta no es Pokémon Básico, si la banca está llena (5), y que acepta cuando es válido.
- `validatePlayTrainer`: cubrir todos los caminos — rechazo si `canPlay()` es false, si la carta no es Trainer, si el Item está bloqueado por ForestsCurse, si ya jugó Supporter, si ya jugó Stadium, si el effectCode es desconocido, si falta target requerido; validar EvolveDirect (rechazo en primer turno, target no encontrado, target entró este turno, target ya evolucionó), ReturnToDeck (sin banca, target activo sin reemplazo), DiscardOpponentEnergy (target propio).
- `validateRetreat`: rechazo si `canPlay()` es false, si ya retiró, si no hay banca, si no hay activo, si no puede pagar energía, si está dormido/paralizado; aceptar con Fairy Garden, aceptar con pago de energía válido.
- `validateEndTurn`: rechazar si hay KO pendiente, rechazar si `canEndTurn()` es false, aceptar cuando está todo bien.
- `validateDrawCard`: rechazar si `canDraw()` es false, si ya robó este turno, aceptar cuando es válido.
- `validateTakePrizeCard`: rechazar si no hay pendingPrizeOwner, si el playerId no coincide, aceptar cuando coincide.
- `validateAttachTool`: rechazar si `canPlay()` es false, handIndex inválido, no es Trainer, no es subtipo Tool, targetId nulo, target no encontrado, target ya tiene tool; aceptar cuando es válido.
- `validateUseAbility`: rechazar si `canPlay()` es false, pokemonInstanceId nulo, abilityName nulo, Pokémon no encontrado, no tiene la habilidad, abilities suprimidas, ya usó la habilidad, está dormido/paralizado; aceptar cuando es válido.
- `validateSetupPlaceActive`: rechazar si player es null, si ya tiene activo, si cardInstanceId es nulo, si la carta no está en la mano, si no es Pokémon Básico; aceptar si es válido.
- `validateSetupPlaceBench`: rechazar si player es null, si banca >= 5, si cardInstanceId es nulo, si la carta no está en la mano, si no es Básico; aceptar si es válido.
- `validateSetupRemoveActive`: rechazar si player es null, si no hay activo; aceptar si hay activo.
- `validateSetupRemoveBench`: rechazar si player es null, si cardInstanceId es nulo, si el Pokémon no está en banca; aceptar si está.
- `validateConfirmSetup`: rechazar si player es null, si ya confirmó, si hay mulliganDraw pendiente, si no tiene activo; aceptar si todo ok.
- `validateResolveMulliganDraw`: rechazar si status no es SETUP, si no hay mulliganDrawPending, si el jugador no tiene pending; aceptar si todo ok.
- `isToolSubtype` (static): probar con TrainerSubtype.POKEMON_TOOL, TrainerSubtype.ITEM, subtypes list con "TOOL", "POKEMON_TOOL", variantes con acentos/espacios, y casos sin tool.

---

### 412337-DelLungo — TextEffectParserTest.java + modelos chicos + excepciones

**Archivos a crear:**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/TextEffectParserTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/AttackEffectTypeTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/SpecialConditionTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/PlayerSideTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/MatchStatusTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/model/CardInstanceTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/exceptions/ExceptionTests.java`

**Qué tiene que hacer:** Testear el `TextEffectParser` (el archivo más grande sin test del engine, 1058 líneas), crear tests para enums y modelos pequeños, y testear las 5 excepciones personalizadas.

**TextEffectParser — métodos a testear (nombres en inglés):**

- `parse()`: probar con textos reales de ataque que produzcan: daño simple, daño con multiplicador (×), daño con lanzamiento de moneda, curación, condiciones especiales, daño a banca, descarte de energía, robo de cartas, búsqueda en deck, prevención de daño, etc. Probar casos borde (texto null, texto vacío, texto sin efectos reconocibles).
- `parseSpecialConditions()`: probar strings con "Asleep", "Confused", "Paralyzed", "Burned", "Poisoned" y combinaciones múltiples.
- `parseHeal()`: probar "Heal 30", "Heal 60 damage", "Remove 2 damage counters", casos nulos.
- `parseBenchDamage()`: probar "30 damage to 1 Benched Pokemon", "10 damage to each Benched Pokemon", "20 damage to 2 Benched Pokemon".
- `parseDiscardEnergy()`: probar "Discard 2 Energy", "Discard all Energy", casos sin energía.
- `parseDrawCards()`: probar "Draw 2 cards", "Draw until you have 6 cards", "Draw 1 card".
- `parseSearchDeck()`: probar "Search your deck for a Basic Pokemon", "Search for up to 2 Energy".
- `parseAttachEnergy()`: probar "Attach 2 Energy", "Attach a basic Energy".
- `parseMoveEnergy()`: probar "Move 1 Energy", "Move all Energy".
- `parseDamagePrevention()`: probar "Prevent all damage", "Prevent 30 damage".
- `parseCannotAttackNextTurn()`: probar "The Defending Pokemon can't attack".
- `parseSupporterLock()`: probar "Your opponent can't play Supporter".
- `parseOpponentDiscardHand()`: probar "Your opponent discards their hand".
- `parseNextTurnDamageBonus()`: probar "During your next turn, this Pokemon's attacks do 30 more".
- `parseRetreatLock()`: probar "The Defending Pokemon can't retreat".
- `parseDamageReduction()`: probar "Reduce damage by 20".
- `parseDiscardOpponentDeck()`: probar "Discard 2 cards from your opponent's deck".
- `parseSearchDiscard()`: probar "Search your discard pile for a Pokemon".
- `parseRecycle()`: probar "Put 2 Energy from discard into your hand".
- `parseOpponentShuffleDraw()`: probar "Your opponent shuffles and draws".
- `parseDamageAllBench()`: probar "10 damage to each Benched Pokemon", "20 damage to all Benched".
- `parseCoinFlipBeforeDamage()`: probar con strings que tengan coin flip antes de daño.
- `parseCoinFlipAfterDamage()`: probar con strings que tengan coin flip después de daño.
- `detectPokemonType()`: probar con nombres de Pokémon que contengan tipos ("Fire", "Water", "Grass", etc.).

**Modelos chicos — tests simples:**

- `AttackEffectTypeTest`: verificar que todos los enum values existen y son accesibles.
- `SpecialConditionTest`: verificar los 5 valores del enum (ASLEEP, CONFUSED, PARALYZED, BURNED, POISONED).
- `PlayerSideTest`: verificar PLAYER_A, PLAYER_B.
- `MatchStatusTest`: verificar WAITING, SETUP, ACTIVE, FINISHED.
- `CardInstanceTest`: verificar constructor, getInstanceId(), getCardDefinitionId().

**Excepciones — tests simples:**

- `ExceptionTests`: crear tests para las 5 excepciones personalizadas (`ConflictException`, `DomainException`, `NotFoundException`, `StorageException`, `ValidationException`) verificando constructores, mensajes, causas. Son archivos de ~20-30 líneas cada uno.

---

### 412139-Chachagua — EnergyServiceTest.java + ModelTest.java

**Archivos a crear:**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/energy/EnergyServiceTest.java`
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/model/ModelTest.java`

**Qué tiene que hacer:** Testear todos los archivos del paquete `engine.energy` (18 clases, 0% cobertura hoy) y los modelos/DTOs faltantes.

**EnergyService — métodos a testear (nombres en inglés):**

- `EnergyService`:
  - `buildPool()`: verificar que construye correctamente el pool de energías de un Pokémon.
  - `checkAttackRequirements()`: probar con energía suficiente, insuficiente, con Rainbow/DoubleColorless/Strong, con tipos mixtos.
  - `validateAndPayRetreat()`: probar retirada con energía suficiente, insuficiente, con descarte de energías específicas.
  - `attachFromHand()`: verificar que la energía se mueve de la mano al Pokémon y se aplican efectos onAttach.
  - `attachFromDeck()`: verificar que la energía se mueve del deck al Pokémon.
  - `detachEnergies()`: verificar que se descartan energías específicas.
  - `detachAllEnergies()`: verificar que se descartan todas las energías.
  - `transferEnergy()`: verificar que la energía se mueve entre Pokémon.
- `EnergyMatchingEngine`: probar `selectPayment()` con distintas combinaciones de energía, `canPay()` con energía suficiente/insuficiente.
- `EnergyStrategyRegistry`: probar `registerStrategy()` y `getStrategy()` para cada tipo de energía.
- `BasicEnergyStrategy`: probar `resolve()` y `onAttach()` — verificar que provee el tipo básico correcto.
- `DoubleColorlessEnergyStrategy`: probar `resolve()` (provee 2 Colorless) y `getDamageModifiers()`.
- `RainbowEnergyStrategy`: probar `resolve()` (provee 1 de cualquier tipo) y `onAttach()` (aplica daño al Pokémon).
- `StrongEnergyStrategy`: probar `resolve()` y `getDamageModifiers()` (incrementa daño a Fighting).
- `EnergyPaymentResult`: probar `canPay()`, `getAllocations()`, `getErrorMessage()` en casos de éxito y fracaso.
- `EnergySource`, `EnergyAllocation`: probar constructores y getters (records).

**Modelos faltantes:**

- `ModelTest.java`: testear `PublicDiscardCard` (record, verificar constructor y componentes), `DeckValidationError` (verificar constructor, getters). Son clases pequeñas de 5-15 líneas cada una.

---

### tobiaszairusandivara — AbilitySystemTest.java

**Archivo a crear:** `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/ability/AbilitySystemTest.java`

**Qué tiene que hacer:** Testear todo el sistema de habilidades (abilities): el registry, los 5 hooks y los 6 resolvers. No existe ningún test hoy para ninguna de estas 13 clases.

**Clases a testear (nombres de test en inglés):**

- `AbilityRegistry`: probar `register()` y `resolveAbility()` con distintas habilidades.
- Hooks (interceptan eventos del juego):
  - `FurCoatHook`: probar `reduceDamage()` — verifica que reduce daño a la mitad si el defensor tiene Fur Coat.
  - `SweetVeilHook`: probar `isImmune()` — verifica que Pokémon con energía Fairy y Sweet Veil son inmunes a condiciones.
  - `ForestsCurseHook`: probar `isItemBlocked()` — verifica que bloquea Items cuando ForestsCurse está activo.
  - `SpikyShieldHook`: probar que refleja daño al atacante cuando Spiky Shield está activo.
  - `DestinyBurstHook`: probar que hace daño masivo al knockoutearse.
- Resolvers (ejecutan efectos de habilidades):
  - `MysticalFireResolver`: probar `resolve()` — descarta energía y roba cartas.
  - `StanceChangeResolver`: probar `resolve()` — cambia el stance del Pokémon.
  - `FairyTransferResolver`: probar `resolve()` y `transferEnergy()` — mueve energía Fairy entre Pokémon.
  - `DriveOffResolver`: probar `resolve()` — fuerza cambio del activo del oponente.
  - `WaterShurikenResolver`: probar `resolve()` y `multiTargetDamage()` — daño múltiple a objetivos.
  - `UpsideDownEvolutionResolver`: probar `resolve()` — evolución desde el mazo.

---

### Eduardo-Mendoza-412488 — AttackPipelineTest.java + DamageCalculatorTest.java (expansión) + MatchStatesTest.java + bordes en handlers

**Archivos a crear/modificar:**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/AttackPipelineTest.java` **(nuevo)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/DamageCalculatorTest.java` **(expandir)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/match/states/MatchStatesTest.java` **(nuevo)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/model/TurnFlagsTest.java` **(expandir, agregar bordes)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/handlers/HandlerHelperTest.java` **(expandir, agregar bordes)**

**Qué tiene que hacer:** Testear el pipeline de ataque (PostDamageEffectStep, PreDamageStep, AttackChainBuilder, AttackContext, etc.), expandir DamageCalculatorTest para llegar a 95%, testear los 5 estados de la partida, y agregar tests bordes a TurnFlags y HandlerHelper.

**AttackPipelineTest — métodos a testear (nombres en inglés):**

- `PostDamageEffectStep.execute()`:
  - Camino 1: ataque con post-damage effect simple (ej. curarse después de atacar) — verificar que el efecto se ejecuta.
  - Camino 2: ataque con post-damage effect que aplica condición — verificar que la condición se aplica.
  - Camino 3: ataque sin post-damage effects — verificar que no pasa nada.
  - Camino 4: ataque con post-damage effect que descarta energía — verificar el descarte.
- `PreDamageStep.execute()`:
  - Aplica modificadores pre-daño (ej. aumentar daño si cierta condición).
  - No aplica modificadores si no hay condiciones.
  - Caso borde con modificadores nulos.
- `AttackChainBuilder.executeChain()`:
  - Crear cadena completa con todos los steps y ejecutar.
  - Verificar que cada step se ejecuta en orden.
  - Verificar que si un step falla, la cadena se interrumpe.
- `AttackContext`: testear constructor, getters, setters, `addCoinFlipResult()`, `allCoinFlipsHeads()`.
- `BaseAttackStep`: testear `setNext()`, `proceed()`, `buildChain()` (factory estática).
- `AbstractAttackStep`: testear `execute()` y el manejo de la cadena.

**DamageCalculatorTest — expansión para 95%:**

Agregar 3-4 tests bordes:
- Daño con tipos que no matchean ni weakness ni resistance del defensor.
- Ataque con attackerDef con attacks null o lista vacía.
- `parseDamage()` con string que tiene caracteres no numéricos además del número.
- `calculate()` con stadiumEffectCode null.

**MatchStatesTest — métodos a testear (nombres en inglés):**

- `MatchState` (interface): verificar que los 4 estados concretos existen.
- `WaitingMatchState.canExecuteAction()`: debe permitir acciones de unirse a partida, rechazar acciones de juego.
- `SetupMatchState.canExecuteAction()`: debe permitir acciones de setup, rechazar acciones de juego.
- `ActiveMatchState.canExecuteAction()`: debe permitir acciones de juego, rechazar setup.
- `FinishedMatchState.canExecuteAction()`: debe rechazar cualquier acción.

**Bordes en TurnFlagsTest (expandir):**

- `resetAll()`: verificar que todos los flags vuelven a false.
- `hasAttachedEnergy()`: verificar que arranca en false y se setea a true.
- `hasPlayedSupporter()` + `hasPlayedStadium()`: verificar flags de entrenadores.
- Combinaciones de flags activos simultáneamente.

**Bordes en HandlerHelperTest (expandir):**

- `findPokemon()` con player con active null y bench vacío.
- `findPokemon()` con instanceId que no pertenece al player.
- `findPokemon()` con bench que contiene el Pokémon buscado.

---

### 412070-BelattiGarcia — RemainingEffectsTest.java + StatusEffectManagerTest.java (expansión) + bordes en PlayerState y GameState

**Archivos a crear/modificar:**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/effects/RemainingEffectsTest.java` **(nuevo)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/attack/StatusEffectManagerTest.java` **(expandir)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/model/PlayerStateTest.java` **(expandir, agregar bordes)**
- `BE/src/test/java/ar/edu/utn/frc/tup/piii/engine/model/GameStateTest.java` **(expandir, agregar bordes)**

**Qué tiene que hacer:** Testear los 25 efectos de ataque que no tienen cobertura hoy, expandir StatusEffectManagerTest para llegar a 95%, y agregar tests bordes a PlayerState y GameState.

**RemainingEffectsTest — efectos a testear (nombres en inglés, cada efecto puede tener 2-3 escenarios):**

- `AbilitySuppressionEffect`: verificar que suprime habilidades del defensor/atacante.
- `AttachEnergyEffect`: verificar que adjunta energía del deck al Pokémon.
- `CanNotAttackNextTurnEffect`: verificar que marca al defensor para no poder atacar.
- `CoinFlipPostDamageEffect`: verificar heads aplica efecto adicional, tails no.
- `DamageAllBenchEffect`: verificar que aplica daño a todos los Pokémon en banca del rival.
- `DamagePreventionEffect`: verificar que previene todo daño o una cantidad específica.
- `DamageReductionEffect`: verificar que reduce daño que recibe el atacante.
- `DefenderCannotAttackEffect`: verificar que marca al defensor como unable to attack.
- `DiscardOpponentDeckEffect`: verificar que descarta cartas del deck del oponente.
- `DiscardToolEffect`: verificar que descarta la herramienta del Pokémon objetivo.
- `MentalPanicEffect`: verificar que aplica Confused + Poisoned (efecto combinado).
- `MoveEnergyEffect`: verificar que mueve energía entre Pokémon propios.
- `NextTurnDamageBonusEffect`: verificar que incrementa daño del atacante el próximo turno.
- `OpponentDiscardHandEffect`: verificar que el oponente descarta su mano.
- `OpponentRandomDiscardEffect`: verificar que descarta cartas aleatorias de la mano del oponente.
- `PeekOpponentDeckEffect`: verificar que permite ver cartas del deck del oponente.
- `PostDamageEffect` (abstract): crear una subclase concreta de test y verificar execute().
- `RecycleFromDiscardEffect`: verificar que recupera cartas del discard a la mano.
- `ReorderDeckEffect`: verificar que reordena el tope del deck.
- `RetreatLockEffect`: verificar que impide la retirada del Pokémon defensor.
- `SearchDiscardEffect`: verificar que busca cartas en la pila de descarte.
- `SetHpEffect`: verificar que setea el HP de un Pokémon a un valor específico.
- `SupporterLockEffect`: verificar que impide jugar Partidarios al oponente.
- `SwitchDefenderEffect`: verificar que fuerza cambio del Pokémon activo del oponente.

Nota: efectos ya testeados en archivos separados **NO** tocar (ApplyConditionEffect, DamageBenchEffect, DiscardEnergyEffect, DrawCardsEffect, HealUserEffect, RecoilEffect, SearchDeckEffect).

**StatusEffectManagerTest — expansión para 95%:**

Agregar 4-5 tests bordes:
- `checkKoBetweenTurns()` con Pokémon de la banca (no activo) — verificar que el Pokémon se remueve de la banca.
- `checkKoBetweenTurns()` con Pokémon-EX — verificar que pendingPrizeCount suma 2.
- `processBetweenTurnStatuses()` con múltiples condiciones simultáneas (envenenado + quemado).
- `applyCondition()` re-aplicando la misma condición volátil (debería re-aplicarse, no duplicarse).
- `processBetweenTurnStatuses()` con active sin conditions (null) — no debe explotar.

**Bordes en PlayerStateTest (expandir):**

- `pushManyToDiscard()` con lista vacía.
- `pushManyToDiscard()` con lista con múltiples cartas.
- Getters/setters de campos que no estén probados (getDiscard(), getHand(), getBench(), etc.).

**Bordes en GameStateTest (expandir):**

- `fromStatus()` con cada estado del enum MatchStatus.
- `fromPhase()` con cada fase del turno.
- `hasPlayerCompletedFirstTurn()` con playerId nulo o inexistente.
- `isMulliganFullyResolved()` en distintos estados del mulligan.

---

## Recordatorio: estilo de tests

```java
@ExtendWith(MockitoExtension.class)
class MiClaseTest {

    @Mock
    private Dependencia mockDependencia;

    private MiClase sut;  // System Under Test

    @BeforeEach
    void setUp() {
        sut = new MiClase(mockDependencia);
    }

    @Test
    void should_expectedBehavior_when_condition() {
        // given
        // when
        // then
    }
}
```

Todos los nombres de métodos y variables en **inglés**. Comentarios y descripciones pueden estar en español para facilitar la lectura del equipo.

---

## Resumen de archivos por persona (verificación rápida)

| Persona | Archivos |
|---------|----------|
| Cotaimich | `RuleValidatorTest.java` (expande) |
| DelLungo | `TextEffectParserTest.java` (nuevo), `AttackEffectTypeTest.java` (nuevo), `SpecialConditionTest.java` (nuevo), `PlayerSideTest.java` (nuevo), `MatchStatusTest.java` (nuevo), `CardInstanceTest.java` (nuevo), `ExceptionTests.java` (nuevo) |
| Chachagua | `EnergyServiceTest.java` (nuevo), `ModelTest.java` (nuevo) |
| tobiaszairusandivara | `AbilitySystemTest.java` (nuevo) |
| Eduardo-Mendoza | `AttackPipelineTest.java` (nuevo), `DamageCalculatorTest.java` (expande), `MatchStatesTest.java` (nuevo), `TurnFlagsTest.java` (expande), `HandlerHelperTest.java` (expande) |
| BelattiGarcia | `RemainingEffectsTest.java` (nuevo), `StatusEffectManagerTest.java` (expande), `PlayerStateTest.java` (expande), `GameStateTest.java` (expande) |

Ningún archivo se repite entre personas. Sin conflictos de merge. ✅

---

## Estimación de líneas y complejidad por persona

### 412285-Cotaimich

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `RuleValidatorTest.java` (expandir existente) |
| **Líneas actuales del test** | ~226 |
| **Líneas estimadas finales** | ~700 – 750 |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~50 – 55 métodos |
| **Complejidad** | 🔴 **ALTA** — requiere mockear ~16 métodos distintos de RuleValidator, cada uno con entre 3 y 8 escenarios (éxito + múltiples fallos). `validatePlayTrainer` es el más complejo por sus 6+ caminos internos. Necesita crear mocks de `TrainerEffectRegistry`, `EnergyService`, `ForestsCurseHook`, `HandlerHelper`, `GameState`, `PlayerState`, `PokemonInPlay`, `CardLookupPort`, `TurnFlags`, `TurnState`, etc. |
| **Riesgo principal** | Cantidad de tests: es la persona que más métodos nuevos escribe (~50 tests). |
| **Impacto en cobertura** | Lleva RuleValidator de ~30% a **95%** (clase crítica del PDF). |

---

### 412337-DelLungo

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `TextEffectParserTest.java` (nuevo), `AttackEffectTypeTest.java` (nuevo), `SpecialConditionTest.java` (nuevo), `PlayerSideTest.java` (nuevo), `MatchStatusTest.java` (nuevo), `CardInstanceTest.java` (nuevo), `ExceptionTests.java` (nuevo) |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~40 – 45 métodos |
| **Complejidad** | 🟡 **MEDIA-ALTA** — `TextEffectParser` es el archivo **más grande sin test del engine** (1058 líneas). Tiene ~23 métodos públicos de parseo que cubren muchos formatos de texto de ataque distintos. El patrón de test es repetitivo: entrada (string) → salida esperada. Los 5 tests de modelos chicos son triviales (~5 líneas cada uno). Los tests de excepciones son simples (~10-15 líneas cada excepción). |
| **Riesgo principal** | Archivo muy grande (1058 líneas) → puede ser tedioso. Pero los tests son mayormente parametrizables. |
| **Impacto en cobertura** | Cubre el archivo individual **más grande** sin cobertura + 5 excepciones. Impacto masivo en el 95% global. |

---

### 412139-Chachagua

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `EnergyServiceTest.java` (nuevo), `ModelTest.java` (nuevo) |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~35 – 40 métodos |
| **Complejidad** | 🟡 **MEDIA-ALTA** — `EnergyService` tiene 8 métodos principales que requieren entender cómo funciona el matching de energías, las estrategias (Basic, DoubleColorless, Rainbow, Strong) y el pago de retirada. Las estrategias son clases chicas pero con lógica específica. `EnergyMatchingEngine` requiere entender combinaciones de tipos de energía. `ModelTest.java` es trivial (~20 líneas). |
| **Riesgo principal** | Requiere comprensión del sistema de energía (pool, matching, payment). Los tests de `RainbowEnergyStrategy` necesitan verificar que aplica daño al Pokémon al adjuntarse. |
| **Impacto en cobertura** | Cubre **18 archivos** del paquete energy que hoy tienen 0% de cobertura + modelos faltantes. Impacto crítico para el 95% global. |

---

### tobiaszairusandivara

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `AbilitySystemTest.java` (nuevo) |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~30 – 35 métodos |
| **Complejidad** | 🟡 **MEDIA-ALTA** — El sistema de habilidades tiene 3 partes con distinta complejidad: (1) `AbilityRegistry` — simple (registrar y resolver), (2) 5 hooks — cada uno intercepta un evento específico del juego con lógica diferente (`FurCoatHook` reduce daño a la mitad, `SweetVeilHook` chequea inmunidad por energía Fairy, `ForestsCurseHook` bloquea Items, etc.), (3) 6 resolvers — cada uno ejecuta una habilidad con efectos distintos (transferir energía, cambiar stance, evolucionar desde el mazo, etc.). |
| **Riesgo principal** | Cada hook y resolver tiene lógica muy distinta → no se puede copiar y pegar fácilmente. Requiere entender la mecánica de cada habilidad. |
| **Impacto en cobertura** | Cubre **13 archivos** del paquete ability que hoy tienen 0%. Impacto crítico para el 95% global. |

---

### Eduardo-Mendoza-412488

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `AttackPipelineTest.java` (nuevo), `DamageCalculatorTest.java` (expandir), `MatchStatesTest.java` (nuevo), `TurnFlagsTest.java` (expandir), `HandlerHelperTest.java` (expandir) |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~30 – 35 métodos |
| **Complejidad** | 🟢 **MEDIA** — `PostDamageEffectStep` (438 líneas) es la parte más compleja: testear 4 caminos de ejecución. `DamageCalculatorTest` expandir es trivial (3-4 tests bordes). `MatchStatesTest` es simple (5 estados). `TurnFlagsTest` y `HandlerHelperTest` son expansiones pequeñas de archivos existentes. |
| **Riesgo principal** | `PostDamageEffectStep` tiene lógica densa. `AttackChainBuilder` requiere entender cómo se encadenan los steps. |
| **Impacto en cobertura** | Lleva DamageCalculator a **95%** (clase crítica del PDF). Cubre pipeline de ataque completo + match states + bordes en handlers. |

---

### 412070-BelattiGarcia

| Ítem | Valor |
|------|-------|
| **Archivo(s)** | `RemainingEffectsTest.java` (nuevo), `StatusEffectManagerTest.java` (expandir), `PlayerStateTest.java` (expandir), `GameStateTest.java` (expandir) |
| **Líneas nuevas estimadas** | ~500 |
| **Cantidad de tests nuevos** | ~50 – 55 métodos |
| **Complejidad** | 🔴 **ALTA** — tiene que testear **25 efectos de ataque diferentes**, cada uno con lógica distinta. Algunos son simples (2-3 tests) y otros más complejos (`CoinFlipPostDamageEffect` con 4 caminos, `MoveEnergyEffect` que requiere transferencia). La variedad de efectos hace que no pueda aplicar un solo patrón: cada efecto requiere entender qué hace exactamente. Las expansiones de `PlayerStateTest` y `GameStateTest` son simples (~5-10 tests bordes cada una). |
| **Riesgo principal** | Es la persona con más clases fuente distintas que cubrir (25 efectos). |
| **Impacto en cobertura** | Lleva StatusEffectManager a **95%** (clase crítica del PDF). Cubre **25 archivos** de efectos que hoy tienen 0% de cobertura + bordes en modelos principales. Impacto masivo en el 95% global. |

---

### Comparativa final

| Persona | Archivos | Líneas nuevas | Tests nuevos | Complejidad |
|---------|----------|:------------:|:------------:|:-----------:|
| Cotaimich | 1 (expandir) | ~500 | ~55 | 🔴 Alta |
| DelLungo | 7 (1 grande + 5 chicos + excepciones) | ~500 | ~45 | 🟡 Media-Alta |
| Chachagua | 2 (1 grande + 1 chico) | ~500 | ~40 | 🟡 Media-Alta |
| tobiaszairusandivara | 1 (nuevo) | ~500 | ~35 | 🟡 Media-Alta |
| Eduardo-Mendoza | 5 (2 nuevos + 3 expandir) | ~500 | ~35 | 🟢 Media |
| BelattiGarcia | 4 (1 nuevo + 3 expandir) | ~500 | ~55 | 🔴 Alta |

**Total líneas nuevas estimadas:** ~3.000
**Total tests nuevos estimados:** ~265
**Cobertura esperada:** ≥95% global · ≥95% en RuleValidator, DamageCalculator, StatusEffectManager ✅
