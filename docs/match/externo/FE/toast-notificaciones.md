# Toast / Notificaciones

## Ubicación
- Componente: `FE/src/app/features/match/components/toast-container/toast-container.component.ts`
- Servicio: `FE/src/app/shared/services/toast.service.ts`

## Propósito
Sistema de notificaciones temporales (toasts) que aparecen en la esquina superior derecha durante la partida para informar eventos importantes con un código de colores según el tipo.

## Arquitectura

### Servicio `ToastService` (provisto en root)
```typescript
interface ToastMessage {
  id: string;
  text: string;
  type: 'hostile' | 'reward' | 'heal' | 'energy' | 'info';
}
```

**API**:
| Método | Descripción |
|---|---|
| `show(text, type='info', durationMs=3000)` | Agrega un toast a la cola con ID único (`crypto.randomUUID()`). Se auto-remueve después de `durationMs` milisegundos. |
| `clear()` | Vacía todos los toasts inmediatamente. |

El estado se maneja con una **signal** `_toasts` (arreglo de `ToastMessage[]`), expuesta como readonly `toasts`.

### Componente `ToastContainerComponent`
- **Selector**: `app-toast-container`
- **Posición**: `fixed top-20 right-4`, columna vertical con gap.
- **Pointer events**: el contenedor tiene `pointer-events-none` y cada toast individual tiene `pointer-events-auto`.

**Colores según tipo**:
| Tipo | Clase | Color |
|---|---|---|
| `hostile` | `bg-red-600` | Rojo — daño, KOs, eventos negativos |
| `reward` | `bg-amber-500` | Ámbar — premios, victoria |
| `heal` | `bg-green-600` | Verde — curaciones |
| `energy` | `bg-blue-500` | Azul — energía |
| `info` | `bg-gray-700` | Gris — información general |

**Animación**: Los toasts entran con `toast-slide-in 0.3s ease-out` (animación definida globalmente).

## Flujo de uso
1. Cualquier servicio o componente del módulo de partida llama a `toastService.show(text, type)`.
2. El servicio agrega el toast a la señal `_toasts` con un ID único.
3. El componente `ToastContainerComponent` itera sobre `toastService.toasts()` y renderiza cada uno.
4. Después de `durationMs` (default 3000ms), el servicio elimina el toast automáticamente.
5. El toast puede cerrarse manualmente si se agrega lógica de botón de cierre.

## Dependencias
- `ToastService` — servicio singleton inyectable en toda la app.
- Sin dependencias externas — usa `crypto.randomUUID()` para IDs únicos.
