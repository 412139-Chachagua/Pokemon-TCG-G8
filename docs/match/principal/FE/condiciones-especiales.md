# Condiciones Especiales

## Vista General

Las condiciones especiales son estados alterados que pueden afectar a los Pokémon durante la partida. El frontend las visualiza mediante iconos en **PokemonSlotComponent**, el **ConditionIconPipe** para resolver las rutas de los iconos, y tintes de color de fondo en el slot del Pokémon.

## Tipos de Condiciones Especiales

| Condición | Clave | Traducción |
|-----------|-------|------------|
| Dormido | `ASLEEP` | Dormido |
| Quemado | `BURNED` | Quemado |
| Confundido | `CONFUSED` | Confundido |
| Paralizado | `PARALYZED` | Paralizado |
| Envenenado | `POISONED` | Envenenado |

## ConditionIconPipe

`FE/src/app/shared/pipes/condition-icon.pipe.ts`

Pipe Angular simple que transforma una clave de condición en la ruta de su icono SVG:

```typescript
transform(condition: string): string {
    return `assets/icons/conditions/condition-${condition.toLowerCase()}.svg`;
}
```

Ejemplos de rutas generadas:
- `condition-asleep.svg`
- `condition-burned.svg`
- `condition-confused.svg`
- `condition-paralyzed.svg`
- `condition-poisoned.svg`

Los archivos SVG deben estar ubicados en `FE/src/app/assets/icons/conditions/`. El componente `PokemonSlotComponent` importa el pipe y lo usa directamente en el template.

## Visualización en PokemonSlotComponent

`FE/src/app/features/match/components/pokemon-slot/pokemon-slot.component.ts`

### Iconos de Condiciones (Vista Activo)

En la vista expandida del Pokémon activo:

```html
<div class="flex gap-1 mt-0.5 flex-wrap">
    @for (condition of pokemon().specialConditions; track $index) {
        <img [src]="condition | conditionIcon" alt="{{ condition }}" class="w-4 h-4" />
    }
</div>
```

Cada condición se renderiza como un icono de 16×16px al lado del nombre y HP del Pokémon.

### Tinte de Color de Fondo (ConditionTint)

Además de los iconos, se aplica un tinte de color semitransparente sobre toda la carta del Pokémon para indicar visualmente la condición principal:

```typescript
readonly conditionTint = computed(() => {
    const conditions = this.pokemon().specialConditions;
    if (!conditions || conditions.length === 0) return null;
    switch (conditions[0]) {  // Se usa la primera condición
        case 'ASLEEP':    return 'bg-sky-400/25';     // Azul cielo
        case 'BURNED':    return 'bg-orange-500/30';  // Naranja
        case 'CONFUSED':  return 'bg-purple-500/25';  // Púrpura
        case 'PARALYZED': return 'bg-yellow-400/25';  // Amarillo
        case 'POISONED':  return 'bg-green-600/30';   // Verde
    }
});
```

```html
@if (conditionTint(); as tint) {
    <div class="absolute inset-0 rounded-lg pointer-events-none z-5"
         [class.bg-sky-400/25]="tint === 'bg-sky-400/25'"
         [class.bg-orange-500/30]="tint === 'bg-orange-500/30'"
         ...>
    </div>
}
```

### Efectos Visuales Adicionales por Condición

#### Dormido (ASLEEP)

- Bloquea retirada (verificando `active.specialConditions?.includes('ASLEEP')` en `retreatInfo`).
- Bloquea ataque (verificado en `activeAbilityInfo` y en el backend con `ATTACK_CANCELED`).
- Al inicio del turno, el Pokémon puede despertar mediante un coin flip (`COIN_FLIP_RESULT` con `source: 'sleep_check'`):
  - Cara → despierta, toast "¡El Pokémon despertó!"
  - Cruz → sigue dormido, toast "El Pokémon sigue dormido..."

#### Paralizado (PARALYZED)

- Bloquea retirada y ataque, similar a Dormido.
- El Pokémon se recupera automáticamente después de un turno.

#### Confundido (CONFUSED) — Pánico Mental

- Al atacar, se lanza un coin flip (`source: 'mental_panic'`):
  - Cara → ataca normalmente, toast "El Pokémon superó el Pánico Mental."
  - Cruz → se autogolpea (`CONFUSION_SELF_HIT`), toast "¡Se golpeó a sí mismo por confusión!"

#### Quemado (BURNED)

- Entre turnos, el Pokémon recibe daño.

#### Envenenado (POISONED)

- Entre turnos, el Pokémon recibe daño.

## Toast Notifications

En `MatchStateService.addEvent()`, cuando se aplica o remueve una condición:

```typescript
if (event.type === 'STATUS_APPLIED' && event.payload?.['blocked'] !== true) {
    const condition = event.payload?.['condition'];
    if (condition) {
        const translated = { ASLEEP: 'Dormido', BURNED: 'Quemado', ... };
        this.toastService.show(`¡${translated[condition] ?? condition}!`, 'hostile', 3000);
    }
}

if (event.type === 'STATUS_REMOVED') {
    const cond = event.payload?.['condition'];
    if (cond) {
        this.toastService.show(`${translated[cond] ?? cond} removido`, 'heal', 3000);
    }
}
```

## Game Event Formatter

Eventos relacionados con condiciones especiales:

- `STATUS_APPLIED`: Traduce la condición (ej. "Dormido aplicado", "Condición bloqueada: Dormido" si `blocked` es `true`).
- `STATUS_REMOVED`: "Dormido removido".
- `CONFUSION_SELF_HIT`: "Se golpeó a sí mismo por confusión: [daño] de daño".
- `ATTACK_CANCELED` con `reason: 'asleep'`: "El Pokémon está Dormido y no puede atacar".
- `ATTACK_CANCELED` con `reason: 'paralyzed'`: "El Pokémon está Paralizado y no puede atacar".

## Coin Flip Display

En `MatchPage`, cuando hay un coin flip por condición (sleep check o mental panic), se muestra un overlay animado:

```html
@if (matchState.attackCoinFlip(); as result) {
    <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-[200] ... animate-fade-in">
        <div class="w-12 h-12 rounded-full ...">C/S</div>
        <span>{{ result === 'HEADS' ? '¡Cara!' : 'Cruz' }}</span>
    </div>
}
```

## Iconos en Assets

Los archivos SVG de condiciones deben seguir el patrón de nombres:
```
FE/src/app/assets/icons/conditions/
├── condition-asleep.svg
├── condition-burned.svg
├── condition-confused.svg
├── condition-paralyzed.svg
└── condition-poisoned.svg
```

Cada SVG debe ser un icono reconocible que represente la condición (ej. Z's para dormido, llama para quemado, etc.).
