# Chat en Partidas — WebSocket

## Resumen

El sistema de chat permite a los jugadores de una partida enviarse mensajes de texto en tiempo real a través de WebSocket usando el protocolo STOMP. Los mensajes se enriquecen con un timestamp del servidor, se cachean en memoria y se retransmiten a todos los suscriptores del tópico de chat de esa partida.

## Conexión WebSocket

El endpoint STOMP se registra en `WebSocketConfig.java:29`:

```
/ws  (con soporte SockJS)
```

Los clientes se conectan usando `Authorization: Bearer <jwt>` en los headers STOMP `CONNECT`. El `StompInboundInterceptor` valida el token JWT y almacena los datos del usuario (`JwtUserDetails`) en los atributos de sesión. Sin un JWT válido no se puede suscribir a tópicos privados, pero los tópicos públicos de chat son accesibles.

**Configuración de transporte** (`WebSocketConfig.java:35-38`):
- Límite de tamaño de mensaje: 4 MB
- Límite de buffer de envío: 4 MB
- Prefijo de destino de aplicación: `/app`
- Brokers simples: `/topic`, `/queue`

## Modelo de mensaje (`ChatMessage.java`)

```java
public record ChatMessage(
    String senderId,    // ID del jugador que envía
    String senderName,  // nombre para mostrar
    String content,     // contenido del mensaje
    long timestamp      // epoch millis (asignado por el servidor)
) {}
```

## Flujo de envío y recepción

### 1. El cliente envía un mensaje

El cliente publica en:
```
/app/matches/{matchId}/chat
```

Esto es manejado por `MatchWebSocketController.handleChatMessage()` (línea 67).

### 2. El servidor procesa el mensaje

En `handleChatMessage()`:

1. Se recibe el `ChatMessage` del cliente (sin timestamp confiable).
2. Se crea un nuevo `ChatMessage` enriquecido con `Instant.now().toEpochMilli()` como timestamp del servidor.
3. Se agrega al caché con `chatCache.addMessage(matchUuid, enriched)`.
4. Se retransmite a todos los suscriptores en `/topic/matches/{matchId}/chat`.

```java
ChatMessage enriched = new ChatMessage(
    message.senderId(),
    message.senderName(),
    message.content(),
    Instant.now().toEpochMilli()
);
chatCache.addMessage(matchUuid, enriched);
messagingTemplate.convertAndSend("/topic/matches/" + matchId + "/chat", enriched);
```

### 3. Los clientes reciben el mensaje

Todos los clientes suscritos a `/topic/matches/{matchId}/chat` reciben el mensaje enriquecido. La suscripción es pública — cualquier cliente autenticado puede suscribirse al tópico de chat de una partida.

## Caché de mensajes (`ChatMessageCacheService.java`)

Es un servicio Spring con un `ConcurrentHashMap<UUID, List<ChatMessage>>` en memoria:

| Método | Comportamiento |
|--------|---------------|
| `addMessage(matchId, message)` | Agrega el mensaje a la lista del match, creándola si no existe |
| `getMessages(matchId)` | Retorna la lista de mensajes del match, o lista vacía |
| `clearMatch(matchId)` | Elimina la entrada del match |

**Propósito**: El caché permite que un jugador que se reconecta pueda recuperar los mensajes recientes (aunque no hay un endpoint REST expuesto para esto actualmente). Los mensajes **no persisten en base de datos** — se pierden al reiniciar el servidor.

## Seguridad y suscripciones

El `StompInboundInterceptor` (líneas 51-63) intercepta comandos `SUBSCRIBE` y verifica que un jugador solo pueda suscribirse a su propio tópico privado (`/topic/matches/{matchId}/player/{playerId}`). El tópico de chat (`/topic/matches/{matchId}/chat`) **no está protegido** por esta verificación, por lo que cualquier cliente autenticado conectado a la partida puede escuchar los mensajes.

## Anti-spam / Rate limiting

**Actualmente no hay implementado ningún mecanismo de rate limiting o anti-spam.** No hay:
- Límite de mensajes por segundo
- Filtro de contenido
- Bloqueo por spam
- Tamaño máximo de mensaje a nivel de aplicación (solo el límite de 4 MB del transporte WebSocket)

## Eventos de conexión/desconexión

`MatchWebSocketEventListener` rastrea sesiones activas. Cuando un jugador se suscribe a su tópico privado, se publica un evento `PLAYER_RECONNECTED` y al desconectarse un `PLAYER_DISCONNECTED` en el tópico de eventos de la partida.
