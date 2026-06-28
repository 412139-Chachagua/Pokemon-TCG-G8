# Plan de Implementación: Importación de Cartas Pokémon TCG

## 1. Visión General
Este documento describe el plan para importar cartas del juego Pokémon TCG desde las APIs oficiales hacia la base de datos H2 del proyecto, respetando todas las restricciones establecidas y aprovechando la arquitectura existente del proyecto.

## 2. Capa de Obtención de Datos (Interna únicamente)

### 2.1. Cliente de API Mejorado
**Clase responsable:** `ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient`

**Modificaciones a implementar:**
- Implementar tres métodos específicos para obtener cartas filtradas por supertype y set XY1:
  1. `fetchPokemonCards()` → Consulta: `supertype:pokemon%20set.id:xy1`
  2. `fetchTrainerCards()` → Consulta: `supertype:trainer%20set.id:xy1`  
  3. `fetchEnergyCards()` → Consulta: `supertype:energy%20set.id:xy1`

**Características de implementación:**
- Utilizar `RestTemplate` (o equivalente existente en el proyecto) para llamadas HTTP
- Configurar timeouts y headers apropiados (`Accept: application/json`)
- Manejar respuestas de la API mediante deserialización a DTOs específicos
- Implementar logging detallado para depuración (éxitos, errores, conteos)
- **Restricción crítica:** Este cliente **NO** será expuesto mediante ningún endpoint REST o WebSocket - se usará únicamente internamente por servicios de backend

### 2.2. Especificación de DTOs para Consumo de API

#### DTO Principal: `PokemonTcgCardDto`
Contendrá exactamente los siguientes campos solicitados:

- `id`: string
- `name`: string
- `supertype`: string ("Pokémon", "Trainer", "Energy")
- `subtype`: string[]
- `hp`: Integer (null para Trainer/Energy)
- `types`: string[] (null para Trainer)
- `rules`: List<String> (puede ser null/empty)
- `evolvesFrom`: string (null si no evoluye de nada)
- `evolvesTo`: string[] (empty si no evoluye a nada)

#### DTOs Anidados (estructura exacta solicitada)

**AbilityDto:**
- `name`: string
- `text`: string  
- `type`: string

**AttackDto:**
- `name`: string
- `cost`: string[]
- `convertedEnergyCost`: Integer
- `damage`: string
- `text`: string

**WeaknessDto:**
- `type`: string
- `value`: string

**ResistanceDto:**
- `type`: string
- `value`: string

**SetInfoDto:**
- `id`: string

**ImagesDto:**
- `small`: string (URL imagen pequeña)
- `large`: string (URL imagen grande)

#### Mapeo desde API Pokémon TCG v2
Los campos se mapean directamente desde la respuesta de la API, con estas conversiones clave:
- `hp`: Convertir String de API a Integer
- `convertedEnergyCost` y `convertedRetreatCost`: Ya proporcionados directamente por la API
- Listas vacías en lugar de null para todos los campos coleccionales
- Campos opcionales según supertype (ej. hp=null para Trainer/Energy)

## 3. Manejo de los DTOs

### 3.1. Clase Responsable de la Obtención Inicial
**`PokemonTcgApiClient`** tiene la responsabilidad directa de:
- Ejecutar las llamadas HTTP a las tres endpoints especificadas
- Recibir y parsear la respuesta JSON de la API
- Deserializar los objetos JSON a instancias de `PokemonTcgCardDto` (incluyendo todos los DTOs anidados)
- Devolver listas de DTOs al llamador

### 3.2. Clase Responsable del Procesamiento y Mapeo
**`CardCacheSyncService`** tiene la responsabilidad de:
- Utilizar al `PokemonTcgApiClient` para obtener los DTOs de cada tipo
- Mapear los DTOs recibidos a **entidades JPA completamente nuevas y especializadas** (`PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity`)
- Persistir esas entidades en sus respectivas tablas H2 mediante los repositorios JPA
- Orquestar todo el proceso (manejo de transacciones por tipo, logging detallado, manejo de errores)

### 3.3. Flujo de Manejo de DTOs
1. **Inicialización:** `CardCacheSyncService` se activa al arranque de la aplicación
2. **Obtención:** Llama secuencialmente a los tres métodos de `PokemonTcgApiClient`
3. **Mapeo:** Para cada DTO recibido, convierte a la entidad JPA correspondiente mediante:
    - Mapeo directo de campos comunes
    - Mapeo específico por tipo (Pokémon/Trainer/Energy)
    - Conversión de DTOs anidados a entidades de relación one-to-many (solo para Pokémon)
