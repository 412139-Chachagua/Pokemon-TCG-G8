<p align="center"> <img src="./BE/docs/assets/images/pokemon-tcg.png" alt="Pokemon TCG"/> </p>

# Pokémon Trading Card Game

El Pokémon Trading Card Game, inspirado en el universo creado por Nintendo, es un juego de cartas estratégico en el que los
jugadores asumen el rol de entrenadores Pokémon que compiten entre sí utilizando mazos personalizados. Cada jugador construye su
estrategia combinando Pokémon, cartas de energía y cartas de entrenador, buscando derrotar a los Pokémon rivales mediante
ataques, habilidades y decisiones tácticas a lo largo de la partida.

A diferencia de otros juegos de estrategia tradicionales, el Pokémon TCG combina planificación y adaptabilidad, ya que el
éxito depende tanto de la construcción del mazo como de la toma de decisiones en tiempo real. Los jugadores deben
gestionar recursos, evolucionar sus Pokémon y anticipar los movimientos del oponente para obtener ventaja en el combate.

En este contexto, como parte de la materia Programación III, se presenta el desafío de desarrollar una versión digital
del juego de cartas Pokémon, implementando tanto el front-end como el back-end de la aplicación. Este proyecto permitirá
poner en práctica conceptos fundamentales de desarrollo de software, incluyendo la arquitectura cliente-servidor, el
diseño de APIs, la gestión del estado del juego y la interacción entre múltiples jugadores.

Los estudiantes deberán modelar las entidades del juego, definir las reglas principales y garantizar una experiencia de
usuario fluida e interactiva, integrando lógica de negocio con una interfaz clara y funcional.

Este desafío propone una experiencia completa que combina estrategia, diseño y programación, permitiendo aplicar los
conocimientos adquiridos en un entorno práctico y motivador.

<p align="center"> <img src="./BE/docs/assets/images/UTN-FRC_logo.png" alt="UTN - FRC"/> </p>

<p align="center"> <img src="./BE/docs/assets/images/Tup_completo_negro_transparente.png" alt="TUP"/> </p>

# Pokémon TCG - TPI Programación III

Implementación digital de **Pokémon Trading Card Game** (set XY1) con **Spring Boot 4.0** en backend y **Angular 20** en frontend, como Trabajo Práctico Integrador de Programación III — UTN FRC.

---

## Stack tecnológico

### Backend

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Java | 21 (JDK) | Lenguaje principal |
| Spring Boot | 4.0.0 | Framework de aplicación |
| Spring Web | — | Controladores REST |
| Spring Data JPA | — | Persistencia con Hibernate |
| Spring Security | — | Autenticación JWT |
| Spring WebSocket | — | Comunicación en tiempo real (STOMP) |
| Spring Validation | — | Validación de DTOs |
| Spring Cache | — | Caché de cartas en memoria |
| Spring Boot Docker Compose | — | Auto-inicio del contenedor PostgreSQL |
| Maven | 3.9+ | Build y dependencias |
| PostgreSQL | 16 | Base de datos principal |
| H2 | 2.4 | Base de datos en memoria para tests/dev |
| Flyway | — | Migraciones de base de datos versionadas |
| JaCoCo | 0.8.12 | Reporte de cobertura de código |
| ModelMapper | 3.1.1 | Mapeo entre entidades y DTOs |
| SpringDoc OpenAPI | 2.8.0 | Documentación Swagger/OpenAPI |
| Lombok | 1.18.30 | Reducción de boilerplate |
| JJWT | 0.12.6 | Tokens JWT |
| PDFBox | 3.0.3 | Exportación de mazos a PDF |

### Frontend

| Tecnología | Versión | Propósito |
|------------|---------|-----------|
| Angular | 20.3 | Framework de aplicación |
| TypeScript | 5.9 | Lenguaje |
| RxJS | 7.8 | Programación reactiva |
| STOMP.js | 7.3 | Cliente WebSocket STOMP |
| SockJS | 1.6 | Fallback de WebSocket |
| Tailwind CSS | 4.3 | Estilos utilitarios |
| Playwright | 1.61 | Tests E2E |

---

## Diagrama de arquitectura

