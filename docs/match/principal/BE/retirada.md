# Sistema de Retirada

## Resumen

La retirada permite al jugador cambiar su Pokémon activo por uno de la banca durante su turno. El Pokémon activo actual se retira y es reemplazado por un Pokémon de la banca elegido por el jugador. La retirada normalmente tiene un costo de energía que debe pagarse (descarte de energías), aunque algunos Pokémon tienen "Retirada Gratuita" (costo 0).

## Arquitectura

El sistema de retirada es simple y se maneja a través de un solo handler:

```
RetreatActiveHandler
  │
  ├── EnergyService.verifyCost()
  ├── PlayerState.activePokemon ↔ PlayerState.benchPokemon
  └── GameEngine.notifyStateChange()
```

---

## `RetreatActiveHandler`

**Ubicación:** `engine/handlers/RetreatActiveHandler.java`

Maneja el evento `RETREAT_ACTIVE` enviado por el jugador para retirar su Pokémon activo.

### Flujo de Retirada:

1. **Recepción del evento**: el jugador envía `RETREAT_ACTIVE` con el `benchPokemonId` del Pokémon que pasa a ser activo
2. **Validación básica**:
   - Es el turno del jugador
   - El jugador tiene un Pokémon activo
   - El jugador tiene al menos un Pokémon en banca
   - El Pokémon seleccionado está en la banca
   - El Pokémon activo no ha sido retirado ya este turno
3. **Verificación de costo**:
   - Lee el `retreatCost` del Pokémon activo actual
   - Verifica que el Pokémon tenga energía suficiente adjunta
   - Usa `EnergyService.verifyCost()` para la validación
4. **Pago del costo**:
   - Descarta la cantidad requerida de energías del Pokémon activo
   - Las energías van al mazo de descarte del jugador
5. **Intercambio de Pokémon**:
   - El Pokémon activo pasa a la banca
   - El Pokémon seleccionado de la banca pasa a ser activo
6. **Limpieza**:
   - Se eliminan condiciones especiales del nuevo activo (según reglas)
   - Se aplican efectos de "cambio forzado" si corresponde (ej: habilidades que se disparan al cambiar)

### Métodos Clave:

| Método | Descripción |
|---|---|
| `handleEvent(GameEvent event)` | Procesa el evento de retirada |
| `validateRetreat(PlayerState player)` | Valida que la retirada sea legal |
| `payRetreatCost(PokemonInPlay active)` | Descarta energías para pagar el costo |
| `executeRetreat(PlayerState player, PokemonInPlay newActive)` | Realiza el intercambio de Pokémon |

### Costo de Retirada:

El costo de retirada está definido en la carta del Pokémon (`PokemonCard.retreatCost`). Puede ser:
- **0**: Retirada gratuita (sin costo)
- **1, 2, 3, 4**: Número de energías a descartar

El tipo de energía descartada **no importa** para la retirada; cualquier energía cuenta. El sistema descarta la energía más versátil primero (incolora antes que específica) para minimizar la pérdida.

### Reglas Especiales:

- **Pokémon dormido o paralizado**: no pueden retirarse (a menos que una carta lo permita)
- **Pokémon confundido**: lanzamiento de moneda para retirarse (cara: éxito, cruz: falla)
- **Pokémon atrapado**: efectos como "Atrapado" del oponente impiden la retirada
- **Herramientas**: "Burbuja de Escape" reduce el costo en 1, "Campana de Retirada" permite retirada adicional
- **Habilidades**: ciertas habilidades pueden reducir o eliminar el costo de retirada

---

## Integración con `EnergyService`

El handler utiliza `EnergyService` para:
1. **Verificar costo**: `EnergyService.canPayCost(activePokemon, retreatCost)`
2. **Pagar costo**: `EnergyService.payCost(activePokemon, retreatCost)`

Aunque el costo de retirada normalmente usa el mismo sistema que los ataques, el matching es más simple porque la retirada solo requiere una cantidad X de energías de cualquier tipo.

---

## Efectos de Cartas que Interactúan con la Retirada

| Tipo de Carta | Efecto |
|---|---|
| Herramienta "Burbuja de Escape" | Reduce el costo de retirada en 1 |
| Herramienta "Campana de Retirada" | Permite una retirada adicional por turno |
| Entrenador "Relevo" | Cambia el activo sin pagar costo de retirada |
| Habilidad "Vuelo" (ej: Pidgeot) | Reduce costo de retirada de todos los Pokémon |
| Condición "Atrapado" | Impide la retirada del Pokémon activo |

---

## Resumen Visual del Flujo de Retirada

```
[Jugador: RETREAT_ACTIVE]
  │
  ▼
RetreatActiveHandler.handleEvent()
  │
  ├── Validar turno
  ├── Validar activo existe
  ├── Validar banca no vacía
  ├── Validar Pokémon seleccionado en banca
  │
  ├── ¿Pokémon dormido o paralizado? ──→ NO puede retirarse ❌
  ├── ¿Pokémon confundido? ──→ Moneda: ¿cara? ──→ NO → falla ❌
  │
  ├── EnergyService.canPayCost(active, retreatCost)
  │   └── ¿Tiene energía suficiente? ──→ NO → no puede retirarse ❌
  │
  ├── EnergyService.payCost(active, retreatCost)
  │   └── Descarta energías
  │
  ├── Intercambiar activo ↔ banca
  ├── Aplicar efectos post-cambio
  │
  └── [Éxito]
```
