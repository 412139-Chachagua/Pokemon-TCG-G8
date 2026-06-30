# Energy Selector Dialog (Selector de Energía)

## Ubicación
`FE/src/app/features/match/components/energy-selector-dialog/energy-selector-dialog.component.ts`

## Propósito
Diálogo modal que permite al jugador **seleccionar energías específicas para descartar** del Pokémon rival. Se usa cuando un efecto de ataque o habilidad requiere descartar una cantidad determinada de energías del oponente.

## Arquitectura
Componente standalone con estado interno mínimo mediante señales.

### Inputs
| Input | Tipo | Descripción |
|---|---|---|
| `pokemonInstanceId` | `string` | ID de instancia del Pokémon objetivo (requerido) |
| `energies` | `EnergyOption[]` | Lista de energías adheridas al Pokémon (cada una con `instanceId` y `cardId`) |
| `count` | `number` | Cantidad de energías que el jugador debe seleccionar para descartar |

### Outputs
| Output | Tipo | Descripción |
|---|---|---|
| `confirmed` | `string[]` | Arreglo de `instanceId` de las energías seleccionadas |
| `cancelled` | `void` | Se emite si el jugador cancela la selección |

### Interfaz `EnergyOption`
```typescript
interface EnergyOption {
  instanceId: string;
  cardId: string;
}
```

## Funcionamiento
1. El diálogo se abre cuando un componente padre lo necesita, pasándole las energías disponibles y la cantidad a descartar.
2. El jugador ve una grilla con las imágenes de cada energía adjunta.
3. Puede hacer clic en las energías para marcarlas/desmarcarlas (checkboxes ocultos visualmente, reemplazados por borde `border-color: #fbbf24` en las seleccionadas).
4. Las casillas ya no disponibles (cuando se alcanzó el máximo `count`) se deshabilitan.
5. El botón "Confirmar" se habilita solo cuando `selectedIds().size === count()`.
6. Al confirmar, emite `confirmed` con los IDs de instancia seleccionados.
7. Al cancelar (clic en backdrop o botón Cancelar), emite `cancelled`.

## Visual
- Overlay semitransparente con z-index 1000.
- Panel oscuro (`bg: #1e293b`) con borde `#334155`.
- Título: "Seleccionar energías para descartar".
- Hint: "Elegí N energía(s) para descartar del Pokémon rival".
- Grilla flexible de energías con imágenes de 50px.
- Botón "Cancelar" (rojo, borde rojo) y "Confirmar" (azul, borde azul).
- El botón Confirmar muestra contador: `"Confirmar (X/N)"`.

## Dependencias
- `CardImagePipe` — muestra la imagen de cada carta de energía.
- Sin dependencias de servicios — componente puramente UI con inputs/outputs.
