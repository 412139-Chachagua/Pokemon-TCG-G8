# Autenticación

## Visión general

El sistema de autenticación permite a los usuarios registrarse e iniciar sesión en la aplicación Pokemon TCG. Está basado en **JWT (JSON Web Tokens)** con un enfoque **stateless**: el servidor no mantiene sesiones, sino que cada request autenticado incluye un token JWT en el header `Authorization`. El backend está construido con **Spring Boot 3 + Spring Security**.

## Arquitectura en capas

```
UserController (API REST)
    ↓
UserService (lógica de negocio)
    ↓
UserJpaRepository (persistencia JPA)
    ↓
UserEntity (modelo de datos)

Security:
    SecurityConfig     → define reglas HTTP, CORS, PasswordEncoder
    JwtTokenProvider   → genera y valida tokens JWT
    JwtAuthenticationFilter → intercepta requests, extrae JWT y setea SecurityContext
```

---

## Clases y responsabilidades

### `UserEntity` (`repositories/entities/UserEntity.java`)

Entidad JPA que mapea la tabla `users`. Campos:

| Campo       | Tipo     | Detalle                                |
|-------------|----------|----------------------------------------|
| `id`        | UUID     | Generado automáticamente               |
| `username`  | String   | Único, hasta 50 caracteres, NOT NULL   |
| `email`     | String   | Único, hasta 120 caracteres            |
| `password`  | String   | Hash BCrypt                            |
| `role`      | String   | Por defecto `"PLAYER"`                 |
| `status`    | String   | `"ACTIVE"` o `"INACTIVE"`              |
| `player`    | PlayerEntity | Relación OneToOne con Player      |
| `createdAt` | Instant  | Set con `@PrePersist`                  |
| `updatedAt` | Instant  | Set con `@PrePersist` y `@PreUpdate`   |

### `UserJpaRepository` (`repositories/jpa/UserJpaRepository.java`)

Interface Spring Data JPA que extiende `JpaRepository<UserEntity, UUID>`. Métodos personalizados:
- `findByUsername(String)` → `Optional<UserEntity>`
- `findByEmail(String)` → `Optional<UserEntity>`

### DTOs (`dtos/users/`)

| DTO               | Formato   | Campos                                              | Validaciones                          |
|-------------------|-----------|------------------------------------------------------|---------------------------------------|
| `CreateUserRequest` | record  | `email`, `password`, `displayName`                   | `@NotBlank`, `@Email`, `@Size`        |
| `LoginRequest`      | record  | `email`, `password`                                  | `@NotBlank`                           |
| `UpdateUserRequest` | record  | `email`, `currentPassword`, `newPassword`            | sin validaciones (pueden ser null)    |
| `UserResponse`      | record  | `id`, `email`, `displayName`, `playerId`, `token`    | -                                     |

### `UserController` (`controllers/users/UserController.java`)

Controlador REST mapeado a `/api/users`. Expone los siguientes endpoints:

| Método | Ruta                          | Auth requerida | Descripción                        |
|--------|-------------------------------|----------------|------------------------------------|
| POST   | `/api/users/register`         | No             | Registrar nuevo usuario            |
| POST   | `/api/users/login`            | No             | Iniciar sesión                     |
| GET    | `/api/users/{id}`             | Sí             | Obtener usuario por ID             |
| GET    | `/api/users`                  | Sí             | Listar todos los usuarios          |
| PUT    | `/api/users/{id}`             | Sí             | Actualizar usuario (email/password)|
| PUT    | `/api/users/{id}/deactivate`  | Sí             | Desactivar cuenta                  |
| PUT    | `/api/users/{id}/activate`    | Sí             | Reactivar cuenta                   |
| POST   | `/api/users/{id}/validate-password` | Sí      | Validar contraseña actual          |

### `UserService` (`services/users/UserService.java`)

Contiene toda la lógica de negocio. Dependencias: `UserJpaRepository`, `PlayerJpaRepository`, `PasswordEncoder`, `JwtTokenProvider`.

#### Flujo de registro (`register`)

1. Valida que el email no esté registrado → si lo está, lanza `ConflictException` ("El email ya está registrado")
2. Valida que el username no esté en uso → si lo está, lanza `ConflictException` ("El nombre de usuario ya está en uso")
3. Crea `UserEntity` con:
   - `username` = `displayName` del request
   - `password` = `passwordEncoder.encode(password)` (BCrypt)
   - `role` = `"PLAYER"`
   - `status` = `"ACTIVE"`
4. Crea `PlayerEntity` asociada al usuario con el mismo `displayName`
5. Guarda la entidad en BD (cascada desde User a Player)
6. Genera un JWT con `generateToken(userId, email, role, playerId)`
7. Retorna `UserResponse` con el token incluido

#### Flujo de login (`login`)

1. Busca usuario por email → si no existe, 401 "Invalid credentials"
2. Si el status es `"INACTIVE"`, retorna 403 "Account deactivated|{userId}"
3. Verifica password con `passwordEncoder.matches()` → si no coincide, 401 "Invalid credentials"
4. Genera JWT y retorna `UserResponse` con el token

#### Otros métodos

- **update**: permite cambiar email y/o contraseña. Si se cambia la contraseña, requiere la actual como `currentPassword` y la verifica antes de actualizar.
- **deactivate**: cambia status de `"ACTIVE"` a `"INACTIVE"`.
- **activate**: cambia status de `"INACTIVE"` a `"ACTIVE"`, requiere verificar contraseña.
- **validatePassword**: verifica que la contraseña proporcionada coincida con la almacenada.

