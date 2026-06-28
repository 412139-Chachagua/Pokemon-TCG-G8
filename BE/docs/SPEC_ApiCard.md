# SPEC - Implementación de Catálogo de Cartas y Sincronización con API Externa

## Objetivo
Implementar el módulo de catálogo de cartas y sincronización con la API externa de Pokémon TCG, permitiendo:
- Consumir la API de Pokémon TCG v2 para obtener datos oficiales de cartas
- Sincronizar y almacenar cartas en la base de datos local
- Proveer un servicio de consulta para acceder al catálogo local de cartas
- Exponer endpoints REST para el catálogo de cartas
- Implementar el adaptador que permite al engine acceder al catálogo mediante CardLookupPort

## Tipo de cambio
- [x] Nueva funcionalidad
- [ ] Refactor
- [x] Integración externa (API de Pokémon TCG)
- [ ] Cambio breaking
- [ ] Optimización

## Estado actual
Actualmente el sistema cuenta con:
- Un stub de `PokemonTcgApiClient` que solo obtiene cartas del set XY1 filtradas por tipo
- Un stub de `CardCacheSyncService` que sincroniza cartas pero usa DTOs internos no alineados con los contratos
- Entidades JPA existentes (`PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity` y sus relaciones) pero con inconsistencias respecto al modelo canónico
- Repositorios JPA existentes para estas entidades
- El contrato del modelo de carta definido en `04-card-model-contract.md`
- El contrato de API REST definido en `13-rest-api-contract.md`
- La división de trabajo establecida en `divisionCatalogo.md` (Persona A)

## Resultado esperado
Luego de la implementación el sistema deberá:
1. Sincronizar cartas desde la API externa de Pokémon TCG siguiendo las mejores prácticas de paginación y manejo de errores
2. Almacenar las cartas en la base de datos usando entidades JPA alineadas con el modelo canónico del contrato
3. Proveer un servicio de consulta eficiente para buscar cartas por diversos criterios (id, nombre, supertype, tipo, set, stage)
4. Exponer los endpoints REST especificados en el contrato para el catálogo de cartas
5. Implementar `CardLookupAdapter` que sirva como puente entre el engine y el catálogo local
6. Mantener todo el código siguiendo las restricciones arquitectónicas y respetando los contratos existentes
7. No introducir lógica de negocio en los controllers
8. Reutilizar mappers existentes cuando sea posible y crear nuevos mappers necesarios

## Restricciones arquitectónicas
- No crear lógica de negocio en controllers
- Toda lógica debe ir en services
- No usar lógica duplicada
- Reutilizar mappers existentes
- Mantener compatibilidad con DTOs actuales especificados en los contratos
- No modificar contratos públicos existentes (actualizarlos si es necesario a través del proceso adecuado)
- No usar herencia salvo clases ya existentes
- Seguir estructura package-by-feature
- No introducir frameworks nuevos
- Mantener estilo builder/lombok existente
- Todas las clases deben estar en los paquetes especificados para Persona A en `divisionCatalogo.md`
- El `CardLookupAdapter` debe implementar exactamente `CardLookupPort` sin modificar la interfaz
- El caching debe usarse adecuadamente en `CardLookupAdapter` para evitar consultas repetidas a BD

## Clases existentes relevantes

### PokemonTcgApiClient
Responsable de:
- Consumir la API de Pokémon TCG
- Mapear responses a DTOs internos

Métodos relevantes:
- fetchPokemonCards()
- fetchTrainerCards()
- fetchEnergyCards()
- fetchCardsByType()
- mapApiCardToDto() y métodos auxiliares de mapeo

No modificar:
- La estructura básica de llamadas a RestTemplate (pero sí mejorar el uso)

### CardCacheSyncService
Responsable de:
- Sincronizar cartas desde API externa a BD local
- Mapear DTOs a entidades JPA

Métodos relevantes:
- synchronizeAllCards()
- syncPokemonCards(), syncTrainerCards(), syncEnergyCards()
- mapToPokemonCardEntity(), mapToTrainerCardEntity(), mapToEnergyCardEntity()
- Métodos auxiliares de mapeo para relaciones (ataques, debilidades, resistencias)

