package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeclareAttackHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private PlayerState opponent;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private TurnManager turnManager;
    @Mock
    private EnergyService energyService;
    @Mock
    private RandomizerPort randomizer;

    private DeclareAttackHandler handler;
    private UUID playerId;
    private UUID opponentId;
    private UUID attackerInstanceId;
    private UUID defenderInstanceId;
    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private PokemonCardDefinition attackerDef;
    private PokemonCardDefinition.AttackDefinition attackDef;
    private PokemonCardDefinition defenderDef;
    private TurnFlags flags;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        handler = new DeclareAttackHandler(turnManager);
        playerId = UUID.randomUUID();
        opponentId = UUID.randomUUID();
        attackerInstanceId = UUID.randomUUID();
        defenderInstanceId = UUID.randomUUID();

        attacker = new PokemonInPlay();
        attacker.setInstanceId(attackerInstanceId);
        attacker.setCardDefinitionId("pkm-pikachu");
        attacker.setAttachedEnergies(new ArrayList<>());
        attacker.setDamageCounters(0);

        defender = new PokemonInPlay();
        defender.setInstanceId(defenderInstanceId);
        defender.setCardDefinitionId("pkm-charmander");
        defender.setAttachedEnergies(new ArrayList<>());
        defender.setDamageCounters(0);
        defender.setSpecialConditions(new ArrayList<>());

        attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setName("Thunderbolt");
        attackDef.setDamage("50");
        attackDef.setCost(List.of(EnergyType.LIGHTNING));

        attackerDef = new PokemonCardDefinition();
        attackerDef.setHp(60);
        attackerDef.setAttacks(List.of(attackDef));
        attackerDef.setTypes(new ArrayList<>());

        defenderDef = mock(PokemonCardDefinition.class);

        flags = new TurnFlags();
    }

    private GameAction createAttackAction(Integer attackIndex, String targetIdStr) {
        GameAction action = new GameAction();
        action.setType(ar.edu.utn.frc.tup.piii.engine.action.GameActionType.DECLARE_ATTACK);
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (attackIndex != null) payload.put("attackIndex", attackIndex);
        if (targetIdStr != null) payload.put("targetPokemonInstanceId", targetIdStr);
        action.setPayload(payload);
        return action;
    }

    private void setupBasicAttackMocks() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getActivePokemon()).thenReturn(attacker);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(energyService.checkAttackRequirements(any(), any(), anyInt())).thenReturn(true);
        when(opponent.getActivePokemon()).thenReturn(defender);
        // lenient stubs — usados por algunos tests que completan la cadena, no por todos
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(3);
        lenient().when(player.getBench()).thenReturn(new ArrayList<>());
        lenient().when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        lenient().when(energyService.calculateDamageBonus(any(), any(), any(), anyInt())).thenReturn(0);
        lenient().when(ctx.getRandomizer()).thenReturn(randomizer);
        lenient().when(opponent.getBench()).thenReturn(new ArrayList<>());
        lenient().when(state.getStatus()).thenReturn(null);
    }

    @Test
    void shouldDeclareAttackAndExecuteChain() {
        setupBasicAttackMocks();

        GameAction action = createAttackAction(0, defenderInstanceId.toString());

        handler.handle(ctx, action);

        assertTrue(flags.hasAttacked());
        verify(turnManager).endTurn(ctx);
        verify(turnManager).startTurn(ctx);
    }

    @Test
    void shouldRejectWhenAlreadyAttackedThisTurn() {
        flags.setHasAttacked(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        verify(turnManager, never()).endTurn(any());
    }

    @Test
    void shouldRejectWhenAttackIndexIsNull() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);

        handler.handle(ctx, createAttackAction(null, defenderInstanceId.toString()));

        verify(turnManager, never()).endTurn(any());
    }

    @Test
    void shouldRejectWhenTargetIdIsNull() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);

        handler.handle(ctx, createAttackAction(0, null));

        verify(turnManager, never()).endTurn(any());
    }

    @Test
    void shouldRejectWhenAttackerIsNull() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(player.getActivePokemon()).thenReturn(null);

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        verify(turnManager, never()).endTurn(any());
    }

    @Test
    void shouldRejectWhenDefenderIsNull() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(player.getActivePokemon()).thenReturn(attacker);
        when(opponent.getActivePokemon()).thenReturn(null);
        when(opponent.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        verify(turnManager, never()).endTurn(any());
    }

    @Test
    void shouldHandleConfusionSelfHit() {
        attacker.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));
        setupBasicAttackMocks();
        // randomizer.nextInt(2) returns 0 (default mock) → 0 == 0 is true → self-hit triggers
        when(state.isPendingKOReplacement()).thenReturn(false);

        GameAction action = createAttackAction(0, defenderInstanceId.toString());
        handler.handle(ctx, action);

        assertTrue(flags.hasAttacked());
        verify(turnManager).endTurn(ctx);
        verify(turnManager).startTurn(ctx);
    }

    @Test
    void shouldHandleKOReplacementDuringAttack() {
        setupBasicAttackMocks();
        when(state.isPendingKOReplacement()).thenReturn(true);

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        verify(turnManager).endTurn(ctx);
        verify(turnManager).startTurn(ctx);
        assertTrue(flags.hasAttacked());
    }

    @Test
    void shouldNotEndTurnWhenMatchFinished() {
        setupBasicAttackMocks();
        when(state.getStatus()).thenReturn(MatchStatus.FINISHED);

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        verify(turnManager, never()).endTurn(any());
        assertTrue(flags.hasAttacked());
    }

    @Test
    void shouldHandleDamageMultiplierFromBenchCount() {
        attackDef.setDamage("10x");
        attackDef.setText("Does 10 damage times the number of your Benched Pokémon.");

        setupBasicAttackMocks();
        PokemonInPlay benchPkm = new PokemonInPlay();
        benchPkm.setInstanceId(UUID.randomUUID());
        benchPkm.setCardDefinitionId("pkm-bench");


        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        assertTrue(flags.hasAttacked());
    }

    @Test
    void shouldHandleDamageMultiplierFromEnergyCount() {
        attackDef.setDamage("20x");
        attackDef.setText("Does 20 damage times the amount of Energy attached to this Pokémon.");

        setupBasicAttackMocks();
        attacker.setAttachedEnergies(List.of(
                new CardInstance(UUID.randomUUID(), "energy-lightning"),
                new CardInstance(UUID.randomUUID(), "energy-lightning")
        ));

        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        assertTrue(flags.hasAttacked());
    }

    @Test
    void shouldHandleCoinFlipUntilTailsMultiplier() {
        attackDef.setDamage("30x");
        attackDef.setText("Flip a coin until you get tails. This attack does 30 damage times the number of heads.");

        setupBasicAttackMocks();


        handler.handle(ctx, createAttackAction(0, defenderInstanceId.toString()));

        assertTrue(flags.hasAttacked());
    }
}
