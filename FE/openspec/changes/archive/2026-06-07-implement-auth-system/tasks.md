## 1. Modelos TypeScript

- [x] 1.1 Crear `shared/models/user.models.ts` con interfaces `UserResponse` (id, email, displayName, playerId), `CreateUserRequest` (email, password, displayName), `LoginRequest` (email, password)
- [x] 1.2 Crear `shared/models/player.models.ts` con interfaces `PlayerResponse` (id, displayName, userId, createdAt), `UpdatePlayerRequest` (displayName)

## 2. API Services

Seguir el patrón de `card-api.service.ts`: inyectar `ApiClientService`, usar `this.apiClient.get/post/put` con paths relativos (ya incluyen `/api`). Sin interceptores, sin reintentos, sin manejo de errores extra — solo retornar el Observable.

- [x] 2.1 Crear `core/api/user-api.service.ts`. Métodos: `register(request)` → POST `/users/register`, `login(request)` → POST `/users/login`, `getById(id)` → GET `/users/${id}`, `listAll()` → GET `/users`
- [x] 2.2 Crear `core/api/player-api.service.ts`. Métodos: `listAll()` → GET `/players`, `getById(id)` → GET `/players/${id}`, `update(id, request)` → PUT `/players/${id}`

## 3. AuthService

- [x] 3.1 Crear `core/services/auth.service.ts`:
  - Signals privadas: `_user` (signal<UserResponse | null>), `_player` (signal<PlayerResponse | null>), `_isAuthenticated` (signal(false))
  - Getters públicos readonly: `user`, `player`, `isAuthenticated`
  - `playerId` como `computed(() => this._player()?.id ?? null)`
  - `register(request)`: llama a `UserApiService.register()`, en `tap()`: setea `_user`, llama a `loadPlayer(user.playerId)`, persiste en localStorage
  - `login(email, password)`: llama a `UserApiService.login()`, misma lógica que register
  - `loadPlayer(playerId)`: llama a `PlayerApiService.getById(playerId)` y setea `_player`. **Nota**: el spec dice `loadPlayer(userId)` pero es un bug — `UserResponse` tiene `playerId`, recibir `playerId` directamente
  - `logout()`: resetea las 3 signals a null/false y ejecuta `localStorage.removeItem('auth_user')`
  - Constructor: llama a `loadState()` que lee `localStorage.getItem('auth_user')`, parsea como `UserResponse`, setea `_user` y llama a `loadPlayer(user.playerId)`
  - Persistencia: guardar solo `UserResponse` (incluye `playerId`) bajo la clave `auth_user`

## 4. Feature Auth

- [x] 4.1 Crear `features/auth/routes.ts` con ruta `register` lazy-loaded via `loadComponent`
- [x] 4.2 Crear `features/auth/pages/register-page/register-page.ts`:
  - Formulario reactivo con `FormGroup`: email (required + email validator), password (required + minLength(6)), displayName (required)
  - OnPush, standalone, `inject()` para Router, AuthService, NotificationService
  - Al submit: llama a `authService.register()`. En `subscribe({ next, error })`:
    - Success: `notificationService.show('Registro exitoso', 'success')`, luego `setTimeout(() => router.navigate(['/decks']), 1500)`
    - Error: parsear `err.error` como `ApiErrorModel`, mostrar `notificationService.show(apiError.message, 'error')`
  - Errores del backend se muestran como toast, NO inline en campos del formulario

## 5. Integración con rutas

- [x] 5.1 Modificar `app.routes.ts`: agregar `{ path: 'auth', loadChildren: () => import('./features/auth/routes').then(m => m.authRoutes) }` antes de la ruta wildcard

## 6. Integración con facade services

- [x] 6.1 Modificar `core/api/match-api.service.ts`: agregar `player1Id: string` (requerido) a `CreateMatchRequest`, agregar `playerId: string` (requerido) a `JoinMatchRequest`
- [x] 6.2 Modificar `features/match/services/match-facade.service.ts`:
  - Inyectar `AuthService` via `inject(AuthService)`
  - En `createMatch()`: agregar `player1Id: this.authService.playerId()` al objeto request de `this.matchApi.createMatch()`
  - En `joinMatch()`: agregar `playerId: this.authService.playerId()` al objeto request de `this.matchApi.joinMatch()`
  - `getMatchState()` ya usa `this._playerId()` internamente, no cambiar
  - NO modificar las firmas de los métodos (siguen recibiendo los mismos parámetros)
- [x] 6.3 Modificar `features/decks/services/deck-builder-facade.service.ts`:
  - Inyectar `AuthService` via `inject(AuthService)`
  - Eliminar el parámetro `playerId` de la firma de `createDeck()`: de `createDeck(name, playerId)` a `createDeck(name)`
  - Dentro del método: usar `this.authService.playerId()` como valor de `playerId` en el request a `this.deckApi.create()`

## 7. Verificación (ejecutar al final)

- [x] 7.1 Ejecutar `npm run build` — debe compilar sin errores
- [x] 7.2 Ejecutar `npm test` — el test existente en `app.spec.ts` debe pasar
