# Informe Técnico — Pokemon TCG G8

**Proyecto:** Implementación digital del Juego de Cartas Coleccionables Pokémon (set XY1)
**Curso:** Programación III — UTN FRC
**Tecnologías:** Spring Boot 4.0 (Java 21), Angular 20, PostgreSQL, WebSocket (STOMP)

---

## Índice

1. [Especificación de API REST](#1-especificación-de-api-rest)
2. [Decisiones de diseño justificadas](#2-decisiones-de-diseño-justificadas)
3. [Manual de despliegue](#3-manual-de-despliegue)

---

## 1. Especificación de API REST

La documentación interactiva de la API está disponible en formato OpenAPI 3.0 a través de Swagger UI:

| Recurso | URL |
|---------|-----|
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Paquete escaneado | `ar.edu.utn.frc.tup.piii.controllers` |

### 1.1 Convenciones generales

- **Base URL:** `http://localhost:8080/api`
- **Autenticación:** JWT Bearer Token (header `Authorization: Bearer <token>`), excepto endpoints marcados como `permitAll`
- **Formato de respuestas:** JSON
- **Errores:** Objeto `ErrorApi` con código, mensaje y timestamp
- **Paginación:** Parámetros `page` (0-based) y `size` en queries que devuelven listas

### 1.2 Endpoints

#### 1.2.1 Health Check

| Método | Ruta | Autenticación | Descripción |
|--------|------|:-------------:|-------------|
| `GET` | `/ping` | permitAll | Verifica que el servidor responda. Retorna `"pong"`. |

#### 1.2.2 Usuarios (`/api/users`)

| Método | Ruta | Autenticación | Request Body | Response | Descripción |
|--------|------|:-------------:|--------------|----------|-------------|
| `POST` | `/api/users/register` | permitAll | `CreateUserRequest` | `UserResponse` (201) | Registro de nuevo usuario |
| `POST` | `/api/users/login` | permitAll | `LoginRequest` | `UserResponse` | Inicio de sesión. Retorna JWT en el campo `token` |
| `GET` | `/api/users/{id}` | Authenticated | — | `UserResponse` | Obtener datos de usuario por ID |
| `GET` | `/api/users` | Authenticated | — | `List<UserResponse>` | Listar todos los usuarios |
| `PUT` | `/api/users/{id}` | Authenticated | `UpdateUserRequest` | `UserResponse` | Actualizar datos de usuario |
| `PUT` | `/api/users/{id}/deactivate` | Authenticated | — | `UserResponse` | Desactivar usuario |
| `PUT` | `/api/users/{id}/activate` | Authenticated | `Map<String,String>` | `UserResponse` | Reactivar usuario (requiere password) |
| `POST` | `/api/users/{id}/validate-password` | Authenticated | `Map<String,String>` | 200 OK | Validar contraseña actual |

**DTOs:**

```json
// CreateUserRequest
{
  "username": "ash",
  "email": "ash@pokemon.com",
  "password": "password123"
}

// LoginRequest
{
  "email": "ash@pokemon.com",
  "password": "password123"
}

// UserResponse
{
  "id": "uuid",
  "username": "ash",
  "email": "ash@pokemon.com",
  "role": "PLAYER",
  "status": "ACTIVE",
  "playerId": "uuid",
  "token": "jwt...",
  "createdAt": "2026-01-01T00:00:00"
}
```

#### 1.2.3 Jugadores (`/api/players`)

| Método | Ruta | Autenticación | Request Body | Response | Descripción |
|--------|------|:-------------:|--------------|----------|-------------|
| `GET` | `/api/players` | Authenticated | — | `List<PlayerResponse>` | Listar todos los jugadores |
| `GET` | `/api/players/{id}` | Authenticated | — | `PlayerResponse` | Obtener datos de jugador |
| `PUT` | `/api/players/{id}` | Authenticated | `UpdatePlayerRequest` | `PlayerResponse` | Actualizar perfil |
| `POST` | `/api/players/{id}/avatar` | Authenticated | Multipart `file` | `PlayerResponse` | Subir avatar |

#### 1.2.4 Mazos (`/api/decks`)

| Método | Ruta | Autenticación | Request Body | Response | Descripción |
|--------|------|:-------------:|--------------|----------|-------------|
| `POST` | `/api/decks` | Authenticated | `CreateDeckRequest` | `DeckResponse` (201) | Crear mazo (valida reglas antes de guardar) |
| `GET` | `/api/decks/{id}` | Authenticated | — | `DeckResponse` | Obtener mazo por ID |
| `PUT` | `/api/decks/{id}` | Authenticated | `UpdateDeckRequest` | `DeckResponse` | Actualizar mazo |
| `DELETE` | `/api/decks/{id}` | Authenticated | — | 204 No Content | Eliminar mazo |
| `GET` | `/api/decks` | Authenticated | Query `playerId` | `List<DeckResponse>` | Listar mazos de un jugador |
| `GET` | `/api/decks/predefined` | Authenticated | — | `List<DeckResponse>` | Obtener mazos predefinidos |
| `POST` | `/api/decks/{id}/copy` | Authenticated | Query `playerId` | `DeckResponse` (201) | Copiar mazo a la colección de un jugador |
| `POST` | `/api/decks/{id}/validate` | Authenticated | — | `DeckValidationResponse` | Validar mazo guardado |
| `POST` | `/api/decks/validate` | Authenticated | `ValidateDeckRequest` | `DeckValidationResponse` | Validar lista de cartas sin guardar |
| `POST` | `/api/decks/random` | Authenticated | — | `DeckResponse` (201) | Generar mazo aleatorio válido |
| `POST` | `/api/decks/import` | Authenticated | Multipart: `file`, `playerId`, `format` | `List<DeckResponse>` (201) | Importar mazo(s) desde TXT/JSON |
| `GET` | `/api/decks/{id}/export` | Authenticated | — | PDF (`application/pdf`) | Exportar mazo como PDF |

**DTOs:**

```json
// CreateDeckRequest
{
  "name": "Mi Mazo",
  "playerId": "uuid",
  "cards": [
    { "cardId": "xy1-1", "quantity": 2 },
    { "cardId": "xy1-10", "quantity": 4 }
  ]
}

// DeckResponse
{
  "id": "uuid",
  "name": "Mi Mazo",
  "ownerPlayerId": "uuid",
  "source": "USER",
  "totalCards": 60,
  "valid": true,
  "cards": [
    {
      "cardId": "xy1-1",
      "name": "Venusaur-EX",
      "quantity": 2,
      "supertype": "POKEMON",
      "isBasicEnergy": false,
      "subtypes": ["EX"],
      "stage": "STAGE_2"
    }
  ],
  "validation": {
    "valid": true,
    "errors": []
  },
  "createdAt": "2026-01-01T00:00:00"
}

// DeckValidationResponse
{
  "valid": false,
  "errors": [
    {
      "code": "DECK_SIZE_INVALID",
      "message": "El mazo debe tener exactamente 60 cartas.",
      "details": null
    }
  ]
}
```

**Reglas de validación de mazos implementadas** (`DeckValidator`):

| Código | Regla | Condición |
|--------|-------|-----------|
| `DECK_SIZE_INVALID` | Exactamente 60 cartas | `totalCards != 60` |
| `MORE_THAN_4_COPIES` | Máx. 4 copias por carta | Por ID de carta (exento: Energía Básica); por nombre para Pokémon y Energías Especiales |
| `MISSING_BASIC_POKEMON` | Al menos 1 Pokémon Básico | `count(Pokemon con stage BASIC) < 1` |
| `ACE_SPEC_LIMIT_EXCEEDED` | Máx. 1 carta AS TÁCTICO | `count(aceSpec) > 1` (no aplica en el juego actual) |
| `DUPLICATE_CARDS` | Sin cartas duplicadas | Definido en enum, no implementado |
| `INVALID_DECK_FORMAT` | Formato válido | Definido en enum, no implementado |

#### 1.2.5 Cartas (`/api/cards`)

| Método | Ruta | Autenticación | Request | Response | Descripción |
|--------|------|:-------------:|---------|----------|-------------|
| `GET` | `/api/cards` | permitAll | Query: `query`, `supertype`, `setCode`, `stage`, `page`, `size` | `CardSearchResponse` | Buscar cartas con filtros y paginación |
| `GET` | `/api/cards/{id}` | permitAll | — | `CardDetailResponse` | Obtener detalle de carta por ID |
| `POST` | `/api/cards/sync` | Authenticated | — | `CardSyncResponse` | Forzar sincronización desde API externa |

#### 1.2.6 Partidas (`/api/matches`)

| Método | Ruta | Autenticación | Request Body | Response | Descripción |
|--------|------|:-------------:|--------------|----------|-------------|
| `GET` | `/api/matches` | Authenticated | Query `status` (default WAITING) | `List<MatchResponse>` | Listar partidas por estado |
| `GET` | `/api/matches/active` | Authenticated | — | `List<MatchResponse>` | Partidas activas del jugador autenticado |
| `POST` | `/api/matches` | Authenticated | `CreateMatchRequest` | `MatchResponse` (201) | Crear nueva partida |
| `POST` | `/api/matches/{id}/join` | Authenticated | `JoinMatchRequest` | `MatchResponse` | Unirse a partida |
| `GET` | `/api/matches/{id}/state` | Authenticated | — | `MatchStateResponse` | Obtener estado actual de la partida |
| `DELETE` | `/api/matches/{id}` | Authenticated | — | `MatchResponse` | Cancelar partida |
| `GET` | `/api/matches/{id}/chat` | Authenticated | — | `List<ChatMessage>` | Obtener historial de chat |
| `POST` | `/api/matches/{id}/concede` | Authenticated | — | `MatchResponse` | Rendirse |

#### 1.2.7 Acciones de juego (`/api/matches/{id}/actions`)

| Método | Ruta | Autenticación | Request Body | Response | Descripción |
|--------|------|:-------------:|--------------|----------|-------------|
| `POST` | `/api/matches/{id}/actions` | Authenticated | `GameActionRequest` | `GameActionResponse` | Ejecutar una acción de juego |

**DTOs:**

```json
// GameActionRequest
{
  "type": "DECLARE_ATTACK",
  "playerId": "uuid",
  "payload": {
    "attackIndex": 0,
    "targetPokemonInstanceId": "uuid"
  }
}

// GameActionResponse
{
  "success": true,
  "error": null,
  "actionId": "uuid",
  "state": { /* MatchStateResponse completo */ },
  "pendingEvents": [
    {
      "type": "KNOCKOUT_OCCURRED",
      "matchId": "uuid",
      "turnNumber": 5,
      "timestamp": "2026-01-01T00:00:00",
      "description": "Pikachu fue debilitado.",
      "payload": { "knockedOutPokemonInstanceId": "uuid" }
    }
  ]
}
```

**Tipos de acción soportados:**

| Tipo | Descripción |
|------|-------------|
| `DRAW_CARD` | Robar carta del mazo |
| `PUT_BASIC_ON_BENCH` | Colocar Pokémon Básico en Banca |
| `ATTACH_ENERGY` | Asignar energía del mano a un Pokémon |
| `EVOLVE_POKEMON` | Evolucionar Pokémon |
| `PLAY_TRAINER` | Jugar carta de Entrenador |
| `RETREAT_ACTIVE` | Retirar Pokémon Activo |
| `DECLARE_ATTACK` | Declarar ataque |
| `END_TURN` | Terminar turno |
| `TAKE_PRIZE_CARD` | Tomar carta de premio |
| `ATTACH_TOOL` | Asignar herramienta |
| `USE_ABILITY` | Usar habilidad |
| `CHOOSE_KO_REPLACEMENT` | Elegir reemplazo por KO |
| `SETUP_PLACE_ACTIVE` | Colocar activo en setup |
| `SETUP_PLACE_BENCH` | Colocar banca en setup |
| `SETUP_REMOVE_ACTIVE` | Remover activo en setup |
| `SETUP_REMOVE_BENCH` | Remover banca en setup |
| `CONFIRM_SETUP` | Confirmar setup inicial |
| `RESOLVE_MULLIGAN_DRAW` | Resolver robo por mulligan |
| `RESOLVE_INITIAL_MULLIGAN` | Resolver mulligan inicial |

#### 1.2.8 Historial de partidas (`/api/matches/history`)

| Método | Ruta | Autenticación | Response | Descripción |
|--------|------|:-------------:|----------|-------------|
| `GET` | `/api/matches/history` | Authenticated | `List<MatchSummaryResponse>` | Historial del jugador autenticado |
| `GET` | `/api/matches/history/{id}` | Authenticated | `MatchSummaryResponse` | Detalle de partida finalizada |

#### 1.2.9 Ranking (`/api/ranking`, `/api/players/{id}/stats`)

| Método | Ruta | Autenticación | Response | Descripción |
|--------|------|:-------------:|----------|-------------|
| `GET` | `/api/ranking` | Authenticated | `List<RankingEntryResponse>` | Ranking global de jugadores |
| `GET` | `/api/players/{id}/stats` | Authenticated | `PlayerStatsResponse` | Estadísticas de un jugador |

#### 1.2.10 Administración y Debug (solo perfil `dev`)

| Método | Ruta | Autenticación | Descripción |
|--------|------|:-------------:|-------------|
| `POST` | `/api/admin/regenerate-effect-codes` | Authenticated | Regenerar códigos de efecto de cartas |
| `POST` | `/api/debug/matches/{id}/force-sudden-death` | Authenticated | Forzar muerte súbita en una partida |

---

## 2. Decisiones de diseño justificadas

### 2.1 Arquitectura hexagonal (Ports & Adapters)

El motor de juego (`GameEngine`) está diseñado como un componente **100% puro en Java**, sin ninguna dependencia de Spring Boot, JPA ni ningún framework. Se comunica con el exterior exclusivamente a través de interfaces definidas como puertos:

| Puerto | Responsabilidad |
|--------|----------------|
| `CardLookupPort` | Obtener definiciones de cartas por ID |
| `RandomizerPort` | Aleatoriedad (shuffle, monedas, dados) |
| `StatePersisterPort` | Persistir y cargar el estado de la partida |
| `EventPublisherPort` | Publicar eventos de juego (WebSocket) |
| `DeckLoadPort` | Cargar mazos desde la base de datos |

**Justificación:** Esta decisión permite:

1. **Testabilidad:** El engine puede ser probado con mocks de los puertos sin necesidad de levantar Spring, base de datos ni servidor web.
2. **Aislamiento del dominio:** Las reglas del juego no están contaminadas con anotaciones JPA, HTTP ni detalles de infraestructura. Si se cambia la base de datos o se migra a otro framework, el motor de juego no se modifica.
3. **Claridad:** La separación entre "qué necesita el engine" (puertos) y "cómo se implementa" (adaptadores) es explícita.

### 2.2 Command Pattern para acciones de juego

Cada tipo de acción (`GameActionType`) está mapeado a un handler específico que implementa `GameHandler`. El `GameEngine` mantiene un `Map<GameActionType, GameHandler>` y delega la ejecución al handler correspondiente.

```java
handlers.put(GameActionType.DECLARE_ATTACK, new DeclareAttackHandler(...));
handlers.put(GameActionType.PLAY_TRAINER, new PlayTrainerHandler(...));
handlers.put(GameActionType.TAKE_PRIZE_CARD, new TakePrizeCardHandler(...));
// ... 19 handlers en total
```

**Justificación:** Este patrón cumple con el **Principio Open/Closed** (OCP): para agregar una nueva acción de juego solo se crea un nuevo handler y se registra en el mapa, sin modificar ningún handler existente ni la lógica de dispatch. También facilita las pruebas unitarias al aislar cada acción en una clase independiente.

### 2.3 Chain of Responsibility para el pipeline de ataque

La resolución de un ataque se implementa como una cadena de 10 pasos ejecutados secuencialmente:

```
PrerequisiteStep → EnergyCheckStep → ConfusionCheckStep → ConditionCheckStep
→ ModifierStep → TargetSelectionStep → PreDamageStep → DamageStep
→ PostDamageEffectStep → KnockoutCheckStep
```

Cada paso implementa la interfaz `AttackStep` con el método `execute()`. La cadena puede detenerse en cualquier punto si un paso falla (ej: el Pokémon atacante está dormido y no se despierta).

**Justificación:** Este patrón permite:

1. **Responsabilidad única:** Cada step maneja una preocupación específica del ataque (verificar energía, aplicar daño, detectar KOs).
2. **Extensibilidad:** Se pueden agregar, quitar o reordenar pasos sin afectar al resto. Por ejemplo, se podría insertar un paso de "efecto de campo" entre `ModifierStep` y `TargetSelectionStep` sin modificar ninguna otra clase.
3. **Control de flujo:** La cadena puede abortarse temprano si un prerrequisito no se cumple, evitando ejecución innecesaria.

### 2.4 State Pattern para ciclo de vida y turnos

**Ciclo de vida de la partida:**

```
WaitingMatchState → SetupMatchState → ActiveMatchState → FinishedMatchState
```

Cada estado implementa `MatchState` que define qué acciones están permitidas. `MatchStateMachine` delega la validación al estado actual.

**Fases del turno:**

```
DrawTurnState → MainTurnState → AttackTurnState → BetweenTurnsTurnState
```

Cada fase implementa `TurnState` con transiciones definidas. El `TurnManager` avanza la fase llamando a `advancePhase()`.

**Justificación:** El patrón State evita condicionales `if/switch` dispersos por el código. Cada estado encapsula su propio comportamiento y transiciones, haciendo el flujo explícito y fácil de modificar. Además, estados inválidos son literalmente imposibles de representar.

### 2.5 Frontend como cliente delgado

El frontend Angular implementa una arquitectura de **cliente delgado**: solo se encarga de renderizar la UI y enviar acciones al backend. Toda la lógica de negocio (validación de reglas, resolución del motor de juego, persistencia) reside exclusivamente en el backend.

**Justificación:**

1. **Consistencia:** El backend es la única fuente de verdad. No hay riesgo de que el frontend y el backend tengan implementaciones divergentes de las reglas del juego.
2. **Simplificación del frontend:** El frontend no necesita conocer las reglas del TCG. Solo necesita mostrar el estado que recibe y traducir clics del usuario en acciones.
3. **Independencia tecnológica:** Cualquier cliente (web, mobile, CLI) puede consumir la misma API sin duplicar lógica de juego.

### 2.6 Comunicación en tiempo real con WebSocket (STOMP)

Las partidas activas utilizan WebSocket con el protocolo STOMP para sincronización en tiempo real. Cuando el engine procesa una acción, publica eventos a través de `MatchWebSocketPublisher`, que envía los cambios a todos los clientes suscritos al tópico `/topic/matches/{id}`.

**Justificación:** El modelo REST tradicional requeriría polling constante del frontend para detectar cambios de estado. WebSocket ofrece:

1. **Latencia mínima:** Los eventos se entregan instantáneamente cuando ocurren.
2. **Eficiencia:** Sin requests innecesarios; solo hay tráfico cuando hay cambios.
3. **Sincronización:** Todos los jugadores ven el mismo estado al mismo tiempo.

### 2.7 Autenticación JWT Stateless

Se utiliza JWT (JSON Web Token) con expiración de 24 horas. Las contraseñas se almacenan con BCrypt. No se mantiene sesión en el servidor.

**Justificación:** JWT es adecuado para API REST porque:

1. **Escalabilidad:** Cualquier instancia del backend puede verificar un token sin compartir sesión.
2. **Stateless:** No se requiere almacenamiento en servidor para sesiones activas.
3. **Seguridad:** Los tokens están firmados, las contraseñas hasheadas, y se puede forzar expiración.

### 2.8 Mapeo de cartas desde API externa

Las cartas se sincronizan desde la [Pokemon TCG API](https://pokemontcg.io/) mediante `PokemonTcgApiClient`. Los datos se cachean en base de datos local (`cards` y tablas relacionadas) y en una caché in-memory (Spring Cache).

**Justificación:** Depender directamente de la API externa para cada consulta introduciría latencia y riesgos de rate limiting. La sincronización local permite:

1. **Rendimiento:** Las búsquedas de cartas usan la base de datos local, no la API externa.
2. **Disponibilidad:** El sistema funciona sin conexión a internet una vez sincronizadas las cartas.
3. **Cache:** Spring Cache evita consultas repetitivas a la base de datos para cartas consultadas frecuentemente.

### 2.9 Validación de mazos

La validación se realiza tanto en frontend (en tiempo real, como feedback visual) como en backend (obligatoria, como enforcement). El backend siempre valida antes de guardar, previniendo estados inválidos.

**Justificación:** La validación doble (frontend + backend) ofrece:

1. **Experiencia de usuario:** El feedback inmediato en el frontend evita que el usuario tenga que guardar para descubrir errores.
2. **Seguridad:** La validación en backend es la autoritativa; no se puede eludir.
3. **Consistencia:** No se guardan mazos inválidos en la base de datos.

### 2.10 Esquema de base de datos con Flyway

Las migraciones de base de datos se gestionan con Flyway. Los cambios al schema se versionan como scripts SQL en `src/main/resources/db/migration/`.

**Justificación:** Flyway asegura que el schema de la base de datos está siempre sincronizado con la versión del código. Esto es particularmente importante en un equipo de desarrollo donde múltiples personas pueden hacer cambios al schema.

---

## 3. Manual de despliegue

### 3.1 Requisitos del sistema

| Componente | Versión requerida |
|------------|------------------|
| Java | 21 (JDK) |
| Maven | 3.9+ |
| Node.js | 20+ |
| npm | 10+ |
| Docker | 24+ (para PostgreSQL) |
| Sistema operativo | Windows, macOS o Linux |

### 3.2 Entorno de desarrollo

#### 3.2.1 Clonar el repositorio

```bash
git clone <repo-url>
cd Pokemon-TCG-G8
```

#### 3.2.2 Base de datos

**Opción A: PostgreSQL con Docker (recomendado)**

```bash
# Iniciar PostgreSQL
docker compose up -d

# Verificar que esté corriendo
docker compose ps
```

Esto levanta PostgreSQL 16 en `localhost:5432` con:
- Base de datos: `pokemon_tcg`
- Usuario: `postgres`
- Contraseña: `postgres`
- Puerto: `5432`
- Los datos se persisten en un volumen Docker (`pgdata`)

**Opción B: H2 en memoria (sin Docker)**

Usar el perfil `dev` que automáticamente cambia a H2 sin necesidad de Docker.

#### 3.2.3 Backend (Spring Boot)

```bash
cd BE

# Con PostgreSQL (requiere Docker arriba)
mvn spring-boot:run

# Con H2 en memoria (sin Docker)
mvn spring-boot:run -Dspring.profiles.active=dev
```

La aplicación se inicia en `http://localhost:8080`.

Al iniciar por primera vez:
1. Flyway ejecuta las migraciones (`V1__init.sql` crea tablas, `V2__seed_users.sql` crea usuarios de prueba)
2. El `CardCacheSyncService` sincroniza las cartas desde la API externa (puede tomar varios segundos)
3. El `SeederTestConfig` crea usuarios semilla y mazos predefinidos

**Usuarios de prueba predefinidos:**

| Email | Contraseña | Nombre |
|-------|-----------|--------|
| `ash@pokemon.com` | `password123` | Ash Ketchum |
| `misty@pokemon.com` | `password456` | Misty |

#### 3.2.4 Frontend (Angular)

```bash
cd FE
npm install
ng serve
```

La aplicación se inicia en `http://localhost:4200`. El proxy de Angular CLI redirige las llamadas `/api/**` a `http://localhost:8080`.

#### 3.2.5 Verificar la instalación

| Componente | URL |
|------------|-----|
| Frontend | `http://localhost:4200` |
| Backend health | `http://localhost:8080/ping` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI spec | `http://localhost:8080/v3/api-docs` |
| H2 Console (solo dev) | `http://localhost:8080/h2-console` |

### 3.3 Perfiles de configuración

| Perfil | Base de datos | Docker requerido | Flyway | JPA DDL |
|--------|:-------------:|:----------------:|:------:|:--------:|
| `default` | PostgreSQL | Sí | Habilitado | `validate` |
| `dev` | H2 en memoria | No | Deshabilitado | `update` |

### 3.4 Compilación y empaquetado

#### Backend

```bash
cd BE
mvn clean package    # Genera BE/target/tpi-pokemon-0.0.1-SNAPSHOT.jar
java -jar target/tpi-pokemon-0.0.1-SNAPSHOT.jar
```

#### Frontend

```bash
cd FE
npm run build        # Genera FE/dist/
```

### 3.5 Pruebas

```bash
# Todas las pruebas del backend
cd BE
mvn test

# Pruebas de un módulo específico
mvn test -Dtest="RuleValidatorTest"

# Reporte de cobertura (JaCoCo)
mvn test
# El reporte HTML se genera en BE/target/site/jacoco/index.html

# Pruebas del frontend (Playwright)
cd FE
npx playwright test
```

### 3.6 Reportes

| Reporte | Comando | Ubicación |
|---------|---------|-----------|
| Cobertura de código | `mvn test` | `BE/target/site/jacoco/index.html` |
| Javadoc | `mvn javadoc:javadoc` | `BE/target/site/apidocs/` |
| Swagger | — | `http://localhost:8080/swagger-ui.html` |

### 3.7 Estructura de directorios relevante

```
Pokemon-TCG-G8/
├── BE/                          # Backend Spring Boot
│   ├── src/main/java/.../
│   │   ├── Application.java     # Punto de entrada
│   │   ├── configs/             # Configuraciones Spring
│   │   ├── controllers/         # Controladores REST
│   │   ├── domain/              # Modelos de dominio puros
│   │   ├── engine/              # Motor de juego (sin dependencias Spring)
│   │   ├── services/            # Servicios de negocio
│   │   ├── repositories/        # JPA repositories + entities
│   │   ├── security/            # JWT + Spring Security
│   │   ├── websocket/           # WebSocket STOMP
│   │   └── clients/             # Clientes HTTP externos
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── db/migration/        # Migraciones Flyway
│   └── pom.xml
├── FE/                          # Frontend Angular
│   ├── src/app/
│   │   ├── app.ts               # Componente raíz
│   │   ├── core/                # Servicios core (API, auth, WS)
│   │   ├── features/            # Módulos funcionales
│   │   └── shared/              # Componentes compartidos
│   └── package.json
├── docker-compose.yml           # PostgreSQL
└── docs/                        # Documentación adicional
```

### 3.8 Notas de troubleshooting

**Error: `El sistema no puede encontrar el archivo .mvn\wrapper\maven-wrapper.properties`**

No usar `mvnw.cmd`. Usar `mvn` directamente (Maven 3.9+ está instalado globalmente).

**Error: `Failed to load ApplicationContext` en tests**

Posible condición de carrera entre `CardCacheSyncService` y `SeederTestConfig`. Solución: ya corregido con `@Order` en las clases.

**Error: La sincronización de cartas falla**

Verificar conectividad a internet. La app usa `https://api.pokemontcg.io/v2/` para sincronizar.

**Error de conexión a PostgreSQL**

Verificar que Docker esté corriendo y el contenedor esté activo:
```bash
docker compose ps
```
Si no está activo, iniciarlo con `docker compose up -d`.
