# Autenticación (Frontend)

## Visión general

El frontend en **Angular 19 standalone** proporciona dos páginas de autenticación: **Login** y **Register**. El estado de sesión se gestiona mediante un `AuthService` que almacena el usuario y token JWT en `localStorage` y los expone como signals reactivas. Un interceptor HTTP adjunta automáticamente el token a cada request, y dos route guards protegen las rutas según si el usuario está autenticado o no.

## Arquitectura

```
LoginPage / RegisterPage (componentes)
    ↓
AuthService (servicio central de sesión)
    ↓
UserApiService (llamadas HTTP al backend)
    ↓
ApiClientService (cliente HTTP genérico → http://localhost:8080/api)

Seguridad:
    authInterceptor    → adjunta Bearer token a cada request
    authGuard          → protege rutas (redirige a /auth/login si no auth)
    alreadyAuthGuard   → redirige a /home si ya está autenticado
```

---

## Clases y servicios

### `UserApiService` (`core/api/user-api.service.ts`)

Servicio que encapsula las llamadas HTTP a la API de usuarios del backend. Usa `ApiClientService` (base URL `http://localhost:8080/api`). Métodos:

| Método             | HTTP Call                         |
|--------------------|-----------------------------------|
| `register(request)` | `POST /users/register`           |
| `login(request)`    | `POST /users/login`              |
| `getById(id)`       | `GET /users/{id}`                |
| `listAll()`         | `GET /users`                     |
| `updateUser(id, req)` | `PUT /users/{id}`             |
| `deactivateUser(id)` | `PUT /users/{id}/deactivate`   |
| `activateUser(id, password)` | `PUT /users/{id}/activate` |
| `validatePassword(id, password)` | `POST /users/{id}/validate-password` |

### `AuthService` (`core/services/auth.service.ts`)

Servicio central que maneja toda la lógica de autenticación del lado del cliente. Usa signals de Angular para el estado reactivo.

**Estado interno (signals):**
- `_user: Signal<UserResponse | null>` — datos del usuario logueado
- `_player: Signal<PlayerResponse | null>` — datos del perfil del jugador
- `_isAuthenticated: Signal<boolean>` — flag de autenticación
- `_token: Signal<string | null>` — token JWT actual

**Signals públicas de solo lectura:**
- `user`, `player`, `isAuthenticated`, `token`, `playerId` (computed)

#### Persistencia de sesión

En el constructor, `AuthService` llama a `loadState()`:

1. Lee `localStorage` con clave `"auth_user"`
2. Si existe un JSON válido, lo parsea como `UserResponse`
3. Setea las signals (`_user`, `_token`, `_isAuthenticated`)
4. Si hay `playerId`, carga los datos del jugador en el próximo microtask (`Promise.resolve().then(...)`)

En cada login/register exitoso, llama a `saveState()` que guarda el `UserResponse` completo en `localStorage`.

En `logout()`:
- Limpia todas las signals a `null`/`false`
- Elimina `"auth_user"` de `localStorage`

#### Flujo de Login

1. `LoginPage.onSubmit()` llama a `authService.login(email, password)`
2. `AuthService.login()` llama a `userApi.login({email, password})` (POST `/api/users/login`)
3. En el `tap` de la respuesta:
   - Setea `_user`, `_token`, `_isAuthenticated`
   - Carga el perfil del jugador con `loadPlayer(playerId)`
   - Persiste en `localStorage` con `saveState()`
4. `LoginPage` recibe la respuesta, muestra notificación "Inicio de sesión exitoso" y navega a `/welcome` tras 1.5s
5. **Manejo de cuenta desactivada**: Si el backend responde 403 con mensaje `"Account deactivated|{userId}"`, el login page muestra un botón para reactivar. `onReactivate()` llama a `authService.activateUser(userId, password)` y luego reintenta el login automáticamente.

#### Flujo de Register

1. `RegisterPage.onSubmit()` llama a `authService.register(form.value)` (objeto `CreateUserRequest`)
2. `AuthService.register()` llama a `userApi.register(request)` (POST `/api/users/register`)
3. En el `tap`, mismo proceso que login: setea signals, carga player, persiste
4. `RegisterPage` recibe la respuesta, muestra notificación "Registro exitoso" y navega a `/auth/login` tras 1.5s

> **Nota**: A diferencia del login, el registro también autentica al usuario inmediatamente (el backend devuelve un token). Sin embargo, la UI redirige al login en lugar de al home. Esto es intencional: se espera que el usuario confirme su registro antes de usar la app.

### `authInterceptor` (`core/interceptors/auth.interceptor.ts`)

Interceptor funcional de Angular (`HttpInterceptorFn`) que se ejecuta en cada request HTTP saliente.

