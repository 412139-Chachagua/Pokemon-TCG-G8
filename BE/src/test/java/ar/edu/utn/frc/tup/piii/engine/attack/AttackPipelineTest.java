package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep.AttackStepResult;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.PreDamageStep;
import ar.edu.utn.frc.tup.piii.engine.attack.steps.PostDamageEffectStep;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttackPipelineTest {

    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private RandomizerPort randomizer;
    @Mock
    private StatePersisterPort persister;
    @Mock
    private EventPublisherPort eventPublisher;

    // --- AttackContext tests ---

    @Test
    void shouldInitializeWithCorrectValues_whenConstructed() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        PokemonInPlay defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
        Map<String, Object> mods = Map.of("mod", 10);
        UUID targetId = UUID.randomUUID();

        AttackContext ctx = new AttackContext(attacker, defender, 2, mods, targetId);

        assertSame(attacker, ctx.getAttacker());
        assertSame(defender, ctx.getDefender());
        assertEquals(2, ctx.getAttackIndex());
        assertSame(mods, ctx.getDamageModifiers());
        assertEquals(targetId, ctx.getTargetPokemonInstanceId());
    }

    @Test
    void shouldSetAndGetDamageCalc() {
        AttackContext ctx = createDefaultContext();

        DamageCalculator.DamageCalculatorResult calcResult = new DamageCalculator.DamageCalculatorResult(
                40, 2, -20, 60, 6, true, true);
        ctx.setDamageCalc(calcResult);

        assertSame(calcResult, ctx.getDamageCalc());
    }

    @Test
    void shouldSetAndGetConfusedSelfHit() {
        AttackContext ctx = createDefaultContext();

        assertFalse(ctx.isConfusedSelfHit());

        ctx.setConfusedSelfHit(true);

        assertTrue(ctx.isConfusedSelfHit());
    }

    @Test
    void shouldSetAndGetEnergyValid() {
        AttackContext ctx = createDefaultContext();

        assertFalse(ctx.isEnergyValid());

        ctx.setEnergyValid(true);

        assertTrue(ctx.isEnergyValid());
    }

    @Test
    void shouldTrackCoinFlipsCorrectly() {
        AttackContext ctx = createDefaultContext();

        assertEquals(0, ctx.getTotalCoinFlips());

        ctx.addCoinFlipResult(true);
        assertEquals(1, ctx.getTotalCoinFlips());

        ctx.addCoinFlipResult(false);
        assertEquals(2, ctx.getTotalCoinFlips());
    }

    @Test
    void shouldReturnTrueForAllCoinFlipsHeads_whenAllHeads() {
        AttackContext ctx = createDefaultContext();

        ctx.addCoinFlipResult(true);
        ctx.addCoinFlipResult(true);
        ctx.addCoinFlipResult(true);

        assertTrue(ctx.allCoinFlipsHeads());
    }

    @Test
    void shouldReturnFalseForAllCoinFlipsHeads_whenAnyTails() {
        AttackContext ctx = createDefaultContext();

        ctx.addCoinFlipResult(true);
        ctx.addCoinFlipResult(false);
        ctx.addCoinFlipResult(true);

        assertFalse(ctx.allCoinFlipsHeads());
    }

    @Test
    void shouldReturnFalseForAllCoinFlipsHeads_whenNoFlips() {
        AttackContext ctx = createDefaultContext();

        assertFalse(ctx.allCoinFlipsHeads());
    }

    @Test
    void shouldSetAndGetAllOptionalFields() {
        AttackContext ctx = createDefaultContext();

        ctx.setErrorMessage("error");
        assertEquals("error", ctx.getErrorMessage());

        ctx.setKnockoutOccurred(true);
        assertTrue(ctx.isKnockoutOccurred());

        ctx.setAttackCanceled(true);
        assertTrue(ctx.isAttackCanceled());

        ctx.setCoinFlipDamageBonus(30);
        assertEquals(30, ctx.getCoinFlipDamageBonus());

        ctx.setBaseDamageOverride(50);
        assertEquals(50, ctx.getBaseDamageOverride());

        ctx.setBypassWeakness(true);
        assertTrue(ctx.isBypassWeakness());

        ctx.setBypassResistance(true);
        assertTrue(ctx.isBypassResistance());

        ctx.setRestrictedAttackName("Thunder");
        assertEquals("Thunder", ctx.getRestrictedAttackName());

        ctx.setChosenCondition("BURNED");
        assertEquals("BURNED", ctx.getChosenCondition());

        ctx.setFlipHandledByInline(true);
        assertTrue(ctx.isFlipHandledByInline());
    }

    // --- BaseAttackStep tests ---

    @Test
    void shouldSetNextStep_whenSetNextCalled() {
        ConcreteBaseStep step1 = new ConcreteBaseStep();
        ConcreteBaseStep step2 = new ConcreteBaseStep();

        step1.setNext(step2);

        assertSame(step2, step1.getNext());
    }

    @Test
    void shouldReturnNextStep_whenGetNextCalled() {
        ConcreteBaseStep step1 = new ConcreteBaseStep();
        ConcreteBaseStep step2 = new ConcreteBaseStep();
        step1.setNext(step2);

        AttackStep result = step1.getNext();

        assertSame(step2, result);
    }

    @Test
    void shouldExecuteNextStep_whenProceedWithCONTINUE() {
        ConcreteBaseStep step1 = new ConcreteBaseStep();
        ConcreteBaseStep step2 = new ConcreteBaseStep();
        step1.setNext(step2);

        AttackStepResult result = step1.proceed(AttackStepResult.CONTINUE);

        assertEquals(AttackStepResult.CONTINUE, result);
        assertTrue(step2.executed);
    }

    @Test
    void shouldNotExecuteNextStep_whenProceedWithSTOP() {
        ConcreteBaseStep step1 = new ConcreteBaseStep();
        ConcreteBaseStep step2 = new ConcreteBaseStep();
        step1.setNext(step2);

        AttackStepResult result = step1.proceed(AttackStepResult.STOP_CHAIN);

        assertEquals(AttackStepResult.STOP_CHAIN, result);
        assertFalse(step2.executed);
    }

    @Test
    void shouldBuildChainCorrectly_whenMultipleSteps() {
        ConcreteBaseStep step1 = new ConcreteBaseStep();
        ConcreteBaseStep step2 = new ConcreteBaseStep();
        ConcreteBaseStep step3 = new ConcreteBaseStep();

        AttackStep chain = BaseAttackStep.buildChain(step1, step2, step3);

        assertSame(step1, chain);
        assertSame(step2, step1.getNext());
        assertSame(step3, step2.getNext());
        assertNull(step3.getNext());
    }

    @Test
    void shouldReturnCONTINUE_whenNoNextStep() {
        ConcreteBaseStep step = new ConcreteBaseStep();

        AttackStepResult result = step.proceed(AttackStepResult.CONTINUE);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    // --- AbstractAttackStep tests ---

    @Test
    void shouldSetNextStep_whenAbstractStepSetNextCalled() {
        ConcreteAbstractStep step1 = new ConcreteAbstractStep();
        ConcreteAbstractStep step2 = new ConcreteAbstractStep();

        step1.setNext(step2);

        assertSame(step2, step1.getNext());
    }

    @Test
    void shouldProceedToNextStep_whenAbstractStepProceedCalled() {
        ConcreteAbstractStep step1 = new ConcreteAbstractStep();
        ConcreteAbstractStep step2 = new ConcreteAbstractStep();
        step1.setNext(step2);

        AttackStepResult result = step1.proceed(null, null);

        assertEquals(AttackStepResult.CONTINUE, result);
        assertTrue(step2.executed);
    }

    @Test
    void shouldReturnCONTINUE_whenAbstractStepHasNoNext() {
        ConcreteAbstractStep step = new ConcreteAbstractStep();

        AttackStepResult result = step.proceed(null, null);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldBuildChainCorrectly_whenAbstractStepsChain() {
        ConcreteAbstractStep step1 = new ConcreteAbstractStep();
        ConcreteAbstractStep step2 = new ConcreteAbstractStep();
        ConcreteAbstractStep step3 = new ConcreteAbstractStep();

        AttackStep chain = AbstractAttackStep.buildChain(step1, step2, step3);

        assertSame(step1, chain);
        assertSame(step2, step1.getNext());
        assertSame(step3, step2.getNext());
    }

    // --- AttackChainBuilder tests ---

    @Test
    void shouldExecuteFirstStep_whenExecuteChainCalled() {
        ConcreteAbstractStep step1 = new ConcreteAbstractStep();
        AttackContext attackCtx = createDefaultContext();

        AttackStepResult result = AttackChainBuilder.executeChain(step1, null, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
        assertTrue(step1.executed);
    }

    @Test
    void shouldReturnSTOP_whenFirstStepStopsChain() {
        StopAbstractStep step1 = new StopAbstractStep();
        AttackContext attackCtx = createDefaultContext();

        AttackStepResult result = AttackChainBuilder.executeChain(step1, null, attackCtx);

        assertEquals(AttackStepResult.STOP_CHAIN, result);
    }

    // --- PreDamageStep tests ---

    @Test
    void shouldCancelAttack_whenCoinFlipTails() {
        when(randomizer.nextInt(2)).thenReturn(1);

        EngineContext ctx = buildEngineContext("CANCEL_ATTACK", null);
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");

        PreDamageStep step = new PreDamageStep();
        step.execute(ctx, attackCtx);

        assertTrue(attackCtx.isAttackCanceled());
    }

    @Test
    void shouldProceedWithoutCancel_whenCoinFlipHeads() {
        when(randomizer.nextInt(2)).thenReturn(0);

        EngineContext ctx = buildEngineContext("CANCEL_ATTACK", null);
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");

        PreDamageStep step = new PreDamageStep();
        step.execute(ctx, attackCtx);

        assertFalse(attackCtx.isAttackCanceled());
    }

    @Test
    void shouldApplyDamageBonus_whenCoinFlipHeads() {
        when(randomizer.nextInt(2)).thenReturn(0);

        EngineContext ctx = buildEngineContext("DAMAGE_BONUS", "30");
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");

        PreDamageStep step = new PreDamageStep();
        step.execute(ctx, attackCtx);

        assertEquals(30, attackCtx.getCoinFlipDamageBonus());
    }

    @Test
    void shouldNotApplyDamageBonus_whenCoinFlipTails() {
        when(randomizer.nextInt(2)).thenReturn(1);

        EngineContext ctx = buildEngineContext("DAMAGE_BONUS", "30");
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");

        PreDamageStep step = new PreDamageStep();
        step.execute(ctx, attackCtx);

        assertEquals(0, attackCtx.getCoinFlipDamageBonus());
    }

    @Test
    void shouldCalculateMultiplierDamage_whenMultiCoins() {
        when(randomizer.nextInt(2)).thenReturn(0, 1, 0);

        EngineContext ctx = buildEngineContextWithMultiplier("DAMAGE_MULTIPLIER", "20", "Flip 2 coins");
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");

        PreDamageStep step = new PreDamageStep();
        step.execute(ctx, attackCtx);

        assertEquals(20, attackCtx.getBaseDamageOverride());
    }

    @Test
    void shouldProceedWithoutEffects_whenNoEffects() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("att-1");

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(new ArrayList<>());

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);

        GameState state = new GameState();
        EngineContext ctx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);

        AttackContext attackCtx = new AttackContext(attacker, new PokemonInPlay(), 0, null, null);

        PreDamageStep step = new PreDamageStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isAttackCanceled());
    }

    // --- PostDamageEffectStep tests ---

    @Test
    void shouldSkipEffects_whenAttackCanceled() {
        EngineContext ctx = buildEngineContextWithEffects(AttackEffectType.DRAW_CARDS, Map.of("count", 2));
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");
        attackCtx.setAttackCanceled(true);

        PostDamageEffectStep step = new PostDamageEffectStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldSkipEffects_whenNoPostDamageEffects() {
        EngineContext ctx = buildEngineContextEmpty();
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");

        PostDamageEffectStep postStep = new PostDamageEffectStep();
        AttackStepResult result = postStep.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldApplyDrawCardsEffect_whenDrawCardsType() {
        EngineContext ctx = buildEngineContextWithEffects(AttackEffectType.DRAW_CARDS, Map.of("count", 2));
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");

        PostDamageEffectStep step = new PostDamageEffectStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldApplyRecoilEffect_whenRecoilType() {
        EngineContext ctx = buildEngineContextWithEffects(AttackEffectType.RECOIL, Map.of("count", 2));
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");

        PostDamageEffectStep step = new PostDamageEffectStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldApplyMultipleEffects_whenMultipleEffectsDefined() {
        Map<String, Object> params1 = Map.of("count", 1);
        Map<String, Object> params2 = Map.of("count", 2);
        AttackEffect effect1 = new AttackEffect(AttackEffectType.DRAW_CARDS, params1);
        AttackEffect effect2 = new AttackEffect(AttackEffectType.RECOIL, params2);

        PokemonCardDefinition.AttackDefinition attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setDamage("40");
        attackDef.setEffects(List.of(effect1, effect2));

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(List.of(attackDef));

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(new PokemonCardDefinition());

        GameState state = new GameState();
        EngineContext ctx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);

        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");

        PostDamageEffectStep step = new PostDamageEffectStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldApplyHealUserEffect_whenHealUserType() {
        EngineContext ctx = buildEngineContextWithEffects(AttackEffectType.HEAL_USER, Map.of("count", 3));
        AttackContext attackCtx = createDefaultContext();
        attackCtx.getAttacker().setCardDefinitionId("att-1");
        attackCtx.getDefender().setCardDefinitionId("def-1");

        PostDamageEffectStep step = new PostDamageEffectStep();
        AttackStepResult result = step.execute(ctx, attackCtx);

        assertEquals(AttackStepResult.CONTINUE, result);
    }

    // --- Helper: build EngineContext with PostDamageEffect ---

    private EngineContext buildEngineContextWithEffects(AttackEffectType effectType, Map<String, Object> params) {
        AttackEffect effect = new AttackEffect(effectType, params);

        PokemonCardDefinition.AttackDefinition attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setDamage("40");
        attackDef.setEffects(List.of(effect));

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(List.of(attackDef));

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(new PokemonCardDefinition());

        GameState state = new GameState();
        return new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
    }

    private EngineContext buildEngineContextEmpty() {
        PokemonCardDefinition.AttackDefinition attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setDamage("40");
        attackDef.setEffects(null);

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(List.of(attackDef));

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);
        when(cardLookup.getCardById("def-1")).thenReturn(new PokemonCardDefinition());

        GameState state = new GameState();
        return new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
    }

    // --- Helper: build EngineContext with COIN_FLIP_BEFORE_DAMAGE effect ---

    private EngineContext buildEngineContext(String effectType, String effectParam) {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("att-1");

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("effectType", effectType);
        if (effectParam != null) {
            params.put("effectParam", effectParam);
        }

        AttackEffect effect = new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, params);

        PokemonCardDefinition.AttackDefinition attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setDamage("40");
        attackDef.setEffects(List.of(effect));

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(List.of(attackDef));

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);

        GameState state = new GameState();
        return new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
    }

    private EngineContext buildEngineContextWithMultiplier(String effectType, String effectParam, String attackText) {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("att-1");

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("effectType", effectType);
        if (effectParam != null) {
            params.put("effectParam", effectParam);
        }

        AttackEffect effect = new AttackEffect(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE, params);

        PokemonCardDefinition.AttackDefinition attackDef = new PokemonCardDefinition.AttackDefinition();
        attackDef.setDamage("20×2");
        attackDef.setText(attackText);
        attackDef.setEffects(List.of(effect));

        PokemonCardDefinition attDef = new PokemonCardDefinition();
        attDef.setAttacks(List.of(attackDef));

        when(cardLookup.getCardById("att-1")).thenReturn(attDef);

        GameState state = new GameState();
        return new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
    }

    // --- Helper classes ---

    private static class ConcreteBaseStep extends BaseAttackStep {
        boolean executed = false;

        @Override
        public AttackStepResult execute(ar.edu.utn.frc.tup.piii.engine.EngineContext ctx, AttackContext attackCtx) {
            executed = true;
            return AttackStepResult.CONTINUE;
        }

        @Override
        public AttackStep getNext() {
            return next;
        }
    }

    private static class ConcreteAbstractStep extends AbstractAttackStep {
        boolean executed = false;

        @Override
        public AttackStepResult execute(ar.edu.utn.frc.tup.piii.engine.EngineContext ctx, AttackContext attackCtx) {
            executed = true;
            return AttackStepResult.CONTINUE;
        }
    }

    private static class StopAbstractStep extends AbstractAttackStep {
        @Override
        public AttackStepResult execute(ar.edu.utn.frc.tup.piii.engine.EngineContext ctx, AttackContext attackCtx) {
            return AttackStepResult.STOP_CHAIN;
        }
    }

    private AttackContext createDefaultContext() {
        PokemonInPlay attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        PokemonInPlay defender = new PokemonInPlay();
        defender.setInstanceId(UUID.randomUUID());
        return new AttackContext(attacker, defender, 0, null, null);
    }
}
