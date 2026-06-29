# Chat Box

## Ubicación
`FE/src/app/features/match/components/chat-box/chat-box.component.ts`

## Modelo
`FE/src/app/shared/models/chat.models.ts`
```typescript
interface ChatMessage {
  senderId: string;
  senderName: string;
  content: string;
  timestamp: number;
}
```

## Propósito
Permite que los jugadores se comuniquen durante la partida. Los mensajes se envían vía WebSocket (STOMP) y se muestran en un panel arrastrable (`cdkDrag`). Cada jugador ve los mensajes propios alineados a la derecha (azul) y los del oponente a la izquierda (gris).

## Arquitectura

### WebSocket — Suscripción (`match-socket.service.ts`)
- El servicio `MatchSocketService` se suscribe a `/topic/matches/{matchId}/chat` cuando se conecta a una partida.
- Cuando llega un mensaje, lo parsea como `ChatMessage` y lo emite a través del Subject `_chatMessages` (expuesto como `chatMessages$`).
- Para **enviar**, el componente llama a `socket.sendChatMessage(msg)`, que publica en `/app/matches/{matchId}/chat` con el cuerpo JSON del mensaje.
- El mensaje se incluye tanto en el envío como en la lista local (optimistic update) para que el emisor lo vea de inmediato.

### Componente `ChatBoxComponent`

**Estado interno** (señales):
- `isOpen` — controla si el panel de chat está visible.
- `messages` — arreglo de `ChatMessage[]`.
- `newMessage` — texto del input.
- `unreadCount` — contador de no leídos (se muestra como badge rojo en el botón flotante).
- `showEmoticons` — toggle del selector de emoticonos.
- `profileUserId` — ID del jugador cuyo perfil se está viendo (modal de perfil).
- `profileCache` — caché de datos de perfil para evitar refetch.

**Flujo de mensajes entrantes**:
1. El constructor se suscribe a `socket.chatMessages$`.
2. Filtra duplicados (mismo senderId, contenido y timestamp < 2s del último).
3. Agrega el mensaje a la lista y hace scroll automático al final.
4. Si el chat no está abierto, incrementa `unreadCount`.
5. Si el remitente no es el jugador local y no tiene avatar cacheado, lo obtiene vía `PlayerApiService`.

**Flujo de envío**:
1. El usuario escribe en el input y presiona Enter o el botón "Enviar".
2. `send()` toma el texto, construye un `ChatMessage` con `senderId`, `senderName`, `content`, y `timestamp: Date.now()`.
3. Publica el mensaje vía `socket.sendChatMessage(msg)`.
4. Lo agrega localmente a la lista (optimistic update). No espera confirmación del backend.
5. Limpia el input y hace scroll al final.

**Historial**:
- Al montarse (cuando `matchId()` cambia), llama a `matchApi.getChatHistory(matchId)` para cargar mensajes previos.
- Los mensajes históricos se muestran igual que los entrantes en tiempo real.

**Interfaz de usuario**:
- Botón flotante en `fixed bottom-4 left-4` con badge de no leídos.
- Panel con encabezado arrastrable mediante `cdkDrag` y `cdkDragBoundary="app-match-page"`.
- Lista de mensajes con scroll automático.
- Input con botón de emoticonos (panel de 15 emoticones en grilla 5×3).
- Cada mensaje muestra: avatar (imagen o inicial), nombre (el remitente propio se ve como "Tú"), texto, y hora (`HH:mm`).
- Al hacer clic en el avatar o nombre de un jugador, se abre un **modal de perfil** con: avatar, nombre, victorias, derrotas, win rate y fecha de registro (`MM/yyyy`).

## Quién ve qué
- **Ambos jugadores** ven todos los mensajes del chat. No hay mensajes privados.
- El mensaje propio se distingue con fondo azul (`bg-blue-600`) y alineación derecha.
- El mensaje del oponente se muestra con fondo gris (`bg-slate-700`) y alineación izquierda.
- El nombre del remitente propio se traduce como "Tú".

## Dependencias
- `MatchSocketService` — conexión STOMP/WebSocket.
- `AuthService` — obtiene `playerId` y `displayName`.
- `AvatarService` — resuelve URLs de avatar.
- `MatchApiService.getChatHistory()` — carga historial REST.
- `PlayerApiService.getById()` — obtiene datos del jugador.
- `RankingApiService.getPlayerStats()` — estadísticas de ranking para el perfil.
