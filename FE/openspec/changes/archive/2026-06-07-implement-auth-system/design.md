## Context

El frontend es una app Angular 20.3 con arquitectura feature-based, signals, standalone components y OnPush. El backend expone endpoints REST para Users y Players, pero el frontend no los consume. Actualmente el `playerId` se pasa como parámetro suelto en `MatchFacadeService` y `DeckBuilderFacadeService`. El spec `auth-front-spec.md` define la estructura de los DTOs y servicios a crear.

El componente raíz `App` ya incluye un `NotificationComponent` global para toasts. Los facade services usan `inject()` y signals. La ruta raíz redirige a `/decks`.

## Goals / Non-Goals

**Goals:**
- Crear `AuthService` con signals que maneje sesión de usuario (user, player, isAuthenticated)
- Persistir sesión en localStorage para sobrevivir recargas de página
- Crear página de registro con formulario reactivo y manejo de errores
- Integrar `AuthService` con `MatchFacadeService` y `DeckBuilderFacadeService`
- Mantener compatibilidad: playerId como parámetro opcional en facades existentes

**Non-Goals:**
- No se crea página de login (solo register por ahora)
- No se crean auth guards para proteger rutas
- No se agrega token JWT ni interceptors HTTP (el backend maneja sesión por ahora)
- No se modifica la UI de las pages stub (deck-list, deck-builder, lobby, match)

## Decisions

### 1. AuthService como signals singleton
**Decisión**: Usar `signal()` para `_user`, `_player`, `_isAuthenticated` y `computed()` para `playerId`. Todo via `providedIn: 'root'`.

**Alternativa considerada**: BehaviorSubjects (patrón más clásico). Se descartó porque signals es el patrón establecido en todo el codebase y da mejor integración con OnPush.

### 2. Persistencia en localStorage
**Decisión**: `saveState()` serializa `_user` y `_player` a localStorage. `loadState()` se llama en el constructor del servicio para restaurar sesión al iniciar la app.

**Rango de persistencia**: Solo user y player (no se persiste `_isAuthenticated` — se computa de `user !== null`).

### 3. playerId como parámetro opcional en facades
**Decisión**: En `MatchFacadeService` y `DeckBuilderFacadeService`, el parámetro `playerId` se vuelve opcional. Si se pasa, se usa el valor; si no, se obtiene de `authService.playerId()`.

**Razón**: El usuario pidió mantener compatibilidad. Esto permite que el código funcione tanto con usuarios logueados como sin ellos (durante transición).

### 4. Register page con Reactive Forms
**Decisión**: Usar `ReactiveFormsModule` con `FormGroup` y `FormControl`. Validators: required, email, minLength(6) para password.

**Alternativa considerada**: Template-driven forms. Se descartó porque el spec indica explícitamente "formulario reactivo" y el resto del codebase no usa forms todavía (no hay precedente).

### 5. Post-registro: toast + redirect
**Decisión**: Después del registro exitoso, mostrar toast de éxito via `NotificationService.show()` y redirigir a `/decks` con `Router.navigate()` después de un delay de 1.5s.

### 6. Manejo de errores del backend
**Decisión**: Parsear la respuesta de error del backend (que sigue el formato `ApiErrorModel`定义 en `api-error.models.ts`) y mostrar el campo `message` en la UI, debajo del formulario.

## Risks / Trade-offs

- **[Riesgo] localStorage obsoleto**: Si el usuario cambia password en otro dispositivo, la sesión local queda con datos viejos → Mitigación: Aceptar por ahora, el backend no invalida sesiones
- **[Riesgo] playerId null**: Si el usuario no está logueado y llama a createMatch/createDeck, `playerId()` será null → Mitigación: Los facades mantienen compatibilidad con parámetro opcional, pero la UI de match/deck no está implementada aún (stubs)
- **[Trade-off] Sin login page**: El método `login()` existe en AuthService pero no hay UI → Se acepta para no salir del scope del spec
- **[Trade-off] Sin guards**: Cualquier ruta es accesible sin loguearse → Se acepta porque las pages stub no funcionan aún