1. Lee el token actual de `authService.token()` (signal)
2. Si existe, clona el request agregando el header `Authorization: Bearer <token>`
3. Si no existe, pasa el request original sin modificar

Esto asegura que **todos** los requests autenticados (incluyendo los de otras features como decks, partidas, etc.) lleven el JWT automáticamente.

### `authGuard` (`core/guards/auth.guard.ts`)

Route guard funcional (`CanActivateFn`) que protege rutas que requieren autenticación.

- Si `authService.isAuthenticated()` es `true` → permite el acceso (`return true`)
- Si no → redirige a `/auth/login` (`return router.parseUrl('/auth/login')`)

Se usa en rutas como `/home`, `/profile`, `/decks`, `/games`, etc.

### `alreadyAuthGuard` (`core/guards/already-auth.guard.ts`)

Route guard funcional que evita que usuarios ya autenticados vean páginas de login/register.

- Si `authService.isAuthenticated()` es `false` → permite el acceso (`return true`)
- Si no → redirige a `/home` (`return router.parseUrl('/home')`)

Se usa en las rutas de auth (`/auth/login`, `/auth/register`).

### `LoginPage` (`features/auth/pages/login-page/login-page.ts`)

Componente standalone con `ChangeDetectionStrategy.OnPush`. Formulario reactivo con campos `email` (required, email) y `password` (required, minLength 6).

**Estados locales (signals):**
- `loading` — indica si hay un request en curso
- `errorMessage` — mensaje de error a mostrar
- `deactivatedUserId` — si la cuenta está desactivada, almacena el userId para reactivación
- `reactivateLoading` — indica si la reactivación está en curso

**Flujo de submit:**
1. Si el formulario es inválido, marca todos los campos como tocados y retorna
2. Setea `loading = true`, limpia errores
3. Llama a `authService.login(email, password)`
4. Éxito → notificación + navegación a `/welcome`
5. Error 403 con mensaje `"Account deactivated|"` → muestra botón de reactivación
6. Otro error → muestra el mensaje del backend

### `RegisterPage` (`features/auth/pages/register-page/register-page.ts`)

Componente standalone similar al login. Formulario reactivo con campos `email` (required, email), `password` (required, minLength 6), `displayName` (required).

**Flujo de submit:**
1. Validación del formulario
2. Llama a `authService.register(form.value)`
3. Éxito → notificación + navegación a `/auth/login`
4. Error → muestra mensaje del backend (parseado como `ApiErrorModel`)

### Rutas de autenticación (`features/auth/routes.ts`)

Configuración lazy-loaded para las rutas `/auth/login` y `/auth/register`:

```typescript
export const authRoutes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login-page/login-page').then(m => m.LoginPage) },
  { path: 'register', loadComponent: () => import('./pages/register-page/register-page').then(m => m.RegisterPage) },
];
```

Ambos componentes se cargan de forma lazy (solo se descargan cuando el usuario visita una ruta de auth).

### Modelos (`shared/models/user.models.ts`)

```typescript
interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  playerId: string;
  token?: string;
}

interface CreateUserRequest {
  email: string;
  password: string;
  displayName: string;
}

interface LoginRequest {
  email: string;
  password: string;
}

interface UpdateUserRequest {
  email?: string;
  currentPassword?: string;
  newPassword?: string;
}
```

### `ApiClientService` (`core/api/api-client.service.ts`)

Cliente HTTP genérico que wrappea `HttpClient` de Angular. Base URL hardcodeada: `http://localhost:8080/api`. Proxys todos los métodos HTTP (`get`, `post`, `put`, `delete`) y retorna `Observable<T>`.

---

## Resumen del flujo completo

### Login
```
LoginPage → AuthService.login() → UserApiService.login() → POST /api/users/login
                                                                      ↓
Backend: UserService.login() → verifica credenciales → genera JWT → respuesta
                                                                      ↓
AuthService: guarda token + user en signals y localStorage
LoginPage: notifica éxito → navega a /welcome
```

### Register
```
RegisterPage → AuthService.register() → UserApiService.register() → POST /api/users/register
                                                                          ↓
Backend: UserService.register() → valida unicidad → hashea password → crea User+Player → JWT
                                                                          ↓
AuthService: guarda token + user en signals y localStorage
RegisterPage: notifica éxito → navega a /auth/login
```

### Request autenticado
```
Component → ApiClientService.get('/users/me')
                ↓
authInterceptor: agrega header "Authorization: Bearer <token>"
                ↓
Backend: JwtAuthenticationFilter extrae token, valida, setea SecurityContext
                ↓
Controller: procesa request con usuario autenticado
```