```
┌─────────────────────────────────────────────────────────────────────┐
│                        CLIENTE (Angular 20)                         │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌────────────────────┐  │
│  │  Features │  │  Shared  │  │   Core   │  │  WebSocket (STOMP) │  │
│  │ (lazy)    │  │ Components│  │ API/     │  │  match-socket      │  │
│  │           │  │          │  │ Auth/    │  │  .service.ts       │  │
│  │ - auth    │  │ - cards  │  │ Config   │  └────────┬───────────┘  │
│  │ - decks   │  │ - common │  │          │           │              │
│  │ - match   │  │          │  │          │           │              │
│  │ - lobby   │  │          │  │          │           │              │
│  │ - ranking │  │          │  │          │           │              │
│  │ - history │  │          │  │          │           │              │
│  └──────────┘  └──────────┘  └─────┬────┘           │              │
└────────────────────────────────────┼─────────────────┼──────────────┘
                                     │ HTTP (REST)     │ WS (STOMP)
                                     ▼                 ▼
┌─────────────────────────────────────────────────────────────────────┐
│                     BACKEND (Spring Boot 4.0)                        │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Controller Layer                                             │   │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐   │   │
│  │  │  Match   │ │  Deck    │ │  Card    │ │ User / Player  │   │   │
│  │  │Controller│ │Controller│ │Controller│ │ Controllers    │   │   │
│  │  └─────┬────┘ └────┬─────┘ └────┬─────┘ └───────┬────────┘   │   │
│  └────────┼───────────┼────────────┼───────────────┼─────────────┘   │
│           ▼           ▼            ▼               ▼                 │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Service Layer                                               │   │
│  │  Match, Deck, Card, User, Player, Ranking, CardSync, Pdf    │   │
│  └──────────────────────────┬───────────────────────────────────┘   │
│                             ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Game Engine (pure Java, sin dependencias Spring)             │   │
│  │                                                               │   │
│  │  ┌─────────────┐  ┌──────────────────┐  ┌────────────────┐   │   │
│  │  │ GameEngine   │  │  RuleValidator   │  │  TurnManager   │   │   │
│  │  │ (Command)    │  │  (Strategy)      │  │  (State)       │   │   │
│  │  └──────┬──────┘  └──────────────────┘  └────────────────┘   │   │
│  │         │                                                      │   │
│  │         ▼                                                      │   │
│  │  ┌──────────────────────────────────────────────────┐          │   │
│  │  │  Attack Pipeline (Chain of Responsibility)        │          │   │
│  │  │  Prerequisite → EnergyCheck → ConfusionCheck →   │          │   │
│  │  │  ConditionCheck → Modifier → TargetSelection →   │          │   │
│  │  │  PreDamage → Damage → PostDamage → KO Check      │          │   │
│  │  └──────────────────────────────────────────────────┘          │   │
│  │                                                               │   │
│  │  ┌────────────────────────────┐  ┌────────────────────────┐   │   │
│  │  │  Trainer Effect Resolver   │  │  Ability Resolver      │   │   │
│  │  │  (Registry Pattern)        │  │  (Registry Pattern)    │   │   │
│  │  └────────────────────────────┘  └────────────────────────┘   │   │
│  └──────────┬────────────────────────────────────────────────────┘   │
│             │  Ports (Hexagonal Architecture)                         │
│             ▼                                                        │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Adapters                                                     │   │
│  │  ┌──────────────┐ ┌────────────┐ ┌───────────────────────┐   │   │
│  │  │ CardLookup   │ │ State      │ │ EventPublisher       │   │   │
│  │  │ Adapter      │ │ Persister  │ │ (WebSocket Publisher)│   │   │
│  │  └──────────────┘ └────────────┘ └───────────────────────┘   │   │
│  │  ┌──────────────┐ ┌────────────┐ ┌───────────────────────┐   │   │
│  │  │ Randomizer   │ │ DeckLoad   │ │ CardCacheSyncService  │   │   │
│  │  │ Adapter      │ │ Adapter    │ │ (Pokemon TCG API)     │   │   │
│  │  └──────────────┘ └────────────┘ └───────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │  Persistence Layer                                             │   │
│  │  JPA Entities → Spring Data JPA → Flyway → PostgreSQL / H2   │   │
│  └──────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Instalación y ejecución local

### Requisitos

| Componente | Versión | Verificación |
|------------|---------|:------------:|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| Node.js | 20+ | `node -version` |
| npm | 10+ | `npm -version` |
| Docker | 24+ | `docker --version` |

### 1. Clonar el repositorio

```bash
git clone <repo-url>
cd Pokemon-TCG-G8
```

### 2. Base de datos

Hay dos opciones:

**Opción A: PostgreSQL con Docker (recomendado)**

```bash
docker compose up -d
```

Esto levanta PostgreSQL 16 en `localhost:5432` con:
- Base: `pokemon_tcg`
- Usuario: `postgres`
- Contraseña: `postgres`
- Puerto: `5432`

**Opción B: H2 en memoria (sin Docker, ideal para desarrollo rápido)**

No requiere Docker. Se usa el perfil `dev` al iniciar el backend.

### 3. Backend

```bash
cd BE