### `JwtTokenProvider` (`security/JwtTokenProvider.java`)

Componente que maneja la creación y validación de tokens JWT usando la librería **jjwt**.

- **Configuración**: Lee `jwt.secret` (Base64) y `jwt.expiration-ms` del `application.properties`. Construye una `SecretKey` HMAC-SHA.
- **`generateToken(userId, email, role, playerId)`**: Crea un JWT con:
  - `sub`: userId (UUID)
  - `email`, `role`, `playerId` como claims personalizados
  - `iat`, `exp` (fecha actual + expirationMs)
  - Firmado con la clave HMAC
- **`generateToken(..., displayName)`**: Sobrecarga que además incluye `displayName` como claim.
- **`validateToken(token)`**: Parsea y verifica la firma. Retorna `true` si es válido, `false` si expiró o es inválido.
- **`getUserIdFromToken(token)`**: Extrae el `subject` como UUID.
- **`getUserDetailsFromToken(token)`**: Extrae los claims y construye un `JwtUserDetails(userId, role, playerId)`.

### `JwtUserDetails` (`security/JwtUserDetails.java`)

Record simple que contiene la información extraída del token: `userId` (UUID), `role` (String), `playerId` (UUID). Se usa para setear la autenticación en el SecurityContext.

### `JwtAuthenticationFilter` (`security/JwtAuthenticationFilter.java`)

Filtro que extiende `OncePerRequestFilter`. Se ejecuta antes de `UsernamePasswordAuthenticationFilter`.

1. Extrae el token del header `Authorization: Bearer <token>`
2. Si el token existe y es válido, obtiene `JwtUserDetails` del token
3. Construye `SimpleGrantedAuthority("ROLE_" + role)` con el rol del usuario
4. Crea `UsernamePasswordAuthenticationToken` y lo setea en `SecurityContextHolder`
5. Si no hay token o es inválido, continúa la cadena sin autenticar (Spring Security se encargará de denegar el acceso si el endpoint requiere auth)

**Excepción**: Las requests OPTIONS no pasan por este filtro (`shouldNotFilter`).

### `SecurityConfig` (`security/SecurityConfig.java`)

Configuración central de Spring Security:

- **CSRF**: deshabilitado (API REST stateless).
- **Session**: `SessionCreationPolicy.STATELESS` (no se crean sesiones HTTP).
- **CORS**: permite origen `http://localhost:4200`, métodos GET/POST/PUT/DELETE/OPTIONS, headers arbitrarios, con `allowCredentials(true)`.
- **Reglas de autorización**:
  | Ruta                          | Acceso            |
  |-------------------------------|-------------------|
  | `OPTIONS /**`                 | Permitido         |
  | `/api/users/register`         | Permitido         |
  | `/api/users/login`            | Permitido         |
  | `/swagger-ui/**`              | Permitido         |
  | `/v3/api-docs/**`             | Permitido         |
  | `/h2-console/**`              | Permitido         |
  | `/ws/**`                      | Permitido         |
  | `GET /api/cards/**`           | Permitido         |
  | `/uploads/**`                 | Permitido         |
  | Cualquier otra ruta           | Autenticado       |
- **Filter chain**: Agrega `JwtAuthenticationFilter` antes de `UsernamePasswordAuthenticationFilter`.
- **PasswordEncoder**: Bean `BCryptPasswordEncoder`.

### `WebConfig` (`configs/WebConfig.java`)

Configuración adicional de Spring Web MVC:

- **CORS**: Configuración redundante con `SecurityConfig` (también permite `http://localhost:4200` con los mismos métodos). La config de Spring Security tiene prioridad, pero ambas están presentes como respaldo.
- **Resource handlers**: Mapea `/uploads/**` al directorio configurado en `app.upload.dir` (para servir imágenes de avatares subidas).
- **@EnableScheduling**: Habilita tareas programadas (usado para otras partes del sistema).

### `GlobalExceptionHandler` (`advice/GlobalExceptionHandler.java`)

Manejador global de excepciones con `@RestControllerAdvice`. Toda excepción no capturada por los controllers pasa por aquí. Responde siempre con `ErrorApi` (objeto JSON con `timestamp`, `status`, `error`, `code`, `message`, `path`).

| Excepción                         | Status HTTP | Código               |
|-----------------------------------|-------------|----------------------|
| `ValidationException`             | 400         | código de la excepción |
| `NotFoundException`               | 404         | código de la excepción |
| `ConflictException`               | 409         | código de la excepción |
| `StorageException`                | 500         | `STORAGE_ERROR`      |
| `NoResourceFoundException`        | 404         | `NOT_FOUND`          |
| `MethodArgumentNotValidException` | 400         | `VALIDATION_ERROR`   |
| `IllegalArgumentException`        | 400         | `INVALID_ARGUMENT`   |
| `ResponseStatusException`         | según la excepción | `RESPONSE_STATUS` |
| `DataIntegrityViolationException` | 409         | `DUPLICATE_ENTRY`    |
| `Exception` (catch-all)           | 500         | `INTERNAL_ERROR`     |

Las excepciones de autenticación (`ResponseStatusException` con 401/403 lanzadas desde `UserService.login()`) son capturadas aquí y devueltas como JSON estructurado.
