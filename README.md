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

Proyecto base para el Trabajo Práctico Integrador de Programación III: implementación digital de **Pokémon Trading Card Game** con **Spring Boot** en backend y **Angular** en frontend.

> Estado actual: este repositorio es una **estructura inicial de trabajo**. No representa todavía un MVP funcional del juego. Su objetivo es servir como base común para que el equipo implemente el MVP de forma ordenada, manteniendo contratos compartidos para backend, frontend, persistencia, WebSocket y lógica de juego.

---

## Objetivo del proyecto

Construir una versión digital funcional de Pokémon TCG basada en el reglamento XY1, con:

- Backend como única fuente de verdad del estado de la partida.
- Frontend como capa de presentación e interacción.
- Motor de reglas para setup, turnos, ataques, daño, knockouts, premios, condiciones especiales y victoria.
- Construcción y validación de mazos.
- Persistencia del estado de partida y log de acciones.
- Comunicación en tiempo real mediante WebSockets.
- Integración con datos de cartas desde caché local alimentado por la API pública de Pokémon TCG.

---

## Estado actual del repositorio

Este proyecto contiene principalmente estructura, contratos y stubs iniciales.

### Incluido actualmente

- Proyecto backend Spring Boot con Java 21.
- Proyecto frontend Angular.
- Estructura modular por dominio:
  - `cards`
  - `decks`
  - `matches`
  - `engine`
  - `common`
  - `config`
- Entidades JPA iniciales.
- Repositorios iniciales.
- DTOs base para cartas, mazos, partidas y acciones.
- Migración inicial de base de datos con Flyway.
- Configuración inicial de WebSocket.
- Contratos de IA en `/docs/contracts_ai/`.
- Specs iniciales en `/openspec/specs/`.
- Comandos y skills para OpenCode/OpenSpec en `/.opencode/`.

### No implementado todavía

- Game Engine funcional.
- Setup completo de partida.
- Mulligan funcional.
- Turn Manager funcional.
- Validación real de acciones.
- Resolución real de ataques.
- Cálculo real de daño, debilidad y resistencia.
- Knockouts y toma de cartas de Premio.
- Condiciones especiales.
- Persistencia completa del estado luego de cada acción.
- Controllers REST funcionales.
- WebSocket funcional para sincronización de partida.
- Deck Builder completo.
- Frontend jugable.
- Tests unitarios e integración relevantes.

---

## Stack técnico

### Backend

- Java 21
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring WebSocket
- Flyway
- PostgreSQL
- H2 para tests

### Frontend

- Angular
- TypeScript
- RxJS

> Nota: revisar la versión de Angular requerida por la cátedra antes de avanzar demasiado en frontend. Si el instructivo exige una versión superior a la actual, conviene actualizar temprano.

---

## Estructura general

```text
.
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pokemontcg/
│       │   │   ├── cards/
│       │   │   ├── decks/
│       │   │   ├── matches/
│       │   │   ├── engine/
│       │   │   ├── common/
│       │   │   └── config/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       └── test/
├── frontend/
│   ├── package.json
│   └── src/app/
│       ├── core/
│       ├── features/
│       └── shared/
├── docs/
│   ├── contracts_ai/
├── openspec/
│   ├── config.yaml
│   └── specs/
├── .opencode/
├── Workflow.md
└── README.md
```

---

## Contratos de IA

La carpeta principal para orientar a OpenCode/OpenSpec es:

```text
/docs/contracts_ai/
```

Estos contratos definen el lenguaje común del proyecto:

- reglas generales del proyecto;
- estructura esperada;
- enums compartidos;
- modelo de cartas;
- contratos de mazos;
- estado de partida;
- flujo de setup;
- acciones de juego;
- validaciones;
- pipeline de ataque;
- condiciones especiales;
- persistencia y logs;
- REST API;
- WebSocket;
- estado frontend;
- escenarios de test.

### Regla de trabajo

Si una implementación necesita cambiar DTOs, endpoints, eventos, enums, estados, acciones o reglas, primero debe actualizarse el contrato correspondiente.

El orden recomendado es:

```text
1. Leer contrato.
2. Crear proposal en OpenSpec.
3. Revisar alcance.
4. Aplicar cambio.
5. Implementar código.
6. Agregar tests o pasos de verificación.
```

---

## Cómo levantar el backend

### 1. Requisitos

- Java 21 instalado.
- Maven instalado o Maven Wrapper si se agrega luego.
- PostgreSQL corriendo localmente.

### 2. Crear base de datos

Revisar el archivo:

```text
backend/src/main/resources/application.yml
```