4. **Persistencia:** Guarda las entidades usando los repositorios especializados en transacciones separadas

## 4. Adaptación del Esquema de Base de Datos

### 4.1. Entidades Especializadas (Totalmente Nuevas - Sin Afectar Otros Módulos)
Crear **tres entidades JPA completamente nuevas**, sin reutilizar ni modificar ninguna clase existente (`CardEntity` permanece intacto para uso en otros módulos del proyecto):

**`PokemonCardEntity`**
- Tabla: `pokemon_cards`
- Campos: 
  * `id` (String, PK) - identificador único de la carta de la API
  * `name` (String) - nombre de la carta
  * `supertype` (String) - siempre "POKEMON" 
  * `subtypes` (String o colección, según implementación) - arreglo de subtipos
  * `hp` (Integer) - puntos de vida (solo para Pokémon)
  * `pokemon_types` (String o colección) - tipos de energía asociados (ej. ["GRASS"])
  * `evolves_from` (String) - nombre de la carta previa evolutiva (nullable)
  * `retreat_cost` (String o colección) - costos de energía para retirarse
  * `converted_retreat_cost` (Integer) - costo convertido de retirada
  * `is_ex` (Boolean) - indica si es carta EX
  * `is_mega` (Boolean) - indica si es carta MEGA
  * `rules_text` (String) - texto de reglas de la carta
  * `image_small_url`, `image_large_url` (String) - URLs de imágenes
  * `raw_json` (String, opcional para trazabilidad completa de la API)
  * Campos de auditoría: `created_at` (Instant), `updated_at` (Instant)
- Relaciones one-to-many (tablas separadas detalladas abajo): ataques, debilidades, resistencias

**`TrainerCardEntity`**
- Tabla: `trainer_cards`
- Campos:
  * `id` (String, PK)
  * `name` (String)
  * `supertype` (String) - siempre "TRAINER"
  * `subtypes` (String/colección)
  * `trainer_subtype` (String) - ej. "SUPPORTER", "STADIUM", "ITEM"
  * `is_ace_spec` (Boolean) - indica si es carta Ace Spec
  * `effect_code` (String) - código de efecto derivado de rules_text
  * `rules_text` (String) - efecto completo de la carta
  * `image_small_url`, `image_large_url` (String)
  * `raw_json` (String, opcional)
  * Campos de auditoría: `created_at`, `updated_at`
- **No posee relaciones one-to-many** (los Trainers no tienen ataques, debilidades ni resistencias según especificación DTO)

**`EnergyCardEntity`**
- Tabla: `energy_cards`
- Campos:
  * `id` (String, PK)
  * `name` (String)
  * `supertype` (String) - siempre "ENERGY"
  * `subtypes` (String/colección)
  * `energy_card_type` (String) - tipo de energía (ej. "BASIC", "SPECIAL")
  * `provides_energy_types` (String/colección) - tipos de energía que provee (ej. ["GRASS"])
  * `image_small_url`, `image_large_url` (String)
  * `raw_json` (String, opcional)
  * Campos de auditoría: `created_at`, `updated_at`
- **No posee relaciones one-to-many** (las Energías no tienen ataques, debilidades ni resistencias)

### 4.2. Entidades de Relación (Solamente para Pokémon Card)
Solamente las cartas Pokémon requieren tablas de relación debido a sus ataques, debilidades y resistencias (las cuales están especificadas en el DTO):

**`PokemonCardAttackEntity`**
- Tabla: `pokemon_card_attacks`
- Campos:
  * `id` (UUID, PK) - identificador único del ataque
  * `pokemon_card_id` (String, FK → pokemon_cards.id) - referencia a la carta Pokémon
  * `attack_index` (Integer) - índice del ataque en la carta
  * `name` (String) - nombre del ataque
  * `printed_cost` (String/colección) - costos de energía tal como aparecen en la carta
  * `converted_energy_cost` (Integer) - costo convertido de energía
  * `damage_text` (String) - texto de daño (ej. "20", "10x")
  * `effect_text` (String) - efecto adicional del ataque
  * `effect_code` (String) - código de efecto estandarizado
  * `created_at` (Instant) - timestamp de creación

**`PokemonCardWeaknessEntity`**
- Tabla: `pokemon_card_weaknesses`
- Campos:
  * `id` (UUID, PK)
  * `pokemon_card_id` (String, FK → pokemon_cards.id)
  * `weakness_type` (String) - tipo de energía a la que es débil (ej. "FIRE")
  * `weakness_value` (String) - multiplicador o valor adicional (ej. "x2", "+20")
  * `created_at` (Instant)

