# Sincronización con API Externa de Pokémon TCG

## Descripción General

El sistema sincroniza cartas desde la [Pokémon TCG API](https://docs.pokemontcg.io/) hacia la base de datos local. La sincronización se realiza sobre el set XY1 (XY Base Set) y se ejecuta tanto al iniciar la aplicación como de forma programada diariamente.

---

## PokemonTcgApiClient

Cliente HTTP que se comunica con `https://api.pokemontcg.io/v2/cards`.

### Configuración

```yaml
BASE_URL: "https://api.pokemontcg.io/v2/cards"
PAGE_SIZE: 50
MAX_RETRIES: 3
INITIAL_BACKOFF_MS: 1000
```

### RestTemplateConfig

Configura el `RestTemplate` con:

- **Connect timeout**: 5000ms
- **Read timeout**: 15000ms

### Métodos

#### fetchAllCards()

Obtiene todas las cartas del set XY1.

1. Construye el query: `(supertype:pokemon OR supertype:trainer OR supertype:energy) set.id:xy1`.
2. Obtiene la primera página para determinar `totalCount`.
3. Calcula el número total de páginas: `Math.ceil(totalCount / PAGE_SIZE)`.
4. Itera desde la página 2 hasta `totalPages`, descargando cada página.
5. Retorna la lista completa de `PokemonTcgApiCardDto`.

#### fetchCardById(cardId)

Obtiene una carta individual por su ID. Endpoint: `GET /cards/{id}`.

#### fetchPageWithRetry(url, pageNumber, failedPages)

Mecanismo de reintento con backoff exponencial:

1. **Hasta 3 intentos** por página.
2. **Backoff exponencial**: 1s → 2s → 4s.
3. Si todos los intentos fallan, registra la página en `failedPages` y retorna `null` (no detiene el proceso).

### Rate Limiting

El mecanismo de rate limiting se implementa mediante:

- **Backoff exponencial** entre reintentos (1000ms, 2000ms, 4000ms).
- **Sleep entre reintentos fallidos** para no saturar la API.
- **Timeout de conexión** de 5s y **timeout de lectura** de 15s configurados en `RestTemplateConfig`.

---

## DTOs de la API Externa

### PokemonTcgApiCardDto

```java
public record PokemonTcgApiCardDto(
    String id,
    String name,
    String supertype,
    List<String> subtypes,
    String hp,
    List<String> types,
    List<String> rules,
    String evolvesFrom,
    List<String> evolvesTo,
    List<AbilityDto> abilities,
    List<AttackDto> attacks,
    List<WeaknessDto> weaknesses,
    List<ResistanceDto> resistances,
    List<String> retreatCost,
    Integer convertedRetreatCost,
    SetInfoDto set,
    ImagesDto images,
    String rarity
) {}
```

DTOs anidados:

| DTO | Campos |
|-----|--------|
| `AbilityDto` | `name`, `text`, `type` |
| `AttackDto` | `index`, `name`, `cost`, `convertedEnergyCost`, `damage`, `text`, `baseDamage` |
| `WeaknessDto` | `type`, `value` |
| `ResistanceDto` | `type`, `value` |
| `SetInfoDto` | `id` |
| `ImagesDto` | `small`, `large` |

### PokemonTcgApiResponse

Envoltura de respuesta paginada:

```java
class PokemonTcgApiResponse {
    List<PokemonTcgApiCardDto> data;
    int totalCount;
}
```

### PokemonTcgApiSingleCardResponse

Envoltura para respuesta de carta individual:

```java
class PokemonTcgApiSingleCardResponse {
    PokemonTcgApiCardDto data;
}
```

---

## CardCacheSyncService

Servicio que orquesta el proceso de sincronización de cartas.

### Sincronización al Inicio (`ApplicationReadyEvent`)

```java
@EventListener(ApplicationReadyEvent.class)
@Order(2)
public void synchronizeAllCards()
```

1. Verifica si ya hay cartas en la BD (`cardJpaRepository.count() > 0`).
2. Si hay cartas, skipea la sincronización.
3. Si no hay cartas, ejecuta `syncAll()` de forma asíncrona con `CompletableFuture.runAsync()`.

### Sincronización Programada (`@Scheduled`)

```java
@Scheduled(fixedRate = 86400000)  // 24 horas
public void scheduledSync()
```

Ejecuta `syncAll()` cada 24 horas.

### syncAll()

Proceso completo:

1. Llama a `PokemonTcgApiClient.fetchAllCards()` para obtener todas las cartas del set XY1.
2. Para cada carta, mapea el DTO a entidad (`CardMapper.toCardEntity()`) y la guarda en BD.
3. Si la carta ya existe (`existsById`), la actualiza (cuenta como `updated`); si no, la crea (cuenta como `new`).
4. Al finalizar, invalida la caché de Spring (`cacheManager.getCache("cards").clear()`).
5. Retorna un `CardSyncResponse` con el resumen.

### syncCardById(cardId)

Sincroniza una carta individual:

1. Llama a `fetchCardById(cardId)`.
2. Si la carta no existe en la API externa, retorna `false`.
3. Mapea y guarda la entidad.
4. Invalida la entrada específica en caché (`evictIfPresent`).

### CardSyncResponse

```java
public record CardSyncResponse(
    boolean success,
    String message,
    int newCards,
    int updatedCards
) {}
```

---

## CacheConfig

Configuración de caché en memoria:

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("cards");
    }
}
```

Utiliza `ConcurrentMapCacheManager` de Spring con el nombre de caché `"cards"`. La caché se invalida completamente después de cada sincronización masiva (`syncAll()`) o por entrada individual tras `syncCardById()`.

---

## AdminController

Controlador REST expuesto solo en el perfil `dev`. Opera bajo la ruta base `/api/admin`.

### Endpoints

#### POST /api/admin/regenerate-effect-codes

Regenera los códigos de efecto de todos los ataques de todas las cartas.

1. Obtiene todas las cartas de la BD.
2. Para cada ataque, parsea el `effectText` usando `TextEffectParser.parse()`.
3. Genera el nuevo `effectCode` concatenando los efectos con `;;`.
4. Actualiza solo si el código cambió.
5. Retorna estadísticas: total de ataques procesados, actualizados, saltados y errores.

---

## Flujo Completo de Población/Actualización

```
Inicio de app ──> ApplicationReadyEvent
                     │
                     v
               ¿cards.count() > 0?
                     │
          ┌──────────┴──────────┐
          │ SI (skip)           │ NO
          v                     v
       (no op)           CompletableFuture.runAsync ──> syncAll()
                                                           │
                    Programación diaria                    v
                    @Scheduled(24h) ──> syncAll() ──> fetchAllCards()
                                                           │
                                                           v
                                              API Pokemon TCG (50 por página)
                                                           │
                                                           v
                                              CardMapper.toCardEntity()
                                                           │
                                                           v
                                              CardJpaRepository.save()
                                                           │
                                                           v
                                              Invalida caché "cards"
                                                           │
                                                           v
                                              CardSyncResponse
```
