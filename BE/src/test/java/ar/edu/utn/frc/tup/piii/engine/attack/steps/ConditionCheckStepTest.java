package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConditionCheckStepTest {

    @Mock
    private EngineContext ctx;

    @Mock
    private GameState state;

    @Mock
    private CardLookupPort cardLookup;

    @Mock
    private RandomizerPort randomizer;

    private PokemonInPlay attacker;
    private PokemonInPlay defender;
    private PokemonCardDefinition attackerDef;
    private PokemonCardDefinition.AttackDefinition attackDef;
    private AttackContext attackCtx;
    private ConditionCheckStep step;

    @BeforeEach
    void setUp() {
        step = new ConditionCheckStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("pkm-pikachu");
        attacker.setSpecialConditions(new ArrayList<>());
        defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());

        attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setName("Thunderbolt");
        attackDef.setDamage("50");

        attackerDef = new PokemonCardDefinition();
        attackerDef.setHp(60);
        attackerDef.setAttacks(List.of(attackDef));
    }

    @Test
    void shouldProceedWhenNoConditionsBlock() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldStopChainWhenCannotAttackAndMatchRestrictedAttack() {
        attacker.setCannotAttackNextTurn(true);
        attacker.setRestrictedAttackName("Thunderbolt");
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        assertFalse(attacker.isCannotAttackNextTurn());
        assertNull(attacker.getRestrictedAttackName());
        var capturedEvent = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(capturedEvent.capture());
        assertEquals("ATTACK_CANCELED", capturedEvent.getValue().getType());
    }

    @Test
    void shouldProceedWhenCannotAttackButDifferentRestrictedAttack() {
        attacker.setCannotAttackNextTurn(true);
        attacker.setRestrictedAttackName("Quick Attack");
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attacker.isCannotAttackNextTurn());
    }

    @Test
    void shouldStopChainWhenCannotAttackAndRestrictedIsNull() {
        attacker.setCannotAttackNextTurn(true);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        lenient().when(ctx.getCardLookup()).thenReturn(cardLookup);
        lenient().when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        assertFalse(attacker.isCannotAttackNextTurn());
    }

    @Test
    void shouldStopChainWhenCannotAttackAndAttacksListIsNull() {
        // When attacks list is null, the code can't verify the attack name,
        // so blocked stays true (conservative: block when in doubt).
        attacker.setCannotAttackNextTurn(true);
        attacker.setRestrictedAttackName("Thunderbolt");
        attackerDef.setAttacks(null);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        assertFalse(attacker.isCannotAttackNextTurn());
    }

    @Test
    void shouldStopChainWhenAsleep() {
        attacker.getSpecialConditions().add(SpecialCondition.ASLEEP);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        var capturedEvent = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(capturedEvent.capture());
        Map<String, Object> payload = capturedEvent.getValue().getPayload();
        assertEquals("asleep", payload.get("reason"));
    }

    @Test
    void shouldStopChainWhenParalyzed() {
        attacker.getSpecialConditions().add(SpecialCondition.PARALYZED);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        var capturedEvent = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(capturedEvent.capture());
        Map<String, Object> payload = capturedEvent.getValue().getPayload();
        assertEquals("paralyzed", payload.get("reason"));
    }

    @Test
    void shouldProceedWhenMustFlipToAttackAndHeads() {
        attacker.setMustFlipToAttackNextTurn(true);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0); // heads
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attacker.isMustFlipToAttackNextTurn());
    }

    @Test
    void shouldStopChainWhenMustFlipToAttackAndTails() {
        attacker.setMustFlipToAttackNextTurn(true);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1); // tails
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.STOP_CHAIN, result);
        assertFalse(attacker.isMustFlipToAttackNextTurn());
        verify(ctx, atLeast(2)).addEvent(any(GameEvent.class));
    }
}