No modificar:
- El enfoque general de sincronización por tipo de carta

## Invariantes
- El id de la carta debe coincidir exactamente con el id provisto por la API externa
- El setCode debe ser "xy1" para todas las cartas sincronizadas (según requerimiento inicial)
- Las entidades JPA deben mantener relaciones consistentes (p.ej., un ataque debe pertenecer exactamente a una carta)
- Los datos sincronizados deben ser de solo lectura desde la perspectiva del engine (no se modifican durante partidas)
- El mapeo entre API externa → DTOs internos → Entidades JPA → Modelos de dominio debe preservar toda la información requerida por los contratos
- El CardLookupAdapter nunca debe devolver null para una carta que exista en el catálogo (debe lanzar excepción apropiada si no se encuentra)

## Flujo esperado
1. Al iniciar la aplicación, `CardCacheSyncService.synchronizeAllCards()` se ejecuta vía @PostConstruct
2. Para cada tipo de carta (pokemon, trainer, energy):
   a. `PokemonTcgApiClient.fetch*Cards()` obtiene cartas de la API externa con paginación
   b. Cada respuesta de API se mapea a `PokemonTcgApiCardRequest` DTO
   c. Cada DTO se mapea a la entidad JPA correspondiente mediante los métodos mapTo*Entity
   d. Las entidades se guardan en sus respectivos repositorios JPA
   e. Las relaciones (ataques, debilidades, resistencias) se guardan en cascada
3. Cuando se consulta el catálogo mediante REST:
   a. Los requests llegan a `CardController`
   b. El controller deleita a `CardCatalogService` para realizar las consultas
   c. El service usa los repositorios JPA con filtros apropiados
   d. Los resultados se mapean a DTOs de respuesta mediante mappers
   e. Se devuelven los DTOs como JSON en el response
4. Cuando el engine necesita una carta:
   a. Llama a `CardLookupPort.getCardById()` 
   b. La implementación en `CardLookupAdapter` consulta `CardJpaRepository`
   c. El resultado se mapea a `CardDefinition` (o subclase apropiada) mediante `CardMapper`
   d. Se devuelve el modelo de dominio al engine

## Casos borde
- API externa no disponible o retornando errores HTTP
- Respuestas de API con campos nulos o faltantes
- Cartas con supertypes no contemplados en el modelo inicial
- Duplicados durante sincronización (deberían hacerse upsert por id)
- Problemas de transaccionalidad al guardar cartas con muchas relaciones
- Consultas de catálogo que no retornan resultados
- Solicitudes para cartas que no existen en el catálogo
- Problemas de paginación al consumir la API externa
- Rate limiting de la API externa
- Cambios en la estructura de la API externa de Pokémon TCG

## Prohibiciones
- No modificar las interfaces de los puertos (`CardLookupPort`)
- No crear lógica de mapeo compleja en los controllers
- No usar entidades JPA directamente en los contracts de API (solo DTOs)
- No almacenar datos calculados que puedan derivarse de otros campos
- No introducir estado compartido entre servicios que dificulte la escalabilidad
- No usar bloqueos sincronizados que afecten el rendimiento
- No hacer llamadas HTTP síncronas lentas en el hilo principal sin manejo apropiado
- No exponer información interna de la API externa más allá de lo necesario
- No crear DTOs duplicados cuando existen en los contracts

## Archivos permitidos
Puede modificar:
- PokemonTcgApiClient (mejorar implementación existente)
- CardCacheSyncService (mejorar implementación existente)

Puede crear:
- PokemonTcgApiResponse (wrapper para deserializar response de API)
- CardCatalogService (nuevo servicio)
- CardController (nuevo controller)
- CardMapper (nuevo mapper)
- CardLookupAdapter (mejorar/implementar stub existente)
- DTOs necesarios si no existen ya (CardDetailResponse, CardSummaryResponse, CardSearchRequest)
- Cualquier clase de configuración necesaria (para caching, etc.)

