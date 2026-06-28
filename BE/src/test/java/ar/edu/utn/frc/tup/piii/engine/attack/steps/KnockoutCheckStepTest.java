package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.DestinyBurstHook;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.victory.VictoryConditionChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KnockoutCheckStepTest {

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
    private EnergyService energyService;

    @Mock
    private PokemonCardDefinition defenderDef;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private AttackContext attackCtx;
    private KnockoutCheckStep step;
    private UUID playerId;
    private UUID opponentId;
    private UUID defenderInstanceId;

    @BeforeEach
    void setUp() {
        step = new KnockoutCheckStep();
        playerId = UUID.randomUUID();
        opponentId = UUID.randomUUID();
        defenderInstanceId = UUID.randomUUID();

        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setOwnerPlayerId(opponentId);

        defender = new PokemonInPlay();
        defender.setInstanceId(defenderInstanceId);
        defender.setCardDefinitionId("pkm-charmander");
        defender.setOwnerPlayerId(playerId);
        defender.setDamageCounters(0);

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
    }

    @Test
    void shouldProceedWhenOpponentIsNull() {
        when(ctx.getState()).thenReturn(state);
        lenient().when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldProceedWhenDefenderDefIsNotPokemon() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(opponent.getPlayerId()).thenReturn(opponentId);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(mock(CardDefinition.class));

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldProceedWhenNotKO() {
        defender.setDamageCounters(3); // 30 damage < 60 HP
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(opponent.getPlayerId()).thenReturn(opponentId);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isKnockoutOccurred());
    }

    @Test
    void shouldHandleKOActivePokemonWithBench() {
        defender.setDamageCounters(6); // 60 >= 60 HP → KO
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(opponent.getPlayerId()).thenReturn(opponentId);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);
        when(defenderDef.isEx()).thenReturn(false);
        when(ctx.getEnergyService()).thenReturn(energyService);

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(defenderInstanceId);
        when(player.getActivePokemon()).thenReturn(active);

        PokemonInPlay benchPkm = new PokemonInPlay();
        benchPkm.setInstanceId(UUID.randomUUID());
        List<PokemonInPlay> bench = new ArrayList<>(List.of(benchPkm));
        when(player.getBench()).thenReturn(bench);

        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);
        lenient().when(player.getPlayerId()).thenReturn(playerId);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertTrue(attackCtx.isKnockoutOccurred());
        verify(state).setPendingKOReplacement(true);
        verify(state).setKnockedOutPlayerId(playerId);
    }

    @Test
    void shouldHandleKOActivePokemonWithoutBenchAndFinishMatch() {
        defender.setDamageCounters(6); // 60 >= 60 HP → KO
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);
        when(defenderDef.isEx()).thenReturn(false);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(opponent.getPlayerId()).thenReturn(opponentId);
        when(player.getBench()).thenReturn(new ArrayList<>());

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(defenderInstanceId);
        when(player.getActivePokemon()).thenReturn(active);

        VictoryConditionChecker.VictoryCheckResult victoryResult =
                new VictoryConditionChecker.VictoryCheckResult(true, opponentId, null, false);
        try (MockedStatic<VictoryConditionChecker> mockedVictory = mockStatic(VictoryConditionChecker.class)) {
            mockedVictory.when(() -> VictoryConditionChecker.check(state, opponentId))
                    .thenReturn(victoryResult);
            lenient().when(player.getPlayerId()).thenReturn(playerId);
            var result = step.execute(ctx, attackCtx);

            assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
            assertTrue(attackCtx.isKnockoutOccurred());
            verify(state).setWinnerPlayerId(opponentId);
            verify(state).setStatus(MatchStatus.FINISHED);
        }
    }

    @Test
    void shouldHandleKOBenchPokemon() {
        defender.setDamageCounters(6); // KO
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);
        when(defenderDef.isEx()).thenReturn(false);
        when(ctx.getEnergyService()).thenReturn(energyService);
        lenient().when(player.getPlayerId()).thenReturn(playerId);
        lenient().when(opponent.getPlayerId()).thenReturn(opponentId);

        // Defender is NOT the active
        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(UUID.randomUUID()); // different instance
        when(player.getActivePokemon()).thenReturn(active);

        PokemonInPlay benchPkm1 = new PokemonInPlay();
        benchPkm1.setInstanceId(UUID.randomUUID());
        PokemonInPlay benchPkm2 = new PokemonInPlay();
        benchPkm2.setInstanceId(defenderInstanceId);
        List<PokemonInPlay> bench = new ArrayList<>(List.of(benchPkm1, benchPkm2));
        when(player.getBench()).thenReturn(bench);

        try (MockedStatic<DestinyBurstHook> mockedDestiny = mockStatic(DestinyBurstHook.class)) {
            mockedDestiny.when(() -> DestinyBurstHook.onKnockout(any(), any(), any()))
                    .then(invocation -> null);

            var result = step.execute(ctx, attackCtx);

            assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
            assertTrue(attackCtx.isKnockoutOccurred());
            // Bench should no longer contain defender
            assertFalse(bench.contains(defender));
        }
    }

    @Test
    void shouldHandleKOPrizeCountForExPokemon() {
        defender.setDamageCounters(6); // KO
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);
        when(defenderDef.isEx()).thenReturn(true);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(opponent.getPlayerId()).thenReturn(opponentId);

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(defenderInstanceId);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getBench()).thenReturn(new ArrayList<>());

        VictoryConditionChecker.VictoryCheckResult victoryResult =
                new VictoryConditionChecker.VictoryCheckResult(true, opponentId, null, false);
        try (MockedStatic<VictoryConditionChecker> mockedVictory = mockStatic(VictoryConditionChecker.class)) {
            mockedVictory.when(() -> VictoryConditionChecker.check(state, opponentId))
                    .thenReturn(victoryResult);
            lenient().when(player.getPlayerId()).thenReturn(playerId);
            step.execute(ctx, attackCtx);

            verify(state).setPendingPrizeCount(2);
        }
    }

    @Test
    void shouldHandleKOWithToolCard() {
        defender.setDamageCounters(6); // KO
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getOpponent(playerId)).thenReturn(opponent);
        when(ctx.getPlayer(opponentId)).thenReturn(opponent);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-charmander")).thenReturn(defenderDef);
        when(defenderDef.getHp()).thenReturn(60);
        when(defenderDef.isEx()).thenReturn(false);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(opponent.getPlayerId()).thenReturn(opponentId);

        CardInstance tool = new CardInstance(UUID.randomUUID(), "tool-eviolite");
        defender.setAttachedTool(tool);
        defender.setToolCardInstanceId(tool.getInstanceId());

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(defenderInstanceId);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getBench()).thenReturn(new ArrayList<>());

        VictoryConditionChecker.VictoryCheckResult victoryResult =
                new VictoryConditionChecker.VictoryCheckResult(true, opponentId, null, false);
        try (MockedStatic<VictoryConditionChecker> mockedVictory = mockStatic(VictoryConditionChecker.class)) {
            mockedVictory.when(() -> VictoryConditionChecker.check(state, opponentId))
                    .thenReturn(victoryResult);
            lenient().when(player.getPlayerId()).thenReturn(playerId);
            step.execute(ctx, attackCtx);

            assertNull(defender.getAttachedTool());
            assertNull(defender.getToolCardInstanceId());
        }
    }
}
