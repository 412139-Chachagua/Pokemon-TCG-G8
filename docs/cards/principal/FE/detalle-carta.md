# Detalle de Carta

## Vista General

La página de detalle (`CardDetailPage`) muestra la información completa de una carta individual. Se accede mediante la ruta `/cards/:id`.

La página es cargada de forma lazy mediante `loadComponent` en `routes.ts`.

---

## CardDetailPage

### Estructura

El template renderiza condicionalmente:

1. **Cargando** (`loading()`): muestra `LoadingSpinnerComponent`.
2. **Error** (`error()`): muestra mensaje de error y botón "Reintentar" que llama a `loadCard()`.
3. **Carta encontrada** (`card()`): renderiza `PokemonCardComponent` con el detalle completo.
4. **No encontrada**: muestra mensaje "Carta no encontrada".

### Flujo de carga

1. En el constructor se ejecuta `loadCard()`.
2. `loadCard()` obtiene el `id` del parámetro de ruta `:id`.
3. Si no hay id, establece error y retorna.
4. Llama a `CardRepositoryService.resolve(id)` (método asíncrono).
5. En éxito: asigna la carta a la señal `card()`.
6. En error: asigna mensaje de error.

---

## CardRepositoryService (resolución por ID)

El método `resolve(cardId)` implementa tres caminos:

1. **Caché**: si la carta ya está en el `Map` interno, la retorna inmediatamente como `Promise` resuelta.
2. **Espera concurrente**: si otra llamada está cargando el mismo ID, espera con `waitForLoad()` (polling cada 50ms) hasta que esté disponible.
3. **Fetch**: llama a `CardApiService.getCardById(cardId)`, guarda la respuesta en el `Map` de caché y la retorna.

Previene llamadas duplicadas a la API para la misma carta.

---

## CardApiService (getCardById)

```typescript
getCardById(cardId: string): Observable<CardDetailResponse> {
  return this.apiClient.get<CardDetailResponse>(`/cards/${cardId}`);
}
```

Endpoint: `GET /cards/{id}`. Retorna un `CardDetailResponse`.

---

## Modelo CardDetailResponse

```typescript
interface CardDetailResponse {
  id: string;
  name: string;
  supertype: string;
  subtypes: string[];
  setCode: string;
  number: string;
  imageSmallUrl: string | null;
  imageLargeUrl: string | null;
  rulesText: string[];
  hp?: number;
  stage?: string;
  evolvesFrom?: string;
  types?: string[];
  attacks?: AttackModel[];
  weaknesses?: WeaknessModel[];
  resistances?: ResistanceModel[];
  retreatCost?: string[];
  isEx?: boolean;
  isMega?: boolean;
  abilities?: CardAbilityResponse[];
  providesEnergyTypes?: string[];
}
```

Modelos anidados:

```typescript
interface AttackModel {
  index: number;
  name: string;
  cost: EnergyType[];
  convertedEnergyCost: number;
  damage: string;
  text: string;
}

interface WeaknessModel {
  type: EnergyType;
  value: string;
}

interface ResistanceModel {
  type: EnergyType;
  value: string;
}

interface CardAbilityResponse {
  name: string;
  text: string;
  type: string;
  isActivable: boolean;
}
```

---

## Información Desplegada

### PokémonCardComponent

El componente `PokemonCardComponent` recibe un `CardDetailResponse` y despliega:

| Sección | Contenido |
|---------|-----------|
| **Imagen** | Imagen grande vía `CardImagePipe` con tamaño `large` |
| **Nombre y HP** | Nombre en negrita; HP en badge rojo (solo si es Pokémon) |
| **Supertipo y Etapa** | Supertipo textual. Si es Pokémon: badge de etapa con color (verde Básico, azul Stage 1, violeta Stage 2, rojo MEGA). Si es Entrenador: badge del subtipo (azul ITEM, amarillo SUPPORTER, verde STADIUM, violeta POKEMON_TOOL, rosa ACE_SPEC) |
| **Tipo** | Etiquetas de tipo de energía con fondo de color |
| **Evoluciona de** | Nombre del Pokémon del que evoluciona |
| **Metadatos** | Código de set, número de carta, subtipos, badges EX (amarillo) y MEGA (violeta) |
| **Reglas** | Texto de reglas en cursiva |
| **Ataques** | Cada ataque: nombre, coste de energía (círculos con abreviaturas de una letra y color de tipo), daño en fuente monospace, texto descriptivo |
| **Debilidades** | Tipo y valor multiplicador (ej. ×2) |
| **Resistencias** | Tipo y valor de reducción (ej. -20) |
| **Retiro** | Coste de retiro como círculos de energía |

---

## CardImagePipe

Pipe transform que construye la URL de la imagen de la carta a partir de su ID.

```typescript
@Pipe({ name: 'cardImage', standalone: true })
class CardImagePipe implements PipeTransform {
  transform(cardId: string, size: 'small' | 'large' = 'small'): string
}
```

El ID de carta sigue el formato `{set}-{number}` (ej. `xy1-1`). El pipe:

1. Divide el ID por el guion para obtener `set` y `number`.
2. Si `size` es `large`, añade el sufijo `_hires`.
3. Construye URL: `https://images.pokemontcg.io/{set}/{number}{_hires}.png`

Uso en templates:
- Catálogo: `card().id | cardImage:'small'`
- Detalle: `card().id | cardImage:'large'`

---

## EnergyIconPipe

Pipe que resuelve la ruta del icono SVG para un tipo de energía.

```typescript
@Pipe({ name: 'energyIcon', standalone: true })
class EnergyIconPipe implements PipeTransform {
  transform(type: string): string
}
```

Pasos de resolución:

1. Si el tipo está en el conjunto conocido de `ENERGY_TYPES` (GRASS, FIRE, WATER, LIGHTNING, PSYCHIC, FIGHTING, DARKNESS, METAL, FAIRY, COLORLESS), retorna `assets/icons/energy/energy-{tipo}.svg`.
2. Si no es un tipo conocido, consulta `CardRepositoryService.getFromCache(type)` por si el parámetro es un ID de carta de energía; si encuentra una carta con tipos, usa el primer tipo.
3. Como fallback, retorna el icono de energía colorless.
