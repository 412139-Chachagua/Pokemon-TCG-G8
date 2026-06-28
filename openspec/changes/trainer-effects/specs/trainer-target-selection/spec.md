## ADDED Requirements

### Requirement: Selección de targets para efectos de Entrenador

Los efectos que requieren selección de targets SHALL recibir la información necesaria a través del payload de `GameAction`.

- `targetPokemonInstanceId: UUID` SHALL identificar un Pokémon en juego (Activo o Banca, propio o del rival)
- `targetPlayerId: UUID` SHALL identificar un jugador
- `targetCardIndex: int` SHALL identificar un índice en lista (mano, mazo, descarte) del jugador
- `targetCardInstanceId: UUID` SHALL identificar una carta específica en juego

Los targets SHALL ser validados por el resolver antes de ejecutar el efecto.

#### Scenario: Target válido permite ejecutar efecto
- **WHEN** un efecto requiere `targetPokemonInstanceId` y el payload contiene un UUID válido de un Pokémon en juego
- **THEN** el resolver ejecuta el efecto sobre ese Pokémon

#### Scenario: Target inválido rechaza la acción
- **WHEN** un efecto requiere `targetPokemonInstanceId` pero el payload contiene un UUID que no corresponde a ningún Pokémon en juego
- **THEN** la acción es rechazada con error `INVALID_TARGET`

#### Scenario: Target faltante rechaza la acción
- **WHEN** un efecto requiere `targetPokemonInstanceId` pero el payload no incluye esa clave
- **THEN** la acción es rechazada con error `MISSING_TARGET`

### Requirement: Validación de targets en RuleValidator

`RuleValidator` SHALL validar que los targets requeridos por el `effectCode` estén presentes y sean válidos antes de delegar en el resolver.

- SHALL consultar `TrainerEffectRegistry` para obtener los targets requeridos por cada `EffectType`
- SHALL validar que cada target requerido esté presente en el payload
- SHALL validar que los targets referencien entidades existentes

#### Scenario: Target validado antes de ejecutar efecto
- **WHEN** un jugador juega una carta de Entrenador cuyo effectCode requiere `targetPokemonInstanceId`
- **THEN** `RuleValidator` verifica que `targetPokemonInstanceId` está presente en el payload
- **AND** verifica que el UUID corresponde a un Pokémon en juego
- **AND** si alguna validación falla, rechaza la acción antes de ejecutar el efecto
