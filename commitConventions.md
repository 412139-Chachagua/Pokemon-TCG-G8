# Convenciones de commits — Pokémon TCG TPI

---

## Anatomía de un commit

```
<tipo>(<scope>): <descripción>
```

| Parte | Qué es | Ejemplo |
|---|---|---|
| `tipo` | Qué clase de cambio es | `feat` |
| `scope` | Qué parte del proyecto tocaste | `game-engine` |
| `descripción` | Qué hiciste concretamente, en minúscula, sin punto final | `implementar cálculo de daño con debilidad` |

**Ejemplo completo:**
```
feat(game-engine): implementar cálculo de daño con debilidad y resistencia
```

---

## Tipos de commit

- **feat** — Funcionalidad nueva que antes no existía. Usalo cuando agregás algo al juego o al sistema que no estaba.

- **fix** — Corrección de un bug. Algo roto que dejó de funcionar mal.

- **test** — Agregar o modificar tests. No cambia lógica de producción, solo cobertura de pruebas.

- **refactor** — Mejora interna del código sin cambiar el comportamiento. El juego sigue funcionando igual, pero el código quedó más limpio.

- **docs** — Solo documentación. Comentarios, READMEs, contratos, OpenSpec.

- **chore** — Mantenimiento: dependencias, configs de Maven/npm, scripts de build. No toca lógica de juego.

---

## Ejemplos por tipo

- **feat**
  ```
  feat(game-engine): implementar cálculo de daño con debilidad y resistencia
  feat(match): agregar endpoint POST /api/matches/{id}/actions
  feat(deck-builder): validar que el mazo tenga al menos un Pokémon Básico
  ```

- **fix**
  ```
  fix(deck-builder): corregir validación de AS TÁCTICO duplicado
  fix(turn-manager): corregir que el turno no avanzaba si el jugador no atacaba
  fix(game-engine): corregir aplicación de condición especial BURNED entre turnos
  ```

- **test**
  ```
  test(damage-calculator): agregar tests para Pokémon-EX con doble premio
  test(deck-builder): cubrir caso de mazo con más de 4 copias del mismo nombre
  test(match): agregar test de integración para flujo completo de partida
  ```

- **refactor**
  ```
  refactor(turn-manager): extraer lógica de between-turns a método privado
  refactor(game-engine): separar validación de energía en EnergyRequirementValidator
  refactor(match): mover mapeo de entidades a MatchMapper
  ```

- **docs**
  ```
  docs(api): documentar endpoint POST /api/games/{id}/actions
  docs(contracts): actualizar 06-game-state-contract con campo suddenDeath
  docs(websocket): agregar ejemplos de mensajes GAME_VIEW_UPDATED
  ```

- **chore**
  ```
  chore(deps): actualizar spring-boot a 3.4.1
  chore(deps): agregar dependencia de flyway para migraciones
  chore(config): configurar editorconfig para formato uniforme entre agentes
  ```

---

## Buenos y malos ejemplos

| ✓ Bueno | ✗ Malo | Por qué está mal |
|---|---|---|
| `feat(game-engine): implementar cálculo de daño` | `cambios en el daño` | Sin tipo, sin scope, sin precisión |
| `fix(deck-builder): corregir validación de AS TÁCTICO` | `arregle el bug ese del mazo` | "Ese bug" no le dice nada a nadie |
| `refactor(turn-manager): extraer lógica de between-turns` | `feat: mejoré varias cosas del juego` | "Varias cosas" mezcla todo en un commit |
| `test(damage-calculator): agregar tests para Pokémon-EX` | `WIP commit final` | WIP y final no dicen lo mismo |

---

## Scopes del proyecto

| Scope | Qué cubre |
|---|---|
| `game-engine` | Motor de juego (aislado de Spring, JPA y REST) |
| `damage-calculator` | Cálculo de daño, debilidad y resistencia |
| `turn-manager` | Gestión de turnos y fases |
| `deck-builder` | Construcción y validación de mazos |
| `match` | Partidas: API, entidades, lógica de sesión |
| `api` | Endpoints REST generales |
| `websocket` | Comunicación en tiempo real (STOMP) |
| `deps` | Dependencias Maven o npm |
| `config` | Configuraciones del proyecto |

---

## Regla de oro

> **Un commit = una idea.**
>
> Si tenés que poner "y" más de una vez en la descripción, partí el commit en dos.
> Si un compañero no puede entender qué hiciste sin abrir el diff, el mensaje está mal escrito.

> Los commits mezclan ingles y español a modo de explicacion, para este tpi por favor usemos ingles en su totalidad. 