**`PokemonCardResistanceEntity`**
- Tabla: `pokemon_card_resistances`
- Campos:
  * `id` (UUID, PK)
  * `pokemon_card_id` (String, FK → pokemon_cards.id)
  * `resistance_type` (String) - tipo de energía a la que tiene resistencia (ej. "WATER")
  * `resistance_value` (String) - valor de reducción de daño (ej. "-20")
  * `created_at` (Instant)

### 4.3. Repositorios Correspondientes
Crear interfaces de repositorio para **cada una de las seis entidades nuevas** (3 principales + 3 de relación):
- `PokemonCardJpaRepository` extends `JpaRepository<PokemonCardEntity, String>`
- `TrainerCardJpaRepository` extends `JpaRepository<TrainerCardEntity, String>`
- `EnergyCardJpaRepository` extends `JpaRepository<EnergyCardEntity, String>`
- `PokemonCardAttackJpaRepository` extends `JpaRepository<PokemonCardAttackEntity, UUID>`
- `PokemonCardWeaknessJpaRepository` extends `JpaRepository<PokemonCardWeaknessEntity, UUID>`
- `PokemonCardResistanceJpaRepository` extends `JpaRepository<PokemonCardResistanceEntity, UUID>`

## 5. Servicio de Sincronización

### 5.1. Implementación de `CardCacheSyncService`
**Ubicación:** `ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService`

**Funcionalidades clave:**
- **Inyección de dependencias:** 
  * `PokemonTcgApiClient` (para obtener datos de API)
  * Los seis repositorios especializados (tres para entidades principales, tres para relaciones)
  * `Logger` (SLF4J) para trazabilidad detallada
- **Método principal:** `synchronizeAllCards()` que ejecuta en secuencia:
  1. Obtiene cartas Pokémon → persiste en `pokemon_cards` + relaciones en las tres tablas de ataque/debilidad/resistencia
  2. Obtiene cartas Entrenador → persiste en `trainer_cards` (sin relaciones)
  3. Obtiene cartas Energía → persiste en `energy_cards` (sin relaciones)
- **Manejo de transacciones:** Cada tipo de carta (Pokémon, Entrenador, Energía) se procesa en una transacción separada y aislada para evitar que un fallo en un tipo afecte a los otros
- **Logging:** Registro detallado de:
  * Inicio/finalización de sincronización por tipo
  * Número de cartas obtenidas de la API por tipo
  * Número de cartas exitosamente procesadas y fallidas por tipo
  * Errores específicos de mapeo o persistencia con ID de carta para trazabilidad
- **Activación:** Ejecutar automáticamente al iniciar la aplicación usando `@PostConstruct` o escuchando evento `ApplicationReadyEvent` de Spring

### 5.2. Consideraciones Específicas de Mapeo (Nivel de Detalle Técnico)
Al convertir cada `PokemonTcgCardDto` a su entidad correspondiente:
- **Para todos los tipos:** Mapeo directo de campos simples (id, name, supertype, etc.)
- **Para Pokémon específicamente:**
  * Mapeo de `hp`, `pokemon_types`, `evolves_from`, `retreat_cost`, `converted_retreat_cost`, `is_ex`, `is_mega`
  * Procesamiento de relaciones one-to-many:
    * Para cada `AttackDto` en `dto.getAttacks()`: crear `PokemonCardAttackEntity`, mapear campos, asociar a la carta Pokémon
    * Para cada elemento en `dto.getWeakness()`: crear `PokemonCardWeaknessEntity`, mapear type/value, asociar
    * Para cada elemento en `dto.getResistance()`: crear `PokemonCardResistanceEntity`, mapear type/value, asociar
    * *Nota: Las listas de debilidades y resistencias en el DTO siguen el nombre singular per especificación, pero contienen múltiples elementos*
- **Para Trainer específicamente:** Mapeo de `trainer_subtype`, `is_ace_spec`, `effect_code` (derivado de rules si es necesario)
- **Para Energy específicamente:** Mapeo de `energy_card_type`, `provides_energy_types`
- **Manejo de campos nulos/empty:** Las listas en los DTOs se inicializan como listas vacías en lugar de null; los campos opcionales como `hp`, `rules` pueden ser null según supertype
- **Persistencia en cascada:** Gracias a `cascade = CascadeType.ALL` en las relaciones one-to-many de las entidades Pokémon, guardar la entidad principal también persiste automáticamente todas las entidades de relación asociadas (ataques, debilidades, resistencias)

## 6. Integración con Servicio de Catálogo

### 6.1. Mejora de `CardCatalogService`
**Ubicación:** `ar.edu.utn.frc.tup.piii.services.cards.CardCatalogService`