# Con PostgreSQL (requiere Docker arriba)
mvn spring-boot:run

# Con H2 en memoria (sin Docker)
mvn spring-boot:run -Dspring.profiles.active=dev
```

El backend se inicia en `http://localhost:8080`.

Al iniciar por primera vez:
1. Flyway ejecuta las migraciones (creación de tablas y usuarios de prueba)
2. El `CardCacheSyncService` sincroniza las cartas desde la API pública de Pokémon TCG
3. El `SeederTestConfig` crea usuarios semilla y mazos predefinidos

**Usuarios de prueba:**

| Email | Contraseña | Nombre |
|-------|-----------|--------|
| `ash@pokemon.com` | `password123` | Ash Ketchum |
| `misty@pokemon.com` | `password456` | Misty |

### 4. Frontend

```bash
cd FE
npm install
ng serve
```

El frontend se inicia en `http://localhost:4200`. Las llamadas a `/api/**` se redirigen automáticamente al backend.

### 5. Verificar

| Componente | URL |
|------------|-----|
| Frontend | `http://localhost:4200` |
| Backend health | `http://localhost:8080/ping` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI spec | `http://localhost:8080/v3/api-docs` |

---

## Ejecutar pruebas

```bash
cd BE

# Todas las pruebas
mvn test

# Pruebas específicas
mvn test -Dtest="RuleValidatorTest,GameEngineTest"

# Reporte de cobertura JaCoCo (se genera automáticamente con mvn test)
# Abrir BE/target/site/jacoco/index.html

# Ignorar fallos de contexto Spring en tests que no dependen de DB
mvn test -Dtest="RuleValidatorTest" -DfailIfNoTests=false
```

---

## Estado del proyecto

### Implementado

- **Motor de juego completo**: 19 tipos de acción con handlers específicos (Command Pattern)
- **Pipeline de ataque**: 10 pasos en cadena (Chain of Responsibility) incluyendo daño, condición, KO y chequeo de energía
- **Sistema de turnos**: Draw → Main → Attack → Between Turns (State Pattern)
- **Setup de partida**: Colocación de activo/banca, mulligan, confirmación
- **Validación de reglas**: RuleValidator con 20 validaciones (todas las acciones de juego)
- **Sistema de energía**: 4 estrategias (básica, doble incolora, arcoíris, fuerte)
- **Efectos de entrenadores**: 22 tipos de efectos registrados
- **Habilidades de Pokémon**: 6 habilidades implementadas
- **Condiciones especiales**: Dormido, quemado, confundido, paralizado, envenenado
- **Condiciones de victoria**: Premios, knockout, deck-out, muerte súbita, rendición
- **Persistencia**: Estado serializado después de cada acción
- **API REST**: 11 controladores con ~40 endpoints documentados en Swagger
- **WebSocket STOMP**: Sincronización en tiempo real de eventos de partida
- **Autenticación JWT**: Login/registro con tokens
- **Mazos**: CRUD completo, validación (60 cartas, ≤4 copias, ≥1 básico), importación TXT/JSON, exportación PDF, mazos predefinidos, generación aleatoria
- **Catálogo de cartas**: Sincronización desde Pokemon TCG API con caché local
- **Frontend Angular**: 12 módulos funcionales (auth, decks, match, lobby, ranking, history, profile, cards, rules, sandbox, home, splash)

