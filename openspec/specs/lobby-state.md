# Listar partidas disponibles (WAITING)

Endpoint para obtener partidas en estado `WAITING` (esperando segundo jugador) y conectar el `MatchListComponent` del frontend.

---

## Backend

### MatchJpaRepository

Agregar método de query:

```java
List<MatchEntity> findByStatus(String status);
```

### MatchApplicationService

Agregar método:

```java
public List<MatchResponse> listAvailableMatches() {
    List<MatchEntity> matches = matchJpaRepository.findByStatus("WAITING");
    return matches.stream()
            .map(m -> matchMapper.toMatchResponse(m, m.getPlayers()))
            .toList();
}
```

### MatchController

Agregar endpoint:

```
GET /api/matches
```

| Parámetro | Tipo   | Default | Descripción                     |
|-----------|--------|---------|---------------------------------|
| status    | String | WAITING | Filtro por estado de la partida |

```java
@GetMapping
public ResponseEntity<List<MatchResponse>> listMatches(
        @RequestParam(required = false, defaultValue = "WAITING") String status) {
    List<MatchResponse> matches = matchApplicationService.listAvailableMatches();
    return ResponseEntity.ok(matches);
}
```

**Respuesta (200 OK):**
```json
[
  {
    "id": "uuid-del-match",
    "status": "WAITING",
    "currentPhase": null,
    "turnNumber": 0,
    "currentPlayerId": null,
    "firstPlayerId": null,
    "winnerPlayerId": null,
    "finishReason": null,
    "players": [
      { "playerId": "uuid-jugador", "side": "PLAYER_ONE", "displayName": "Franco" }
    ],
    "createdAt": "2026-06-08T00:00:00Z"
  }
]
```

---

## Frontend

### MatchApiService

Agregar método:

```typescript
listMatches(status?: string): Observable<MatchResponse[]> {
    const params = new URLSearchParams();
    if (status) params.set('status', status);
    return this.apiClient.get<MatchResponse[]>(`/matches?${params.toString()}`);
}
```

### MatchListComponent

Reemplazar la señal estática y el stub de `onRefresh()` por una llamada real a la API.

```typescript
export class MatchListComponent implements OnInit {
  private readonly matchApi = inject(MatchApiService);

  readonly matchSelected = output<string>();
  readonly matches = signal<{ id: string; status: string }[]>([]);

  ngOnInit(): void {
    this.loadMatches();
  }

  onRefresh(): void {
    this.loadMatches();
  }

  private loadMatches(): void {
    this.matchApi.listMatches().subscribe({
      next: (response) => {
        this.matches.set(
          response.map((m) => ({ id: m.id, status: m.status }))
        );
      },
      error: () => {
        this.matches.set([]);
      },
    });
  }
}
```

- `ngOnInit()` SHALL cargar las partidas al montar el componente
- `onRefresh()` SHALL recargar la lista
- En caso de error, `matches` SHALL quedar como array vacío

---

## Tasks

1. **BE:** Agregar `findByStatus` en `MatchJpaRepository`
2. **BE:** Agregar `listAvailableMatches()` en `MatchApplicationService`
3. **BE:** Agregar `GET /api/matches` en `MatchController`
4. **FE:** Agregar `listMatches()` en `MatchApiService`
5. **FE:** Conectar `MatchListComponent` con el API (reemplazar stub)