**Modificaciones requeridas:**
- Inyectar los tres repositorios de entidades principales (`PokemonCardJpaRepository`, `TrainerCardJpaRepository`, `EnergyCardJpaRepository`)
- Implementar métodos de consulta que:
  * Según el `supertype` solicitado en la petición, consultan únicamente la tabla correspondiente (`pokemon_cards`, `trainer_cards` o `energy_cards`)
  * Para consultas sin filtro de tipo, realizan un `UNION ALL` de las tres tablas (o tres consultas separadas según necesidad)
  * Mapean los resultados de estas entidades a los DTOs existentes del proyecto (`CardSummaryResponse`, `CardDetailResponse`) mediante un mapeador dedicado
- Mantener compatibilidad total con `CardController` existente y sus DTOs de respuesta
- **Restricción crítica:** No crear nuevos endpoints - el frontend continúa usando los mismos endpoints de cartas existentes (`/api/cards/*`)

## 7. Cumplimiento de Restricciones Clave

### 7.1. Arquitectura y Nombres
- ✅ **No cambiar nombres de clases/interfaces existentes:** Todas las clases mencionadas en el análisis actual (`CardEntity`, `CardAttackEntity`, etc.) permanecen sin modificaciones
- ✅ **Crear exclusivamente clases nuevas:** 
  * Tres entidades principales nuevas: `PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity`
  * Tres entidades de relación nuevas: `PokemonCardAttackEntity`, `PokemonCardWeaknessEntity`, `PokemonCardResistanceEntity`
  * Seis interfaces de repositorio nuevas
  * Un servicio nuevo: `CardCacheSyncService`
  * ✅ **Cero modificaciones** a clases/interfaces existentes del proyecto
- ✅ **Aprovechar arquitectura existente:** 
  * Respeta la estructura de paquetes definida en `02-project-structure-contract.md`
  * Ubica las nuevas entidades en `repositories/entities` (paquete existente)
  * Ubica los nuevos repositorios en `repositories/jpa` (paquete existente)
  * Ubica el nuevo servicio en `services/cards` (paquete existente)
  * Utiliza las mismas capas (api, application, domain, infrastructure) según corresponda
  * El motor de juego (`engine`) permanece completamente aislado como requiere el contrato

### 7.2. Persistencia y Exposición
- ✅ **Seis tablas H2 nuevas (ninguna modificando esquemas existentes):**
  * `pokemon_cards` para cartas de tipo Pokémon
  * `trainer_cards` para cartas de tipo Entrenador
  * `energy_cards` para cartas de tipo Energía
  * `pokemon_card_attacks` para relaciones de ataque de Pokémon
  * `pokemon_card_weaknesses` para relaciones de debilidad de Pokémon
  * `pokemon_card_resistances` para relaciones de resistencia de Pokémon
- ✅ **Datos específicos por tipo:** 
  * Cada tabla principal contiene únicamente los campos relevantes para su tipo de carta
  * Las tablas de relación solo existen para Pokémon (donde son necesarias según spec DTO)
  * Trainer y Energy no tienen tablas de relación (coherente con su DTO que no incluye ataques/debilidades/resistencias)
- ✅ **Set XY1 únicamente:** Las consultas API en `PokemonTcgApiClient` están filtradas específicamente para este set mediante `set.id:xy1` en el query
- ✅ **Endpoint no expuesto al frontend:** 
  * La sincronización ocurre únicamente en backend mediante `CardCacheSyncService@PostConstruct`
  * El frontend accede a cartas únicamente mediante los endpoints existentes de `CardController`
  * **Cero nuevos endpoints** creados en cualquier capa (api, websocket, etc.)

### 7.3. Inicio y Operación
- ✅ **Activación automática:** La sincronización se dispara al arranque de la aplicación, asegurando que los datos estén disponibles antes de que el frontend realice su primer request
- ✅ **Operación idempotente:** 
  * Volver a ejecutar la sincronización actualizará cartas existentes (por ID de API) o creará nuevas
  * Gracias al uso del `id` de la API como clave primaria natural y el comportamiento `save()` de Spring Data JPA
  * El `updated_at` se actualiza automáticamente en cada persistencia gracias a `@PreUpdate`

## 8. Orden de Implementación Recomendado

1. **Implementar cliente API** (`PokemonTcgApiClient`)
   - Añadir los tres methods de consulta especificados (`fetchPokemonCards`, `fetchTrainerCards`, `fetchEnergyCards`)
   - Configurar `RestTemplate` con timeouts, headers apropiados y manejo de errores
   - Implementar logging de llamadas API y respuestas

