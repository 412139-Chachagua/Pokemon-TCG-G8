## Why

El backend ya separa Users (cuenta con email/password) de Players (perfil de juego), pero el frontend no tiene integración con esta lógica. No hay registro, ni login, y el `playerId` se pasa manualmente como parámetro suelto. Esto impide que cada usuario tenga sus propios decks y partidas. El spec `auth-front-spec.md` define los endpoints y estructuras necesarias.

## What Changes

- Nuevo servicio `AuthService` con signals para manejar estado de autenticación (`_user`, `_player`, `_isAuthenticated`), con `playerId` como computed signal y persistencia en localStorage
- Nuevos servicios de API: `UserApiService` (register, login, getById, listAll) y `PlayerApiService` (listAll, getById, update)
- Nueva página de registro en `/auth/register` con formulario reactivo (email, password, displayName) y manejo de errores del backend
- `MatchFacadeService` y `DeckBuilderFacadeService` obtienen `playerId` del `AuthService` inyectado, manteniendo compatibilidad con parámetro opcional
- `CreateMatchRequest` y `JoinMatchRequest` ahora incluyen `playerId` como campo requerido
- Modelos TypeScript nuevos: `UserResponse`, `CreateUserRequest`, `LoginRequest`, `PlayerResponse`, `UpdatePlayerRequest`

## Capabilities

### New Capabilities
- `user-registration`: Página de registro, validación de formulario reactivo, llamada a POST /api/users/register, manejo de errores del backend, redirect a /decks con toast de éxito
- `auth-state-management`: AuthService con signals, persistencia en localStorage, integración con MatchFacadeService y DeckBuilderFacadeService para obtener playerId automáticamente

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **Archivos nuevos** (7): shared/models/user.models.ts, shared/models/player.models.ts, core/api/user-api.service.ts, core/api/player-api.service.ts, core/services/auth.service.ts, features/auth/routes.ts, features/auth/pages/register-page/register-page.ts
- **Archivos modificados** (4): app.routes.ts (+ruta auth), core/api/match-api.service.ts (+player1Id, +playerId en interfaces), features/match/services/match-facade.service.ts (+AuthService), features/decks/services/deck-builder-facade.service.ts (+AuthService)
- **API backend**: Se consumen endpoints /api/users/register, /api/users/login, /api/players/*
- **Dependencias**: No se agregan dependencias nuevas, se usa ReactiveFormsModule existente