La configuración actual espera una base PostgreSQL local. Crear la base con el nombre configurado o ajustar el `application.yml` a la base local de cada integrante.

Ejemplo desde `psql`:

```sql
CREATE DATABASE V1__init_schema;
```

### 3. Ejecutar backend

Desde la carpeta `backend`:

```bash
mvn spring-boot:run
```

O para validar compilación y tests:

```bash
mvn clean test
```

El backend usa por defecto:

```text
http://localhost:8080
```

---

## Cómo levantar el frontend

### 1. Requisitos

- Node.js instalado.
- npm instalado.
- Angular CLI instalado o usar `npx ng`.

### 2. Instalar dependencias

Desde la carpeta `frontend`:

```bash
npm install
```

### 3. Ejecutar frontend

```bash
npm start
```

Por defecto Angular levanta en:

```text
http://localhost:4200
```

---

## Flujo de trabajo recomendado para el equipo

Cada integrante debe trabajar sobre una feature o módulo concreto, evitando modificar áreas ajenas sin coordinación.

### División sugerida

| Área | Carpetas principales | Contratos relacionados |
|---|---|---|
| Game Engine / turnos | `backend/.../engine/turn`, `engine/rules` | `08`, `09` |
| Setup / mulligan | `backend/.../engine/setup` | `07` |
| Ataques y daño | `backend/.../engine/attack` | `10` |
| Condiciones especiales | `backend/.../engine/status` | `11` |
| Victoria / knockout | `backend/.../engine/victory` | `06`, `10`, `11` |
| Mazos | `backend/.../decks` | `05` |
| Cartas / caché local | `backend/.../cards` | `04` |
| Partidas / persistencia | `backend/.../matches` | `12`, `13` |
| WebSocket | `backend/.../matches/websocket`, `config/WebSocketConfig` | `14` |
| Frontend partida | `frontend/src/app/features/match` | `15` |
| Frontend mazos | `frontend/src/app/features/decks` | `05`, `15` |
| Lobby | `frontend/src/app/features/lobby` | `13`, `14`, `15` |

---

## Reglas de Git

### No subir archivos generados

No deben subirse:

- `node_modules/`
- `dist/`
- `.angular/`
- `target/`
- logs
- archivos `.env` locales
- configuraciones personales del IDE

Usar el archivo `.gitignore` del proyecto.

### Branches sugeridas

```text
main
feature/game-engine-turns
feature/setup-mulligan
feature/attack-pipeline
feature/deck-builder
feature/websocket-sync
feature/frontend-match-board
```

### Commits sugeridos

Formato simple:

```text
feat(engine): add turn phase validation skeleton
fix(decks): adjust deck validation contract
 docs(readme): document project structure
 test(attack): add damage calculator cases
```

---

## Orden recomendado de implementación del MVP

1. Limpiar repositorio y confirmar `.gitignore`.
2. Confirmar contratos canónicos en `/docs/contracts_ai/`.
3. Crear seed mínimo de cartas y dos mazos válidos.
4. Implementar validación de mazos.
5. Implementar setup de partida.
6. Implementar turnos y fases.
7. Implementar acciones básicas:
   - colocar básico en banca;
   - unir energía;
   - atacar;
   - finalizar turno.
8. Implementar cálculo de daño.
9. Implementar knockout y premios.
10. Implementar condiciones de victoria.
11. Implementar condiciones especiales.
12. Exponer endpoints REST.
13. Sincronizar por WebSocket.
14. Implementar frontend mínimo jugable.
15. Agregar tests unitarios y de integración.
16. Actualizar documentación final.

---

## Criterio de arquitectura

- El backend decide todo.
- El frontend no calcula reglas de juego.
- El frontend envía acciones.
- El backend valida acciones.
- El backend genera eventos.
- El backend persiste estado y logs.
- El frontend renderiza la vista recibida.
- Las cartas usadas en partida deben salir del caché local, no de llamadas directas a la API externa.

---

## Advertencias importantes

- Este repositorio no debe presentarse como MVP funcional todavía.
- La estructura está preparada para construir el MVP, pero falta implementar la lógica real.
- No modificar contratos sin registrar el cambio.
- No implementar reglas inventadas fuera del reglamento o del instructivo.
- No duplicar fuentes de verdad entre backend, frontend y documentación.

---

## Documentación relevante

- `Workflow.md`: flujo de trabajo con OpenSpec/OpenCode.
- `/docs/contracts_ai/`: contratos principales para IA e implementación.
- `/openspec/specs/`: specs iniciales del proyecto.
