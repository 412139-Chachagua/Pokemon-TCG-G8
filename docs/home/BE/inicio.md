# Pantalla de Inicio (Backend)

## Endpoints de Health Check

### `GET /ping`

- **Controlador:** `PingController.java`
- **Propósito:** Health check básico para verificar que la aplicación Spring Boot está viva.
- **Respuesta:** Devuelve el string `"pong"`.
- **Códigos de respuesta:**
  - `200 OK` — la aplicación responde correctamente.
  - `500 Internal Server Error` — error interno del servidor.
- Documentado con OpenAPI mediante las anotaciones `@Operation` y `@ApiResponses`.

Este endpoint no requiere autenticación. Se usa desde el frontend o herramientas de monitoreo para confirmar que el servidor está operativo antes de cualquier interacción.

---

## Documentación de API con SpringDoc / Swagger

**Clase de configuración:** `SpringDocConfig.java`

### Configuración de OpenAPI

La clase `SpringDocConfig` (anotada con `@Configuration`) define un bean `OpenAPI` que personaliza la interfaz de Swagger UI con:

- **Título:** Se inyecta desde `app.name` en `application.properties`.
- **Versión:** Se inyecta desde `app.version`.
- **Descripción:** Se inyecta desde `app.desc`.
- **Contacto:** Nombre del desarrollador (`app.dev-name`) y email (`app.dev-email`).
- **Servidor:** URL base del servidor (`app.url`), que por defecto apunta a `http://localhost:8080`.

Además registra un bean `ModelResolver` para que Swagger pueda serializar correctamente los modelos de Jackson.

### Valores de configuración (`application.properties`)

```properties
app.name=@project.name@
app.desc=@project.description@
app.version=@project.version@
app.url=http://localhost:8080
app.dev-name=John Doe
app.dev-email=dumy@dumy
```

Los valores `@project.name@`, `@project.description@` y `@project.version@` se resuelven desde el `pom.xml` de Maven en tiempo de compilación.

### Acceso a Swagger UI

Una vez que la aplicación está corriendo, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

También se puede obtener el spec OpenAPI en JSON desde:

```
http://localhost:8080/v3/api-docs
```

---

## Seed Data / Configuración Inicial

**Clase:** `SeederTestConfig.java`

Este componente se ejecuta automáticamente al iniciar la aplicación cuando el evento `ApplicationReadyEvent` es disparado. Su propósito es poblar la base de datos con datos de prueba para desarrollo y testing.

### Flujo de ejecución

1. **Sincronizar catálogo de cartas:** Si la tabla de cartas está vacía, invoca `CardCacheSyncService.syncAll()` para poblar el catálogo desde la API externa de Pokémon TCG.
2. **Crear usuarios semilla:** Para cada usuario definido, verifica si ya existe por email. Si no existe, lo crea junto con su perfil de jugador (`PlayerEntity`).
3. **Crear mazos semilla:** Si el jugador no tiene mazos y el catálogo de cartas está disponible, crea un mazo con cartas predefinidas.

### Usuarios semilla creados

| Email              | Contraseña   | Nombre        | Mazo                |
|--------------------|-------------|---------------|---------------------|
| ash@pokemon.com    | password123 | Ash Ketchum   | Mazo de tipo Fuego  |
| misty@pokemon.com  | password456 | Misty         | Mazo de tipo Agua   |

### Cartas de mazo de Fuego (Ash)

Incluye cartas de la serie XY como `xy1-1` hasta `xy1-14`, con cantidades que varían entre 2 y 18 (las 18 son cartas de Energía, `xy1-133`).

### Cartas de mazo de Agua (Misty)

Incluye cartas de la serie XY como `xy1-15` hasta `xy1-28`, también con 18 cartas de Energía (`xy1-134`).

### Notas importantes

- Si el catálogo de cartas no puede sincronizarse (por ejemplo, la API externa no responde), los mazos no se crean y se registrará una advertencia en el log.
- La anotación `@Order(1)` asegura que este seeder se ejecute antes que otros inicializadores.
- La anotación `@Transactional` garantiza que toda la operación se ejecute en una sola transacción de base de datos.
