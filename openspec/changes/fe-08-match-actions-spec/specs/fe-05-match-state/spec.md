## MODIFIED Requirements

### Requirement: GameActionDispatcherService.putBasicOnBench()

`GameActionDispatcherService.putBasicOnBench()` SHALL accept `benchIndex: number` as a 4th parameter and include it in the action payload.

**Current signature:**
```typescript
putBasicOnBench(matchId: string, playerId: string, handIndex: number): void
```

**Modified signature:**
```typescript
putBasicOnBench(matchId: string, playerId: string, handIndex: number, benchIndex: number): void
```

**Current behavior:**
- Dispatches `PUT_BASIC_ON_BENCH` with payload `{ handIndex }`

**Modified behavior:**
- Dispatches `PUT_BASIC_ON_BENCH` with payload `{ handIndex, benchIndex }`

**Contract references:** `08-game-action-contract.md` (PUT_BASIC_ON_BENCH action format: payload includes both `handIndex` and `benchIndex`)

#### Scenario: putBasicOnBench sends benchIndex in payload
- WHEN `putBasicOnBench(matchId, playerId, 2, 3)` is called
- THEN `dispatchAction` SHALL be called with type `PUT_BASIC_ON_BENCH` and payload `{ handIndex: 2, benchIndex: 3 }`
