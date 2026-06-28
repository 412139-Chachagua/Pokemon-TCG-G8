## ADDED Requirements

### Requirement: Zona de Estadio compartida

Las cartas de Estadio (`TrainerSubtype.STADIUM`) SHALL colocarse en `GameState.stadiumCardInstanceId` y permanecer en juego hasta ser reemplazadas o descartadas por un efecto.

- `PlayTrainerHandler` SHALL detectar `trainerSubtype == STADIUM` y delegar en `StadiumPlayResolver`
- `StadiumPlayResolver` SHALL asignar `cardInstanceId` a `GameState.stadiumCardInstanceId`
- Si ya existe un Estadio en juego, SHALL mover el Estadio anterior al descarte de su dueño
- SHALL publicar evento `STADIUM_PLAYED` con el nuevo Estadio
- SHALL publicar evento `STADIUM_REMOVED` si se reemplazó un Estadio anterior

#### Scenario: Jugar Estadio sin Estadio previo
- **WHEN** un jugador juega un Estadio y no hay ningún Estadio en juego
- **THEN** la carta de Estadio se coloca en `GameState.stadiumCardInstanceId`
- **AND** no se descarta ningún Estadio previo
- **AND** se publica evento `STADIUM_PLAYED`

#### Scenario: Jugar Estadio reemplaza al anterior
- **WHEN** un jugador juega un Estadio y ya existe un Estadio en juego
- **THEN** el Estadio anterior se mueve al descarte de su dueño
- **AND** el nuevo Estadio se asigna a `GameState.stadiumCardInstanceId`
- **AND** se publica evento `STADIUM_REMOVED`
- **AND** se publica evento `STADIUM_PLAYED`

#### Scenario: Estadio no se descarta al terminar el turno
- **WHEN** un Estadio está en juego al finalizar un turno
- **THEN** el Estadio permanece en `GameState.stadiumCardInstanceId`
- **AND** no se mueve al descarte
