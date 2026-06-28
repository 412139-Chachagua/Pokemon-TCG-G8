package ar.edu.utn.frc.tup.piii.engine.attack.steps;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffect;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackEffectType;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackStep;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrerequisiteStepTest {

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
    private PrerequisiteStep step;

    @BeforeEach
    void setUp() {
        step = new PrerequisiteStep();
        attacker = new PokemonInPlay();
        attacker.setInstanceId(UUID.randomUUID());
        attacker.setCardDefinitionId("pkm-pikachu");
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
    void shouldProceedWhenNoEffects() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldProceedWhenAttacksListIsNull() {
        attackerDef.setAttacks(null);
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldProceedWhenCardDefIsNotPokemon() {
        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(null);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
    }

    @Test
    void shouldCancelAttackWhenCoinFlipTails() {
        AttackEffect cancelEffect = new AttackEffect();
        cancelEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        cancelEffect.setParams(Map.of("effectType", "CANCEL_ATTACK"));
        attackDef.setEffects(List.of(cancelEffect));

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1); // tails
        lenient().when(ctx.getState()).thenReturn(state);
        lenient().when(state.getMatchId()).thenReturn(UUID.randomUUID());
        lenient().when(state.getTurnNumber()).thenReturn(1);

        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertTrue(attackCtx.isAttackCanceled());
        verify(ctx, atLeast(2)).addEvent(any());
    }

    @Test
    void shouldNotCancelAttackWhenCoinFlipHeads() {
        AttackEffect cancelEffect = new AttackEffect();
        cancelEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        cancelEffect.setParams(Map.of("effectType", "CANCEL_ATTACK"));
        attackDef.setEffects(List.of(cancelEffect));

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0); // heads
        lenient().when(ctx.getState()).thenReturn(state);
        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isAttackCanceled());
    }

    @Test
    void shouldSetDamageBonusWhenCoinFlipHeads() {
        AttackEffect bonusEffect = new AttackEffect();
        bonusEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        bonusEffect.setParams(Map.of("effectType", "DAMAGE_BONUS", "effectParam", "20"));
        attackDef.setEffects(List.of(bonusEffect));

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0); // heads
        lenient().when(ctx.getState()).thenReturn(state);
        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertEquals(20, attackCtx.getCoinFlipDamageBonus());
    }

    @Test
    void shouldNotSetDamageBonusWhenCoinFlipTails() {
        AttackEffect bonusEffect = new AttackEffect();
        bonusEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        bonusEffect.setParams(Map.of("effectType", "DAMAGE_BONUS", "effectParam", "20"));
        attackDef.setEffects(List.of(bonusEffect));

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1); // tails
        lenient().when(ctx.getState()).thenReturn(state);
        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertEquals(0, attackCtx.getCoinFlipDamageBonus());
    }

    @Test
    void shouldHandleMultipleEffects() {
        AttackEffect cancelEffect = new AttackEffect();
        cancelEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        cancelEffect.setParams(Map.of("effectType", "CANCEL_ATTACK"));

        AttackEffect bonusEffect = new AttackEffect();
        bonusEffect.setType(AttackEffectType.COIN_FLIP_BEFORE_DAMAGE);
        bonusEffect.setParams(Map.of("effectType", "DAMAGE_BONUS", "effectParam", "10"));

        attackDef.setEffects(List.of(cancelEffect, bonusEffect));

        attackCtx = new AttackContext(attacker, defender, 0, Map.of(), null);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById("pkm-pikachu")).thenReturn(attackerDef);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0); // heads for both
        lenient().when(ctx.getState()).thenReturn(state);
        var result = step.execute(ctx, attackCtx);

        assertEquals(AttackStep.AttackStepResult.CONTINUE, result);
        assertFalse(attackCtx.isAttackCanceled());
        assertEquals(10, attackCtx.getCoinFlipDamageBonus());
    }
}