### Perfiles de configuración

| Perfil | Base de datos | Docker | Flyway | JPA DDL |
|--------|:-------------:|:------:|:------:|:--------:|
| `default` | PostgreSQL | Requerido | Habilitado | `validate` |
| `dev` | H2 en memoria | No | Deshabilitado | `update` |

---

## Documentación técnica

El informe técnico completo está disponible en:

```
BE/docs/informe-tecnico.md
```

Incluye:
- Especificación completa de la API REST (todos los endpoints, DTOs, ejemplos)
- Decisiones de diseño justificadas (arquitectura hexagonal, patrones, etc.)
- Manual de despliegue detallado

Swagger UI disponible en `http://localhost:8080/swagger-ui.html` (requiere backend corriendo).

---

## Estructura del proyecto

```text
Pokemon-TCG-G8/
├── BE/                              # Backend Spring Boot
│   ├── src/main/java/.../
│   │   ├── Application.java         # Punto de entrada
│   │   ├── configs/                 # Configuraciones Spring
│   │   ├── controllers/             # Controladores REST
│   │   ├── domain/                  # Modelos de dominio puros
│   │   ├── engine/                  # Motor de juego (hexagonal)
│   │   │   ├── GameEngine.java      # Orquestador
│   │   │   ├── rules/               # Validación de reglas
│   │   │   ├── handlers/            # Handlers de acciones
│   │   │   ├── attack/              # Pipeline de ataque
│   │   │   ├── turn/                # Gestión de turnos
│   │   │   ├── model/               # Estado de partida
│   │   │   ├── energy/              # Sistema de energía
│   │   │   ├── trainer/             # Efectos de entrenadores
│   │   │   ├── ability/             # Habilidades de Pokémon
│   │   │   ├── event/               # Eventos de juego
│   │   │   ├── victory/             # Condiciones de victoria
│   │   │   ├── setup/               # Setup de partida
│   │   │   └── ports/               # Puertos (hexagonal)
│   │   ├── services/                # Servicios de negocio
│   │   ├── mappers/                 # Mapeo entidad ↔ DTO
│   │   ├── repositories/            # JPA repositories y entities
│   │   ├── security/                # JWT + Spring Security
│   │   ├── websocket/               # WebSocket STOMP
│   │   └── clients/                 # Clientes HTTP externos
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── application-dev.properties
│   │   └── db/migration/            # Migraciones Flyway
│   ├── docs/informe-tecnico.md      # Documentación técnica
│   └── pom.xml
├── FE/                              # Frontend Angular
│   ├── src/app/
│   │   ├── app.ts                   # Componente raíz
│   │   ├── core/                    # API, auth, WebSocket
│   │   ├── features/                # Módulos funcionales
│   │   └── shared/                  # Componentes compartidos
│   └── package.json
├── docker-compose.yml               # PostgreSQL
└── docs/                            # Documentación adicional
```

---

## Contratos de IA

La carpeta principal para orientar a OpenCode/OpenSpec es:

```text
/docs/contracts_ai/
```

Estos contratos definen el lenguaje común del proyecto. Ver `BE/docs/informe-tecnico.md` para documentación detallada de la API, decisiones de diseño y despliegue.

---

## Reglas de Git

### No subir archivos generados

- `node_modules/`
- `dist/`
- `.angular/`
- `target/`
- logs
- archivos `.env` locales
- configuraciones personales del IDE

Usar el archivo `.gitignore` del proyecto.

### Commits

Formato: `<tipo>(<scope>): <descripción>`

Tipos: `feat`, `fix`, `test`, `refactor`, `docs`, `chore`
Scopes: `engine`, `attack`, `turn`, `decks`, `match`, `api`, `websocket`, `frontend`

Ejemplos:
```
feat(engine): add turn phase validation skeleton
fix(decks): adjust deck validation contract
docs(readme): document project structure
test(attack): add damage calculator cases
```
