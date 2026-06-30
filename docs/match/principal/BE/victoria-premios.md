# Sistema de Victoria y Cartas de Premio

## Resumen

El sistema de victoria determina cuándo termina una partida y quién es el ganador. La partida puede terminar por cuatro razones: KO total del oponente, robo de todas las cartas de premio, agotamiento del mazo del oponente, o rendición. Además, las cartas de premio se gestionan a través de un sistema que permite al jugador seleccionar qué carta tomar cuando gana un KO.

## Arquitectura

```
VictoryConditionChecker (verificador de condiciones de victoria)
├── FinishReason (enum: 4 razones de finalización)
│   ├── KNOCKOUT
│   ├── PRIZES
│   ├── DECK_OUT
│   ├── CONCEDE
│   └── SUDDEN_DEATH
│
├── TakePrizeCardHandler (toma de premios)
├── ChooseKOReplacementHandler (reemplazo por KO)
└── RuleValidator (validación general de reglas)
```

---

## `VictoryConditionChecker`

**Ubicación:** `engine/victory/VictoryConditionChecker.java`

Clase que verifica las condiciones de victoria después de cada acción relevante. Se ejecuta después de ataques, al final del turno, y después de robar cartas.

### Métodos Clave:

| Método | Descripción |
|---|---|
| `checkVictory(GameState state)` | Verifica todas las condiciones de victoria |
| `isKnockoutVictory(PlayerState player)` | Verifica si el oponente no tiene Pokémon en juego |
| `isPrizeVictory(PlayerState player)` | Verifica si el jugador tomó todas sus cartas de premio |
| `isDeckOutVictory(PlayerState player)` | Verifica si el oponente no puede robar al inicio de su turno |
| `getWinner(GameState state)` | Retorna el jugador ganador o null si la partida continúa |

### Lógica de Verificación:

1. **Después de un KO**: verifica si el oponente tiene algún Pokémon en juego (activo o banca)
   - Si no tiene ningún Pokémon → victoria por KNOCKOUT
2. **Después de tomar una carta de premio**: verifica si el jugador tomó todas sus 6 cartas de premio
   - Si tomó la sexta → victoria por PRIZES
3. **Al inicio del turno**: verifica si el jugador actual puede robar carta
   - Si el mazo está vacío → victoria por DECK_OUT para el oponente

---

## `FinishReason`

**Ubicación:** `engine/victory/FinishReason.java`

Enum que define las formas en que puede terminar una partida.

| Razón | Descripción | ¿Quién gana? |
|---|---|---|
| `KNOCKOUT` | Un jugador se queda sin Pokémon en juego | El jugador que hizo el último KO |
| `PRIZES` | Un jugador tomó todas sus 6 cartas de premio | El jugador que tomó la última carta |
| `DECK_OUT` | Un jugador no puede robar al inicio de su turno | El oponente del jugador sin mazo |
| `CONCEDE` | Un jugador se rinde voluntariamente | El oponente del jugador que se rinde |
| `SUDDEN_DEATH` | Muerte súbita (regla de torneo, tiempo extra) | Quien gane la muerte súbita |

### `KNOCKOUT`

Ocurre cuando después de un ataque exitoso:
1. El Pokémon activo del oponente es debilitado (PS ≤ 0)
2. El oponente no tiene Pokémon en banca para reemplazarlo
3. Victoria inmediata para el atacante

### `PRIZES`

Ocurre cuando un jugador ha tomado todas sus cartas de premio:
1. Cada KO permite tomar 1 carta de premio
2. Cuando se toma la sexta (o las que correspondan según la modalidad), el jugador gana
3. Se verifica después de cada `TakePrizeCardHandler`

### `DECK_OUT`

Ocurre cuando un jugador debe robar una carta pero su mazo está vacío:
1. Se verifica al inicio del turno del jugador
2. Si el mazo está vacío antes de robar → el oponente gana
3. Esto incluye el efecto de cartas como "Mano Vacía" o "Investigación del Profesor" que agotan el mazo

### `CONCEDE`

Un jugador envía el evento `CONCEDE` para rendirse voluntariamente:
1. El oponente gana inmediatamente
2. No hay restricciones de cuándo se puede conceder

### `SUDDEN_DEATH`

Modo de desempate para torneos donde el tiempo reglamentario expiró:
1. Comienza una partida nueva con 1 carta de premio
2. Quien gane la primera ronda de KO gana la partida
3. Se determina por lanzamiento de moneda quién empieza

---

## `TakePrizeCardHandler`

**Ubicación:** `engine/handlers/TakePrizeCardHandler.java`

Maneja el evento `TAKE_PRIZE_CARD` cuando un jugador debe tomar una carta de premio después de hacer un KO.

### Flujo:

