# Módulo de Perfil (Backend)

## Entidad Player y su relación con User

`PlayerEntity` (`BE/src/.../repositories/entities/PlayerEntity.java`) representa el perfil público de un jugador dentro del juego. Está mapeada a la tabla `players` y tiene una relación **uno a uno** con `UserEntity` (tabla `users`):

| Campo | Tipo | Descripción |
|---|---|---|
| `id` | `UUID` | Identificador único del player |
| `user` | `UserEntity` (FK: `user_id`) | Referencia lazy al usuario del sistema |
| `displayName` | `String` (varchar 80) | Nombre visible del entrenador |
| `avatarUrl` | `String` (varchar 255) | Ruta relativa del avatar |
| `createdAt` | `Instant` | Fecha de creación (autogenerado con `@PrePersist`) |
| `decks` | `List<DeckEntity>` | Mazos propiedad del jugador (relación uno a muchos) |

La relación con `UserEntity` es bidireccional: `UserEntity` tiene un campo `player` con `mappedBy = "user"` y `CascadeType.ALL`, lo que significa que al crear un `UserEntity` se puede persistir su `PlayerEntity` asociado automáticamente.

`PlayerEntity` usa Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) y JPA (`@Entity`, `@Table`). El campo `user` tiene fetch LAZY para no cargar datos de autenticación innecesariamente.

## Repositorio

`PlayerJpaRepository` (`BE/src/.../repositories/jpa/PlayerJpaRepository.java`) extiende `JpaRepository<PlayerEntity, UUID>`. Es un repositorio Spring Data JPA estándar que provee métodos CRUD básicos (`findById`, `findAll`, `save`, `existsById`, etc.).

## DTOs

### PlayerResponse
Record inmutable devuelto al frontend:

```java
public record PlayerResponse(
    String id,           // UUID como string
    String displayName,  // Nombre visible
    String userId,       // ID del usuario asociado
    Instant createdAt,   // Fecha de creación
    String avatarUrl     // Ruta relativa del avatar (puede ser null)
) {}
```

### UpdatePlayerRequest
Record que recibe el frontend para actualizar el perfil:

```java
public record UpdatePlayerRequest(String displayName) {}
```

Solo se puede actualizar el `displayName` desde el request. El email y contraseña se manejan desde el módulo de usuario.

## Endpoints CRUD (PlayerController)

El controlador expone los siguientes endpoints bajo `/api/players`:

### `GET /api/players`
Lista todos los jugadores. Retorna `List<PlayerResponse>`.

### `GET /api/players/{id}`
Obtiene un jugador por UUID. Retorna `PlayerResponse` o lanza `NotFoundException` si no existe.

### `PUT /api/players/{id}`
Actualiza el perfil del jugador. Recibe un `UpdatePlayerRequest` en el body. Retorna `PlayerResponse`.

### `POST /api/players/{id}/avatar`
Sube un avatar. Endpoint que consume `multipart/form-data`. Recibe el archivo en el parámetro `file`.

## Avatar upload/storage system

El flujo de subida de avatar funciona así:

1. El frontend envía un archivo `MultipartFile` al endpoint `POST /api/players/{id}/avatar`.
2. `PlayerController` delega en `AvatarStorageService.store(file)`.
3. `AvatarStorageService` valida el archivo:
   - Tipos permitidos: `image/png`, `image/jpeg`, `image/webp`.
   - Tamaño máximo: **2 MB** (constante `MAX_SIZE_BYTES`).
4. Si la validación falla, lanza `ValidationException`.
5. Si pasa, genera un nombre único con `UUID.randomUUID()` y resuelve la extensión según el content type.
6. Guarda el archivo en el directorio configurado por `${app.upload.dir}`.
7. Almacena la ruta relativa (`avatars/<uuid>.<ext>`).
8. Retorna la ruta relativa al controlador.
9. `PlayerController` llama a `PlayerService.updateAvatar(id, avatarFileName)` que:
   - Busca el `PlayerEntity` por ID.
   - Actualiza `avatarUrl` con la ruta relativa.
   - Persiste y devuelve `PlayerResponse`.

## Profile update flow (servicio)

`PlayerService.update(id, request)`:

1. Busca `PlayerEntity` por ID. Si no existe, lanza `NotFoundException`.
2. Actualiza `displayName` en la entidad.
3. **Importante**: también actualiza `username` en el `UserEntity` asociado (si existe), manteniendo sincronizados el nombre de entrenador y el nombre de usuario.
4. Persiste con `playerJpaRepository.save(entity)`.
5. Convierte la entidad a `PlayerResponse` usando el método privado `toResponse()`.

El método `toResponse()` extrae los campos y resuelve el `userId` desde `entity.getUser().getId()`, manejando el caso de que `user` sea null.

## Mapa de archivos

```
BE/src/.../controllers/players/PlayerController.java    → Endpoints REST
BE/src/.../services/players/PlayerService.java           → Lógica de negocio
BE/src/.../services/players/AvatarStorageService.java    → Almacenamiento de avatares
BE/src/.../repositories/entities/PlayerEntity.java       → Entidad JPA
BE/src/.../repositories/entities/UserEntity.java         → Entidad de usuario relacionada
BE/src/.../repositories/jpa/PlayerJpaRepository.java     → Repositorio Spring Data
BE/src/.../dtos/players/PlayerResponse.java             → DTO de respuesta
BE/src/.../dtos/players/UpdatePlayerRequest.java        → DTO de actualización
```