2. **Crear entidades especializadas y repositorios**
   - Definir `PokemonCardEntity`, `TrainerCardEntity`, `EnergyCardEntity` con sus campos exactos
   - Definir `PokemonCardAttackEntity`, `PokemonCardWeaknessEntity`, `PokemonCardResistanceEntity` 
   - Crear los seis interfaces de repositorio JPA (extendiendo `JpaRepository` con los tipos correctos)
   - Configurar mapeos de columnas usando `@Column` donde sea necesario (longitud, nullable, etc.)

3. **Implementar servicio de sincronización** (`CardCacheSyncService`)
   - Desarrollar lógica de obtención secuencial por tipo mediante los tres methods del cliente API
   - Implementar mapeo detallado de DTOs a las seis entidades nuevas
   - Añadir manejo de errores por carta (continuar con siguiente carta al fallar una) y por tipo (transacciones separadas)
   - Configurar logging detallado (inicio, progreso por tipo, errores, resumen final)
   - Configurar ejecución automática en startup (`@PostConstruct` o `ApplicationReadyEvent`)

4. **Mejorar servicio de catálogo** (`CardCatalogService`)
   - Inyectar los tres repositorios principales
   - Implementar métodos de búsqueda que seleccionen la tabla correcta según supertype
   - Crear mapeador de entidades nuevas a DTOs existentes del proyecto
   - Verificar compatibilidad total con `CardController` existente

5. **Verificación y pruebas**
   - Confirmar que las seis tablas se creen correctamente en H2 con esquema preciso
   - Verificar que los endpoints existentes de cartas devuelvan los datos importados (mapeo correcto a DTOs)
   - Asegurar que no se hayan creado nuevos endpoints expuestos al frontend (revisar todos los Controllers)
   - Validar que el motor de juego siga funcionando correctamente (sus tests deben pasar)
   - Probar específicamente:
     * Obtención de cartas Pokémon con sus ataques/debilidades/resistencias
     * Obtención de cartas Trainer y Energy sin relaciones
     * Operación idempotente (ejecutar sincronización dos veces)
     * Manejo de errores parciales (fallos en algunas cartas no detienen el proceso)

## 9. Consideraciones Adicionales

### 9.1. Rendimiento
- La sincronización ocurre solo una vez al inicio (o bajo demanda explícita mediante admin interno si se decide añadir posteriormente)
- Para sets futuros, considerar hacer el ID del set configurable mediante propiedad de aplicación (`pokemon.set.id=xy1` en application.properties)
- Crear índices de base de datos en:
  * Todas las tablas principales: `id` (PK), `set_code` (si se agrega este campo para futuros sets), `name`, `supertype`
  * Tablas de relación: columnas FK (`pokemon_card_id`) para joins eficientes
  * Campos de búsqueda frecuente: `name` en `pokemon_cards` para búsquedas por nombre

### 9.2. Mantenimiento
- El proceso es idempotente: ejecutar la sincronización múltiples veces es seguro y actualiza con datos más recientes de la API
- Para actualizaciones de set o nuevos sets, modificar únicamente las consultas API en `PokemonTcgApiClient` (cambiar el valor de `set.id:` en los queries)
- Los servicios de dominio y motor de juego **no requieren cambios** ya que:
  * Continúan trabajando con su modelo de carta existente (`CardDefinition` y subclases)
  * Obtienen los datos a través de `CardCatalogService` que mapea las nuevas entidades a sus DTOs existentes
  * La capa de dominio permanece completamente aislada de los cambios de persistencia
- Las entidades nuevas están aisladas en su propio contexto y no afectan a otros módulos del proyecto (cumpliendo con el requisito de no afectar otros módulos)

### 9.3. Seguridad
- Ningún cambio en superficie de ataque ya que no se añaden endpoints nuevos ni se modifican existentes (todas las llamadas son salientes a API pública)
- Las llamadas API salientes usan URLs fijas y hardcodeadas a `https://api.pokemontcg.io` (servicio público confiable sin autenticación requerida)
- No se almacenan claves API, tokens sensibles ni credenciales (la API de Pokémon TCG es de acceso público)
- Los datos persistidos son exclusivamente información pública de cartas de juego

Este plan cumple completamente con los requisitos especificados mientras mantiene la integridad arquitectónica del proyecto existente. La implementación se centra en crear entidades completamente nuevas (sin tocar nada existente) para asegurar que ningún otro módulo se vea afectado, siguiendo las directrices de no cambio de nombres y aprovechando la arquitectura base definida en los contratos del proyecto.