NO modificar:
- Entities existentes (pero sí se puede actualizar su alineación con los contratos si es necesario a través del proceso de actualización de contracts)
- BaseEntity (si existe)
- Interfaces de los puertos (CardLookupPort)
- Contratos existentes sin seguir el proceso adecuado

## Ejemplo request
GET /api/cards?query=charizard&supertype=POKEMON&setCode=xy1&page=0&size=20

Response:
```json
{
  "items": [
    {
      "id": "xy1-4",
      "name": "Charizard",
      "supertype": "POKEMON",
      "setCode": "xy1",
      "number": "4",
      "imageSmallUrl": "https://images.pokemontcg.io/xy1/4.png",
      "imageLargeUrl": "https://images.pokemontcg.io/xy1/4_hires.png",
      "hp": 160,
      "stage": "STAGE_2",
      "types": ["FIRE"],
      "attacks": [
        {
          "index": 0,
          "name": "Seismic Toss",
          "cost": ["FIGHTING", "FIGHTING"],
          "damage": "20",
          "text": ""
        },
        {
          "index": 1,
          "name": "Fire Spin",
          "cost": ["FIRE", "FIRE", "FIRE", "COLORLESS"],
          "damage": "100",
          "text": ""
        }
      ],
      "weaknesses": [
        {
          "type": "WATER",
          "value": "x2"
        }
      ],
      "resistances": [],
      "retreatCost": ["COLORLESS", "COLORLESS"],
      "isEx": false,
      "isMega": false
    }
  ],
  "page": 0,
  "size": 20,
  "totalItems": 1
}
```

## Definition of Done
- [ ] PokemonTcgApiClient consume correctamente la API de Pokémon TCG v2 con paginación
- [ ] PokemonTcgApiClient implementa manejo de errores HTTP apropiado
- [ ] PokemonTcgApiClient implementa rate limiting básico
- [ ] PokemonTcgApiResponse existe y se usa para deserializar responses de API
- [ ] CardCacheSyncService realiza upsert por cardId al sincronizar
- [ ] CardCacheSyncService se ejecuta automáticamente al inicio de la aplicación
- [ ] CardCacheSyncService puede ser gatillado manualmente vía endpoint
- [ ] Las entidades JPA están alineadas con el modelo canónico del contrato (04-card-model-contract.md)
- [ ] CardCatalogService implementa todos los métodos de búsqueda requeridos
- [ ] CardController expone los endpoints REST según el contrato (13-rest-api-contract.md)
- [ ] CardMapper existe y mapea correctamente entre todas las capas
- [ ] CardLookupAdapter implementa CardLookupPort.getCardById() con @Cacheable
- [ ] Todos los endpoints REST están documentados en Swagger/OpenAPI
- [ ] Se han actualizado los contracts si se发现了 inconsistencias durante la implementación
- [ ] Se han corrido y pasado todas las pruebas existentes
- [ ] Se han agregado pruebas para la nueva funcionalidad
- [ ] Se ha verificado que el engine pueda obtener cartas mediante CardLookupPort
- [ ] No se han introducido dependencias de frameworks nuevos
- [ ] Se sigue el estilo de código existente (Lombok, builders, etc.)

## Decisiones ya tomadas
- Usar RestTemplate para llamadas HTTP a la API externa (según implementación existente)
- Sincronizar únicamente cartas del set XY1 como requisito inicial
- Mantener la separación de entidades por tipo de carta (PokemonCardEntity, TrainerCardEntity, EnergyCardEntity) por ahora
- Usar el mismo enfoque de mapeo que existía pero actualizado para cumplir con los contratos
- Implementar sincronización batch completa en lugar de incremental para la versión inicial
- Usar los repositorios JPA existentes en lugar de crear nuevos
- No almacenar el JSON raw de la API externa en las entidades (a menos que el contract lo requiera)
- Mapear las relaciones uno-a-muchos (ataques, debilidades, resistencias) usando listas en lugar de CSV en las entidades