1. **Recepción del evento**: después de que `KnockoutCheckStep` determina que hubo un KO
2. El jugador que hizo el KO puede tomar `N` cartas de premio (normalmente 1, pero puede ser más según la carta del Pokémon debilitado, ej: Pokémon EX → 2 cartas de premio)
3. El jugador selecciona qué carta de premio tomar (no se revela al oponente):
   - `prizeCardIndex`: índice de la carta de premio a tomar
4. Validación:
   - El jugador tiene cartas de premio restantes
   - El índice es válido
5. Ejecución:
   - La carta de premio se mueve a la mano del jugador
   - Se reduce el contador de premios restantes
6. Post-ejecución:
   - `VictoryConditionChecker.isPrizeVictory()` verifica si el jugador ganó

### Cartas de Premio Especiales:

| Situación | Cartas de Premio |
|---|---|
| KO normal | 1 carta de premio |
| KO a Pokémon EX/GX/V | 2 cartas de premio |
| KO a Pokémon TAG TEAM | 3 cartas de premio |
| Modalidad "Premios Reducidos" | Configurable (3, 4 cartas) |

---

## `ChooseKOReplacementHandler`

**Ubicación:** `engine/handlers/ChooseKOReplacementHandler.java`

Maneja el evento `CHOOSE_KO_REPLACEMENT` cuando el Pokémon activo de un jugador es debilitado y debe elegir un reemplazo de la banca.

### Flujo:

1. **Recepción del evento**: después de que `KnockoutCheckStep` determina KO en el activo del jugador
2. El jugador debe seleccionar un Pokémon de su banca para ser el nuevo activo:
   - Envía el `benchPokemonId` del Pokémon que pasa a ser activo
3. Validación:
   - El jugador tiene al menos un Pokémon en banca
   - El Pokémon seleccionado está en la banca
4. Ejecución:
   - El Pokémon seleccionado pasa de banca a activo
   - El Pokémon debilitado va al descarte junto con todas sus energías y herramientas
   - Se aplican efectos de "entrada en juego" del nuevo activo (habilidades, etc.)
5. Post-ejecución:
   - Si el jugador no tiene Pokémon en banca, se declara victoria por KNOCKOUT para el oponente

### Lógica de Reemplazo:

```
[Pokémon activo debilitado (KO)]
  │
  ├── ¿Hay Pokémon en banca? ── NO ──→ [Victoria por KO para el oponente]
  │
  SÍ
  │
  ▼
[Jugador selecciona reemplazo de banca]
  │
  ▼
[Pokémon debilitado → descarte (con energía y herramientas)]
  │
  ▼
[Pokémon seleccionado → activo]
  │
  ▼
[Efectos de entrada en juego]
```

---

## `RuleValidator`

**Ubicación:** `engine/rules/RuleValidator.java`

Validador general de reglas del juego que se utiliza en múltiples puntos, incluyendo condiciones de victoria.

### Métodos Clave:

| Método | Descripción |
|---|---|
| `validateGameState(GameState state)` | Valida que el estado del juego sea consistente |
| `isDeckEmpty(PlayerState player)` | Verifica si el mazo de un jugador está vacío |
| `hasPokemonInPlay(PlayerState player)` | Verifica si el jugador tiene algún Pokémon en juego |
| `countPrizesRemaining(PlayerState player)` | Cuenta cuántas cartas de premio le quedan |
| `isLegalMove(GameEvent event, GameState state)` | Valida que un movimiento sea legal antes de ejecutarlo |

---

## Integración con el Sistema de Ataque

El flujo de victoria está integrado en el último paso de la cadena de ataque:

1. `DamageStep` inflige daño
2. `PostDamageEffectStep` aplica efectos secundarios
3. `KnockoutCheckStep` (último paso) verifica:
   - El defensor tiene PS ≤ 0
   - Si SÍ:
     - El Pokémon va al descarte
     - Se inicia `TakePrizeCardHandler` para el atacante
     - Se inicia `ChooseKOReplacementHandler` para el defensor
     - Si no hay reemplazo → `VictoryConditionChecker` declara KO victory
4. Después de la cadena completa, `VictoryConditionChecker` verifica si el atacante ganó por premios

---

## Resumen Visual del Flujo de Victoria

```
[Evento: KO, toma de premio, fin de turno, rendición]
  │
  ▼
VictoryConditionChecker.checkVictory(GameState)
  │
  ├── ¿Oponente sin Pokémon en juego? ──→ SÍ → Victoria por KNOCKOUT 🏆
  │
  ├── ¿Jugador tomó todos los premios? ──→ SÍ → Victoria por PRIZES 🏆
  │
  ├── ¿Mazo vacío al inicio del turno? ──→ SÍ → Victoria por DECK_OUT 🏆
  │
  ├── ¿Jugador se rinde? ──→ SÍ → Victoria por CONCEDE 🏆
  │
  └── Todo normal → La partida continúa ▶
```
