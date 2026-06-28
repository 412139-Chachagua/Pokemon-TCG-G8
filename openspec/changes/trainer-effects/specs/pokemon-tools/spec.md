## ADDED Requirements

### Requirement: Equipamiento de Herramientas Pokémon

Las cartas de Herramienta Pokémon (subtipo `TrainerSubtype.ITEM` con `effectCode` de tipo tool) SHALL poder equiparse a un Pokémon en juego mediante una acción `ATTACH_TOOL`.

- SHALL crear `GameActionType.ATTACH_TOOL`
- SHALL crear `AttachToolHandler` que maneje la acción
- SHALL validar que la carta en mano es una Herramienta Pokémon
- SHALL validar que el Pokémon objetivo está en juego (Activo o Banca)
- SHALL validar que el Pokémon objetivo no tiene ya una herramienta equipada (`PokemonInPlay.toolCardInstanceId == null`)
- SHALL asignar `toolCardInstanceId` con el `instanceId` de la carta de Herramienta
- SHALL mover la carta de la mano al campo del Pokémon (no al descarte)
- SHALL publicar evento `TOOL_ATTACHED`

#### Scenario: Equipar Herramienta a Pokémon sin herramienta
- **WHEN** un jugador ejecuta `ATTACH_TOOL` con una carta de Herramienta y un Pokémon sin herramienta
- **THEN** la Herramienta se asigna a `PokemonInPlay.toolCardInstanceId`
- **AND** se publica evento `TOOL_ATTACHED`

#### Scenario: Equipar Herramienta a Pokémon con herramienta existente
- **WHEN** un jugador ejecuta `ATTACH_TOOL` sobre un Pokémon que ya tiene una herramienta
- **THEN** la acción es rechazada con error `TOOL_ALREADY_EQUIPPED`

#### Scenario: Herramienta se descarta al quedar KO el Pokémon
- **WHEN** un Pokémon con herramienta equipada queda Fuera de Combate
- **THEN** la herramienta se descarta junto con el Pokémon y sus cartas unidas

### Requirement: Validación en RuleValidator para ATTACH_TOOL

`RuleValidator` SHALL validar las siguientes condiciones para `ATTACH_TOOL`:

- La fase debe ser `MAIN`
- La carta en la mano debe ser una Herramienta Pokémon
- El Pokémon objetivo debe estar en el campo (Activo o Banca) del jugador
- El Pokémon objetivo no debe tener herramienta equipada

#### Scenario: ATTACH_TOOL rechazado por fase incorrecta
- **WHEN** un jugador ejecuta `ATTACH_TOOL` durante la fase `DRAW` o `ATTACK`
- **THEN** la acción es rechazada con error `WRONG_PHASE`

#### Scenario: ATTACH_TOOL rechazado por herramienta existente
- **WHEN** un jugador ejecuta `ATTACH_TOOL` sobre un Pokémon con herramienta
- **THEN** la acción es rechazada con error `TOOL_ALREADY_EQUIPPED`
