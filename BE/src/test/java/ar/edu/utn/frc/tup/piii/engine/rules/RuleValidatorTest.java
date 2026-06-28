package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.turn.states.TurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleValidatorTest {

    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EnergyService energyService;
    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;

    private RuleValidator validator;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator(cardLookup);
        playerId = UUID.randomUUID();
    }

    // --- First turn evolution ---

    @Test
    void shouldRejectEvolveOnPlayersFirstTurn() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.EVOLVE_POKEMON);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptEvolveAfterFirstTurnCompleted() {
        UUID handCardId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(UUID.randomUUID());
        target.setCardDefinitionId("pikachu");
        target.setEnteredTurnNumber(1);

        PokemonCardDefinition targetDef = new PokemonCardDefinition();
        targetDef.setName("Pikachu");
        targetDef.setStage("BASIC");

        PokemonCardDefinition evoDef = new PokemonCardDefinition();
        evoDef.setStage("STAGE_1");
        evoDef.setEvolvesFrom("Pikachu");

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(true);
        when(state.getTurnNumber()).thenReturn(3);

        GameAction action = new GameAction();
        action.setType(GameActionType.EVOLVE_POKEMON);
        action.setPlayerId(playerId);
        action.setPayload(Map.of(
                "handIndex", 0,
                "targetPokemonInstanceId", target.getInstanceId().toString()
        ));

        when(player.getHand()).thenReturn(List.of(
                new ar.edu.utn.frc.tup.piii.engine.model.CardInstance(handCardId, "raichu")
        ));
        when(player.getBench()).thenReturn(List.of(target));
        when(cardLookup.getCardById("raichu")).thenReturn(evoDef);
        when(cardLookup.getCardById("pikachu")).thenReturn(targetDef);

        assertTrue(validator.validate(ctx, action));
    }

    // --- First turn attack ---

    @Test
    void shouldRejectAttackOnPlayersFirstTurn() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canAttack()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.getFirstPlayerId()).thenReturn(playerId);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.DECLARE_ATTACK);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptAttackAfterFirstTurnCompleted() {
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("charizard");
        active.setSpecialConditions(new ArrayList<>());

        PokemonCardDefinition activeDef = new PokemonCardDefinition();
        var attack = new PokemonCardDefinition.AttackDefinition();
        attack.setIndex(0);
        attack.setCost(new ArrayList<>());
        attack.setDamage("50");
        activeDef.setAttacks(List.of(attack));

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canAttack()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.getFirstPlayerId()).thenReturn(UUID.randomUUID());
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(player.getActivePokemon()).thenReturn(active);
        when(cardLookup.getCardById("charizard")).thenReturn(activeDef);
        when(energyService.checkAttackRequirements(active, cardLookup, 0)).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.DECLARE_ATTACK);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("attackIndex", 0));

        assertTrue(validator.validate(ctx, action));
    }

    // --- KO Replacement validation ---

    @Test
    void shouldAcceptKOReplacementWhenValid() {
        UUID benchId = UUID.randomUUID();
        PokemonInPlay benched = new PokemonInPlay();
        benched.setInstanceId(benchId);

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(playerId);
        when(player.getBench()).thenReturn(List.of(benched));

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("benchPokemonInstanceId", benchId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementWhenNotPending() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementForWrongPlayer() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(UUID.randomUUID());

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementWhenBenchPokemonNotOnBench() {
        UUID benchId = UUID.randomUUID();
        UUID wrongId = UUID.randomUUID();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(playerId);
        when(player.getBench()).thenReturn(List.of(
                new PokemonInPlay() {{ setInstanceId(benchId); }}
        ));

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("benchPokemonInstanceId", wrongId.toString()));

        assertFalse(validator.validate(ctx, action));
    }
}