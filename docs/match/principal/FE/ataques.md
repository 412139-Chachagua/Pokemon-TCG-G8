# Ataques

## Vista General

La mecánica de ataque se coordina entre **MatchPage** (orquestador), **ActionPanelComponent** (barra de acciones), **PokemonSlotComponent** (visualización del Pokémon en campo), y **game-event-formatter.ts** (formateo de eventos para el log).

## Barra de Ataques

### Show Attack Bar

En `MatchPage` (línea 1014), la barra de ataques se muestra cuando se cumplen todas estas condiciones:

```typescript
readonly showAttackBar = computed(() => {
    if (state.status !== 'ACTIVE' || state.phase !== 'MAIN') return false;
    if (!this.matchState.isMyTurn()) return false;
    if (this.needsKOReplacement()) return false;
    if (state.turnNumber === 1 && state.currentPlayerId === state.firstPlayerId) return false;
    // El Pokémon activo debe tener al menos un ataque definido
});
```

### attacksWithAvailability

Se calculan los ataques disponibles del Pokémon activo. Por cada ataque se verifica si el costo de energía está cubierto mediante `checkCost()`:

```typescript
private readonly checkCost = (cost: string[], energies: string[]): boolean => {
    // 1. Filtra energías específicas (no COLORLESS) que faltan
    // 2. Cuenta cuántas COLORLESS se necesitan
    // 3. Por cada energía adjunta, descuenta una específica o COLORLESS
    // 4. Retorna true si no faltan energías
};
```

Cada ataque se renderiza como un botón con:
- Nombre del ataque
- Daño base (si tiene)
- Iconos de costo de energía (mediante `EnergyIconPipe`)
- Color de fondo según el tipo del Pokémon (ej. GRASS=verde, FIRE=rojo)
- Tooltip con el texto del ataque al hover

Los ataques no disponibles (costo de energía insuficiente) aparecen atenuados.

## Selección de Objetivo

### Ataque al Activo del Oponente

Cuando el jugador hace clic en un ataque (`onAttackClicked`), se prepara `pendingAttack` con:

```typescript
this.pendingAttack.set({
    attackIndex,
    attackName: attackDef.name,
    cost: attackDef.cost ?? [],
    targetId: opponentActive?.instanceId,  // Objetivo por defecto: activo del oponente
    conditionOptions,   // Opciones de condición especial (si el texto del ataque permite elegir)
    chosenCondition,    // Condición seleccionada
});
```

### Ataques con Efectos Adicionales

Al confirmar (`onConfirmAttack`), se analiza el texto del ataque con expresiones regulares para detectar efectos adicionales que requieren interacción del usuario:

| Efecto | Expresión regular | UI generada |
|--------|-------------------|-------------|
| Daño a banca | `/damage to .+? benched/i` | Panel de selección de objetivos en banca |
| Curación a banca | `/heal.*benched/i` | Panel de selección de Pokémon a curar |
| Descarte de energía del rival | `/discard.*(energy\|energies).*(opponent\|defending\|defender\|their\|active)/i` | Panel de selección de energías a descartar |
| Autodescarte de energía | `/discard.*(energy\|energies).*(this\|attached to this)/i` | Panel de selección de energías propias |
| Switch | `/switch this pokémon/i` o `/opponent switches/i` | Panel de selección de nuevo activo |
| Mover energía | `/move (an )?energy/i` | Panel de selección de energía + destino |
| Bonus opcional | `/you may (do\|discard).*more damage/i` | Panel de confirmación de bonus |
| Restricción de ataque | `/choose 1 of (your opponent's) active.*can't use/i` | Panel de selección de ataque a bloquear |

Cada uno de estos efectos abre un panel en la parte inferior de la pantalla (barra fija) con sus respectivos botones de selección, cancelar y confirmar.

### Panel de Confirmación de Ataque

Una vez resueltos todos los sub-efectos, se muestra un panel de confirmación con:
- Nombre del ataque
- Costo de energía
- Botones "Cancelar" y "Confirmar"
- Si el ataque permite elegir condición especial (ej. Dormir/Envenenar), se muestran botones de opción

## Ejecución del Ataque

### doDispatchAttack

Cuando se confirma, `doDispatchAttack()` construye el payload y lo envía:

```typescript
const payload = {
    attackIndex,
    targetPokemonInstanceId: targetId,
    benchTargets,           // Objetivos de banca
    healTargetId,           // Objetivo de curación
    energyCardInstanceIdsToDiscard,  // Energías a descartar
    energyCardInstanceIdsToMove,     // Energías a mover
    useOptionalBonus,       // Bonus opcional
    restrictedAttackName,   // Ataque restringido
    specialCondition,       // Condición especial elegida
};
```

### Feedback Visual

- **Screen shake**: `_screenShake` se activa por 500ms.
- **Attack flash**: Un gradiente radial del color del tipo de Pokémon aparece en toda la pantalla (`_attackFlash`).
- **Sonido**: Se reproduce el sonido del tipo de Pokémon mediante `AudioService.playTypeSound()`.
- **Estado de selección**: Se limpia `pendingAttack` y todas las señales de sub-efectos.

## PokemonSlotComponent

`FE/src/app/features/match/components/pokemon-slot/pokemon-slot.component.ts`

Cada Pokémon en el campo se renderiza con este componente. Muestra:

- Imagen de la carta (con hover para zoom)
- HP actual/máximo con barra de vida (verde > 50%, amarillo 25-50%, rojo ≤ 25%)
- Energías adjuntas agrupadas por tipo con contador y tooltip
- Tool equipada (icono y nombre)
- Condiciones especiales (iconos)
- **Damage popup**: Animación flotante de daño (+N) cuando se recibe daño
- **Debilidad badges**: Animación "badge-pop" que muestra el multiplicador de debilidad
- **Resistencia badges**: Similar para resistencia
- **Evolution flash**: Animación de partículas violetas al evolucionar

## Game Event Formatter

`FE/src/app/features/match/utils/game-event-formatter.ts`

Convierte eventos crudos del backend (`GameEventDto`) en entradas de log legibles. Para ataques, maneja:

- `ATTACK_DECLARED`: "Usaste [ataque]" / "El oponente usó [ataque]"
- `DAMAGE_APPLIED`: Desglose detallado de daño base, debilidad (×2) y resistencia (-20)
- `CONFUSION_SELF_HIT`: Daño por autogolpe de confusión
- `ATTACK_EFFECT_RESOLVED`: Efectos como condiciones especiales, descarte de energía, daño a banca, retroceso, curación, switch
- `ATTACK_CANCELED`: Mensaje según la razón (Dormido, Paralizado)
- `RECOIL_OCCURRED`: Daño por retroceso
- `KNOCKOUT_OCCURRED`: "¡Debilitaste a un Pokémon del oponente!" / "¡Debilitaron a tu Pokémon!"
