# Módulo de Perfil (Frontend)

## Rutas

El módulo de perfil se carga de forma lazy (`loadComponent`) con Angular standalone. La ruta base es `/profile` y las rutas se definen en `routes.ts`:

```typescript
export const profileRoutes: Routes = [
  { path: '', loadComponent: () => import('./pages/profile-page/profile-page').then(m => m.ProfilePage) },
];
```

## Player models (`player.models.ts`)

```typescript
export interface PlayerResponse {
  id: string;
  displayName: string;
  userId: string;
  createdAt: string;
  avatarUrl?: string | null;
}

export interface UpdatePlayerRequest {
  displayName: string;
}
```

## Player API service (`player-api.service.ts`)

Servicio que comunica con el backend para operaciones CRUD:

| Método | Endpoint | Descripción |
|---|---|---|
| `listAll()` | `GET /api/players` | Listar jugadores |
| `getById(id)` | `GET /api/players/{id}` | Obtener jugador |
| `update(id, request)` | `PUT /api/players/{id}` | Actualizar datos |
| `uploadAvatar(playerId, file)` | `POST /api/players/{id}/avatar` | Subir avatar (usa `FormData`) |

`uploadAvatar` usa `HttpClient` directamente (no el `ApiClientService`) porque necesita enviar `multipart/form-data`.

## Avatar service (`avatar.service.ts`)

```typescript
export class AvatarService {
  resolve(relativePath: string | null | undefined): string | null {
    if (!relativePath) return null;
    return this.uploadsUrl + relativePath;
  }
}
```

Concatena la URL base de uploads con la ruta relativa del avatar. Por ejemplo, si el backend guardó `avatars/abc-123.png`, el servicio resuelve `http://localhost:8080/uploads/avatars/abc-123.png`.

## Profile page (`profile-page.ts`, `.html`, `.css`)

La página de perfil tiene dos modos visuales: **vista** y **edición**.

### Modo vista

Cuando `editMode()` es `false`, se muestran cuatro secciones en una tarjeta (`profile-card`):

1. **Perfil del entrenador** (`card-identity`):
   - Avatar (imagen o placeholder con iniciales).
   - Botón "Cambiar avatar" que abre el avatar picker.
   - Nombre del entrenador (`displayName`).
   - ID del entrenador (`trainerId`).
   - Email del usuario.

2. **Métricas** (`card-metrics`): grid de 6 columnas con:
   - Ranking (#posición).
   - Victorias / Derrotas.
   - Win rate (%).
   - Racha actual / Racha máxima.

3. **Información secundaria** (`card-secondary`): fila con:
   - Tiempo jugado (actualizado cada segundo con `setInterval`).
   - Fecha de inicio.
   - Cantidad de mazos.

4. **Configuración de audio** (`card-audio`): sliders para volumen general y de tablero.

5. **Botón "EDITAR PERFIL"** que activa el modo edición.

### Modo edición

Cuando `editMode()` es `true`, se muestra un formulario con:

- **Nombre de entrenador** (`displayNameControl`): validado con:
  - `required`
  - `minLength(2)`
  - `maxLength(30)`
- **Email** (`emailControl`): validado con `required` y `email`.

Botones:
- **GUARDAR**: dispara `onSave()`.
- **CANCELAR**: vuelve al modo vista sin guardar.
- **"Cambiar contraseña"**: abre modal de cambio de contraseña.
- **"Eliminar cuenta"**: abre modal de eliminación con confirmación de contraseña.

### Flujo de guardado (`onSave()`)

1. Valida que `displayNameControl` y `emailControl` sean válidos.
2. Compara los valores actuales del formulario contra los valores originales:
   - Si `displayName` cambió: llama a `authService.updatePlayer(playerId, newName)`.
   - Si `email` cambió: llama a `authService.updateUser(userId, { email })`.
3. Si solo un campo cambió, hace la llamada única.
4. Si ambos cambiaron, primero actualiza el player y luego (en el `next`) actualiza el email (secuencial).
5. Muestra notificaciones de éxito o error según el resultado.

### Carga de datos (`ngOnInit` y `loadStats`)

Al inicializar:
- `emailControl` y `displayNameControl` se llenan con los datos actuales del usuario y player (obtenidos de `AuthService`).
- Se inicia un intervalo de 1 segundo para actualizar el tiempo jugado.
- Se cargan estadísticas: stats del jugador, ranking y mazos mediante `RankingApiService` y `DeckApiService`.

Cuando el avatar cambia (efecto en `constructor`), se resetea `avatarLoadError` para reintentar la carga de la imagen.

## Avatar picker dialog (`avatar-picker-dialog.component.ts`)

Componente standalone que se muestra como un modal cuando `showAvatarPicker()` es `true`.

### Inputs
| Input | Descripción |
|---|---|
| `playerId` (required) | ID del jugador |
| `avatarUrl` | URL actual del avatar |
| `displayName` | Nombre para mostrar iniciales |

### Outputs
| Output | Descripción |
|---|---|
| `confirmed` | Se emite cuando la subida fue exitosa |
| `cancelled` | Se emite cuando el usuario cierra sin subir |

### Flujo de selección

1. El usuario hace clic en "Seleccionar archivo" → se dispara `triggerFileInput()` que hace clic en un `<input type="file">` oculto.
2. El input acepta solo `.png, .jpg, .jpeg, .webp` (y sus MIME types correspondientes).
3. `onFileSelected(event)` valida:
   - Extensión y MIME type (solo PNG, JPG, WEBP).
   - Tamaño máximo de 2 MB.
4. Si la validación pasa, se muestra un preview del archivo usando `URL.createObjectURL(file)`.
5. El usuario hace clic en "Subir avatar".
6. `onUpload()` llama a `authService.updateAvatar(playerId, file)`.
7. En caso de éxito: emite `confirmed` y limpia el estado.
8. En caso de error: muestra mensaje según el código de estado HTTP (413 = tamaño excedido, 415 = tipo no soportado, otros = error genérico).

### Limpieza

El componente gestiona correctamente la memoria liberando los object URLs creados con `URL.revokeObjectURL()` tanto al cancelar como al destruirse (`ngOnDestroy` / `DestroyRef`).

## Mapa de archivos

```
FE/src/app/features/profile/
  routes.ts                                       → Configuración de ruta /profile
  pages/profile-page/
    profile-page.ts                               → Lógica de la página (componente)
    profile-page.html                             → Template HTML
    profile-page.css                              → Estilos (tema oscuro, pixel art)
  components/avatar-picker-dialog/
    avatar-picker-dialog.component.ts             → Modal selector de avatar

FE/src/app/core/
  api/player-api.service.ts                       → Llamadas HTTP al backend
  services/avatar.service.ts                      → Resolución de URLs de avatar
  services/auth.service.ts                        → Autenticación (provee user, player)

FE/src/app/shared/models/
  player.models.ts                                → Interfaces PlayerResponse, UpdatePlayerRequest
```
