package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemainingEffectsTest {

    @Mock
    protected EngineContext ctx;
    @Mock
    protected AttackContext attackCtx;
    @Mock
    protected GameState state;
    @Mock
    protected CardLookupPort cardLookup;
    @Mock
    protected RandomizerPort randomizer;
    @Mock
    protected EnergyService energyService;

    protected PokemonInPlay createPokemon(UUID ownerId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setCardDefinitionId("pkm-1");
        p.setOwnerPlayerId(ownerId);
        p.setDamageCounters(0);
        p.setSpecialConditions(new ArrayList<>());
        p.setAttachedEnergies(new ArrayList<>());
        return p;
    }

    protected PlayerState createPlayer(UUID playerId, PokemonInPlay active) {
        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(active);
        player.setDiscard(new ArrayList<>());
        player.setHand(new ArrayList<>());
        player.setDeck(new LinkedList<>());
        player.setBench(new ArrayList<>());
        return player;
    }

    protected void mockState(PlayerState player, PlayerState opponent) {
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(ctx.getPlayer(any())).thenAnswer(invocation -> {
            UUID pid = invocation.getArgument(0);
            for (PlayerState p : state.getPlayers()) {
                if (p.getPlayerId().equals(pid)) return p;
            }
            return null;
        });
        when(ctx.getOpponent(any())).thenAnswer(invocation -> {
            UUID pid = invocation.getArgument(0);
            for (PlayerState p : state.getPlayers()) {
                if (!p.getPlayerId().equals(pid)) return p;
            }
            return null;
        });
    }

    // ──────────────────────────────────────────────
    // AbilitySuppressionEffect
    // ──────────────────────────────────────────────

    @Test
    void abilitySuppression_shouldSuppressDefenderAbilities_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        AbilitySuppressionEffect effect = new AbilitySuppressionEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(defender.isAbilitiesSuppressedNextTurn());
    }

    @Test
    void abilitySuppression_shouldNotFail_whenDefenderIsNull() {
        when(attackCtx.getDefender()).thenReturn(null);

        AbilitySuppressionEffect effect = new AbilitySuppressionEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void abilitySuppression_shouldAddEvent_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        AbilitySuppressionEffect effect = new AbilitySuppressionEffect();
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void abilitySuppression_getTiming_returnsAfterDamage() {
        AbilitySuppressionEffect effect = new AbilitySuppressionEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // AttachEnergyEffect
    // ──────────────────────────────────────────────

    @Test
    void attachEnergy_shouldAttachFromDeckToAttacker_whenEnergyAvailable() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        CardInstance energyCard = new CardInstance(UUID.randomUUID(), "fire-1");
        PlayerState player = createPlayer(playerId, attacker);
        player.getDeck().add(energyCard);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        EnergyCardDefinition energyDef = new EnergyCardDefinition();
        energyDef.setEnergyCardType(EnergyCardType.BASIC);
        energyDef.setProvides(List.of(EnergyType.FIRE));
        when(cardLookup.getCardById("fire-1")).thenReturn(energyDef);

        AttachEnergyEffect effect = new AttachEnergyEffect("deck", null, 1, "attacker");
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void attachEnergy_shouldNotAttach_whenNoEnergyInDeck() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        AttachEnergyEffect effect = new AttachEnergyEffect("deck", null, 1, "attacker");
        effect.apply(ctx, attackCtx);

        assertFalse(attackCtx.isEnergyAttachedThisAttack());
    }

    @Test
    void attachEnergy_shouldNotAttach_whenPlayerIsNull() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{createPlayer(UUID.randomUUID(), createPokemon(UUID.randomUUID()))});

        AttachEnergyEffect effect = new AttachEnergyEffect("deck", null, 1, "attacker");
        effect.apply(ctx, attackCtx);

        assertFalse(attackCtx.isEnergyAttachedThisAttack());
    }

    @Test
    void attachEnergy_getTiming_returnsAfterDamage() {
        AttachEnergyEffect effect = new AttachEnergyEffect("deck", null, 1, "attacker");
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // CanNotAttackNextTurnEffect
    // ──────────────────────────────────────────────

    @Test
    void canNotAttackNextTurn_shouldMarkAttacker_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        CanNotAttackNextTurnEffect effect = new CanNotAttackNextTurnEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(attacker.isCannotAttackNextTurn());
    }

    @Test
    void canNotAttackNextTurn_shouldSetRestrictedAttackName_whenProvided() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        CanNotAttackNextTurnEffect effect = new CanNotAttackNextTurnEffect("Fire Blast");
        effect.apply(ctx, attackCtx);

        assertEquals("Fire Blast", attacker.getRestrictedAttackName());
    }

    @Test
    void canNotAttackNextTurn_shouldFireEvent_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        CanNotAttackNextTurnEffect effect = new CanNotAttackNextTurnEffect();
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void canNotAttackNextTurn_getTiming_returnsAfterDamage() {
        CanNotAttackNextTurnEffect effect = new CanNotAttackNextTurnEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // CoinFlipPostDamageEffect
    // ──────────────────────────────────────────────

    @Test
    void coinFlipPostDamage_shouldApplySubEffect_whenHeads() {
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        PostDamageEffect subEffect = mock(PostDamageEffect.class);
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(subEffect);
        effect.apply(ctx, attackCtx);

        verify(subEffect).apply(ctx, attackCtx);
    }

    @Test
    void coinFlipPostDamage_shouldSkipSubEffect_whenTails() {
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        PostDamageEffect subEffect = mock(PostDamageEffect.class);
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(subEffect);
        effect.apply(ctx, attackCtx);

        verify(subEffect, never()).apply(ctx, attackCtx);
    }

    @Test
    void coinFlipPostDamage_shouldApplyOnTails_whenApplyOnHeadsIsFalse() {
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        PostDamageEffect subEffect = mock(PostDamageEffect.class);
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(subEffect, false);
        effect.apply(ctx, attackCtx);

        verify(subEffect).apply(ctx, attackCtx);
    }

    @Test
    void coinFlipPostDamage_shouldNotFail_whenSubEffectIsNull() {
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(null);
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void coinFlipPostDamage_allHeadsMode_shouldApply_whenAllHeads() {
        when(attackCtx.getTotalCoinFlips()).thenReturn(2);
        when(attackCtx.allCoinFlipsHeads()).thenReturn(true);

        PostDamageEffect subEffect = mock(PostDamageEffect.class);
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(subEffect, true, true);
        effect.apply(ctx, attackCtx);

        verify(subEffect).apply(ctx, attackCtx);
    }

    @Test
    void coinFlipPostDamage_allHeadsMode_shouldSkip_whenNotAllHeads() {
        when(attackCtx.getTotalCoinFlips()).thenReturn(2);
        when(attackCtx.allCoinFlipsHeads()).thenReturn(false);

        PostDamageEffect subEffect = mock(PostDamageEffect.class);
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(subEffect, true, true);
        effect.apply(ctx, attackCtx);

        verify(subEffect, never()).apply(ctx, attackCtx);
    }

    @Test
    void coinFlipPostDamage_getTiming_returnsAfterDamage() {
        CoinFlipPostDamageEffect effect = new CoinFlipPostDamageEffect(null);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DamageAllBenchEffect
    // ──────────────────────────────────────────────

    @Test
    void damageAllBench_shouldDamageAllOpponentPokemon_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PokemonInPlay bench1 = createPokemon(UUID.randomUUID());
        PokemonInPlay bench2 = createPokemon(UUID.randomUUID());
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        opponent.setBench(new ArrayList<>(List.of(bench1, bench2)));
        PlayerState player = createPlayer(playerId, attacker);
        mockState(player, opponent);

        DamageAllBenchEffect effect = new DamageAllBenchEffect(2);
        effect.apply(ctx, attackCtx);

        assertEquals(2, defender.getDamageCounters());
        assertEquals(2, bench1.getDamageCounters());
        assertEquals(2, bench2.getDamageCounters());
    }

    @Test
    void damageAllBench_shouldNotThrow_whenOpponentIsNull() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{createPlayer(UUID.randomUUID(), createPokemon(UUID.randomUUID()))});

        DamageAllBenchEffect effect = new DamageAllBenchEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void damageAllBench_getTiming_returnsAfterDamage() {
        DamageAllBenchEffect effect = new DamageAllBenchEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DamagePreventionEffect
    // ──────────────────────────────────────────────

    @Test
    void damagePrevention_shouldSetPreventionOnAttacker_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DamagePreventionEffect effect = new DamagePreventionEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(attacker.isPreventAllDamageNextTurn());
    }

    @Test
    void damagePrevention_shouldSetThreshold_whenProvided() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DamagePreventionEffect effect = new DamagePreventionEffect(true, 30);
        effect.apply(ctx, attackCtx);

        assertEquals(Integer.valueOf(30), attacker.getPreventionDamageThreshold());
    }

    @Test
    void damagePrevention_getTiming_returnsAfterDamage() {
        DamagePreventionEffect effect = new DamagePreventionEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DamageReductionEffect
    // ──────────────────────────────────────────────

    @Test
    void damageReduction_shouldSetReductionOnAttacker_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DamageReductionEffect effect = new DamageReductionEffect(20);
        effect.apply(ctx, attackCtx);

        assertEquals(20, attacker.getReduceDamageNextTurn());
    }

    @Test
    void damageReduction_getTiming_returnsAfterDamage() {
        DamageReductionEffect effect = new DamageReductionEffect(10);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DefenderCannotAttackEffect
    // ──────────────────────────────────────────────

    @Test
    void defenderCannotAttack_shouldMarkDefender_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DefenderCannotAttackEffect effect = new DefenderCannotAttackEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(defender.isCannotAttackNextTurn());
    }

    @Test
    void defenderCannotAttack_shouldSetRestrictedAttackName_whenSetInContext() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.getRestrictedAttackName()).thenReturn("Thunderbolt");
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DefenderCannotAttackEffect effect = new DefenderCannotAttackEffect();
        effect.apply(ctx, attackCtx);

        assertEquals("Thunderbolt", defender.getRestrictedAttackName());
    }

    @Test
    void defenderCannotAttack_getTiming_returnsAfterDamage() {
        DefenderCannotAttackEffect effect = new DefenderCannotAttackEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DiscardOpponentDeckEffect
    // ──────────────────────────────────────────────

    @Test
    void discardOpponentDeck_shouldDiscardCardsFromOpponentDeck_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "card-2"));
        mockState(player, opponent);

        DiscardOpponentDeckEffect effect = new DiscardOpponentDeckEffect(2);
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getDeck().isEmpty());
        assertEquals(2, opponent.getDiscard().size());
    }

    @Test
    void discardOpponentDeck_shouldNotDiscard_whenDeckIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        DiscardOpponentDeckEffect effect = new DiscardOpponentDeckEffect(2);
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getDiscard().isEmpty());
    }

    @Test
    void discardOpponentDeck_shouldDiscardFromSelf_whenTargetIsSelf() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        DiscardOpponentDeckEffect effect = new DiscardOpponentDeckEffect(1, "self");
        effect.apply(ctx, attackCtx);

        assertTrue(player.getDeck().isEmpty());
        assertEquals(1, player.getDiscard().size());
    }

    @Test
    void discardOpponentDeck_getTiming_returnsAfterDamage() {
        DiscardOpponentDeckEffect effect = new DiscardOpponentDeckEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // DiscardToolEffect
    // ──────────────────────────────────────────────

    @Test
    void discardTool_shouldRemoveToolFromDefender_whenToolPresent() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        CardInstance tool = new CardInstance(UUID.randomUUID(), "tool-1");
        defender.setAttachedTool(tool);
        defender.setToolCardInstanceId(tool.getInstanceId());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DiscardToolEffect effect = new DiscardToolEffect();
        effect.apply(ctx, attackCtx);

        assertNull(defender.getToolCardInstanceId());
        assertNull(defender.getAttachedTool());
    }

    @Test
    void discardTool_shouldNotFail_whenNoTool() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        DiscardToolEffect effect = new DiscardToolEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void discardTool_shouldNotFail_whenDefenderIsNull() {
        when(attackCtx.getDefender()).thenReturn(null);

        DiscardToolEffect effect = new DiscardToolEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void discardTool_getTiming_returnsAfterDamage() {
        DiscardToolEffect effect = new DiscardToolEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // MentalPanicEffect
    // ──────────────────────────────────────────────

    @Test
    void mentalPanic_shouldSetMustFlipToAttackOnDefender_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        MentalPanicEffect effect = new MentalPanicEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(defender.isMustFlipToAttackNextTurn());
    }

    @Test
    void mentalPanic_shouldFireEvent_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        MentalPanicEffect effect = new MentalPanicEffect();
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void mentalPanic_getTiming_returnsAfterDamage() {
        MentalPanicEffect effect = new MentalPanicEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // MoveEnergyEffect
    // ──────────────────────────────────────────────

    @Test
    void moveEnergy_shouldMoveFromAttackerToBench_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        CardInstance energy = new CardInstance(UUID.randomUUID(), "fire-1");
        attacker.setAttachedEnergies(new ArrayList<>(List.of(energy)));

        PokemonInPlay benchPkm = createPokemon(playerId);
        PlayerState player = createPlayer(playerId, attacker);
        player.setBench(new ArrayList<>(List.of(benchPkm)));

        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        mockState(player, createPlayer(UUID.randomUUID(), defender));

        MoveEnergyEffect effect = new MoveEnergyEffect("attacker", "ownBench", 1);
        effect.apply(ctx, attackCtx);

        verify(energyService).transferEnergy(energy, attacker, benchPkm, player, ctx);
    }

    @Test
    void moveEnergy_shouldNotMove_whenNoEnergyAttached() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        MoveEnergyEffect effect = new MoveEnergyEffect("attacker", "ownBench", 1);
        effect.apply(ctx, attackCtx);

        verify(energyService, never()).transferEnergy(any(), any(), any(), any(), any());
    }

    @Test
    void moveEnergy_shouldNotMove_whenNoBench() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        CardInstance energy = new CardInstance(UUID.randomUUID(), "fire-1");
        attacker.setAttachedEnergies(new ArrayList<>(List.of(energy)));

        PlayerState player = createPlayer(playerId, attacker);
        player.setBench(new ArrayList<>());

        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        MoveEnergyEffect effect = new MoveEnergyEffect("attacker", "ownBench", 1);
        effect.apply(ctx, attackCtx);

        verify(energyService, never()).transferEnergy(any(), any(), any(), any(), any());
    }

    @Test
    void moveEnergy_getTiming_returnsAfterDamage() {
        MoveEnergyEffect effect = new MoveEnergyEffect("attacker", "ownBench", 1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // NextTurnDamageBonusEffect
    // ──────────────────────────────────────────────

    @Test
    void nextTurnDamageBonus_shouldAddBonusToAttacker_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        NextTurnDamageBonusEffect effect = new NextTurnDamageBonusEffect(30);
        effect.apply(ctx, attackCtx);

        assertEquals(30, attacker.getNextTurnDamageBonus());
    }

    @Test
    void nextTurnDamageBonus_shouldAccumulate_whenAppliedMultipleTimes() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        attacker.setNextTurnDamageBonus(10);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        NextTurnDamageBonusEffect effect = new NextTurnDamageBonusEffect(20);
        effect.apply(ctx, attackCtx);

        assertEquals(30, attacker.getNextTurnDamageBonus());
    }

    @Test
    void nextTurnDamageBonus_getTiming_returnsAfterDamage() {
        NextTurnDamageBonusEffect effect = new NextTurnDamageBonusEffect(10);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // OpponentDiscardHandEffect
    // ──────────────────────────────────────────────

    @Test
    void opponentDiscardHand_shouldDiscardFromOpponentHand_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        opponent.getHand().add(new CardInstance(UUID.randomUUID(), "card-1"));
        opponent.getHand().add(new CardInstance(UUID.randomUUID(), "card-2"));
        mockState(player, opponent);

        OpponentDiscardHandEffect effect = new OpponentDiscardHandEffect(2);
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getHand().isEmpty());
        assertEquals(2, opponent.getDiscard().size());
    }

    @Test
    void opponentDiscardHand_shouldNotDiscard_whenOpponentHandIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        OpponentDiscardHandEffect effect = new OpponentDiscardHandEffect(2);
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getDiscard().isEmpty());
    }

    @Test
    void opponentDiscardHand_getTiming_returnsAfterDamage() {
        OpponentDiscardHandEffect effect = new OpponentDiscardHandEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // OpponentRandomDiscardEffect
    // ──────────────────────────────────────────────

    @Test
    void opponentRandomDiscard_shouldPickRandomCardAndShuffleIntoDeck_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        CardInstance card = new CardInstance(UUID.randomUUID(), "card-1");
        opponent.getHand().add(card);
        opponent.getDeck().clear();
        mockState(player, opponent);

        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(1)).thenReturn(0);
        when(cardLookup.getCardById("card-1")).thenReturn(null);

        OpponentRandomDiscardEffect effect = new OpponentRandomDiscardEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getHand().isEmpty());
        assertEquals(1, opponent.getDeck().size());
        verify(randomizer).shuffle(opponent.getDeck());
    }

    @Test
    void opponentRandomDiscard_shouldNotFail_whenOpponentHandIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        OpponentRandomDiscardEffect effect = new OpponentRandomDiscardEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void opponentRandomDiscard_getTiming_returnsAfterDamage() {
        OpponentRandomDiscardEffect effect = new OpponentRandomDiscardEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // PeekOpponentDeckEffect
    // ──────────────────────────────────────────────

    @Test
    void peekOpponentDeck_shouldPeekTopCard_whenDeckHasCard() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        mockState(player, opponent);

        when(cardLookup.getCardById("card-1")).thenReturn(null);

        PeekOpponentDeckEffect effect = new PeekOpponentDeckEffect();
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void peekOpponentDeck_shouldNotFail_whenDeckIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        PeekOpponentDeckEffect effect = new PeekOpponentDeckEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void peekOpponentDeck_getTiming_returnsAfterDamage() {
        PeekOpponentDeckEffect effect = new PeekOpponentDeckEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // PostDamageEffect (abstract)
    // ──────────────────────────────────────────────

    @Test
    void postDamageEffect_concreteImplementation_shouldApplyAndReturnTiming() {
        PostDamageEffect effect = new PostDamageEffect() {
            @Override
            public void apply(EngineContext ctx, AttackContext attackCtx) {
                attackCtx.getAttacker().setDamageCounters(50);
            }

            @Override
            public EffectTiming getTiming() {
                return EffectTiming.BEFORE_DAMAGE;
            }
        };

        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);

        effect.apply(ctx, attackCtx);

        assertEquals(50, attacker.getDamageCounters());
        assertEquals(PostDamageEffect.EffectTiming.BEFORE_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // RecycleFromDiscardEffect
    // ──────────────────────────────────────────────

    @Test
    void recycleFromDiscard_shouldMoveTopDiscardToDeckTop_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        CardInstance card = new CardInstance(UUID.randomUUID(), "card-1");
        player.getDiscard().add(card);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        RecycleFromDiscardEffect effect = new RecycleFromDiscardEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(player.getDiscard().isEmpty());
        assertEquals(card, player.getDeck().getFirst());
    }

    @Test
    void recycleFromDiscard_shouldNotFail_whenDiscardIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getPlayer(playerId)).thenReturn(createPlayer(playerId, attacker));

        RecycleFromDiscardEffect effect = new RecycleFromDiscardEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void recycleFromDiscard_getTiming_returnsAfterDamage() {
        RecycleFromDiscardEffect effect = new RecycleFromDiscardEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // ReorderDeckEffect
    // ──────────────────────────────────────────────

    @Test
    void reorderDeck_shouldFireReorderEvent_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        player.setActivePokemon(attacker);
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-1"));
        player.getDeck().add(new CardInstance(UUID.randomUUID(), "card-2"));
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        ReorderDeckEffect effect = new ReorderDeckEffect(2);
        effect.apply(ctx, attackCtx);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(captor.capture());
        assertEquals(GameEventType.DECK_ORDERED.name(), captor.getValue().getType());
    }

    @Test
    void reorderDeck_shouldNotFail_whenPlayerHasNoActive() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{createPlayer(UUID.randomUUID(), null)});

        ReorderDeckEffect effect = new ReorderDeckEffect(2);
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void reorderDeck_getTiming_returnsAfterDamage() {
        ReorderDeckEffect effect = new ReorderDeckEffect(1);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // RetreatLockEffect
    // ──────────────────────────────────────────────

    @Test
    void retreatLock_shouldSetCannotRetreatOnDefender_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RetreatLockEffect effect = new RetreatLockEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(defender.isCannotRetreatNextTurn());
    }

    @Test
    void retreatLock_shouldFireEvent_whenApplied() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        RetreatLockEffect effect = new RetreatLockEffect();
        effect.apply(ctx, attackCtx);

        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void retreatLock_getTiming_returnsAfterDamage() {
        RetreatLockEffect effect = new RetreatLockEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // SearchDiscardEffect
    // ──────────────────────────────────────────────

    @Test
    void searchDiscard_shouldRetrieveCardsFromDiscardToHand_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        CardInstance card1 = new CardInstance(UUID.randomUUID(), "card-1");
        CardInstance card2 = new CardInstance(UUID.randomUUID(), "card-2");
        player.getDiscard().add(card1);
        player.getDiscard().add(card2);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        SearchDiscardEffect effect = new SearchDiscardEffect(2);
        effect.apply(ctx, attackCtx);

        assertEquals(2, player.getHand().size());
        assertTrue(player.getDiscard().isEmpty());
    }

    @Test
    void searchDiscard_shouldNotRetrieve_whenDiscardIsEmpty() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        SearchDiscardEffect effect = new SearchDiscardEffect(2);
        effect.apply(ctx, attackCtx);

        assertTrue(player.getHand().isEmpty());
    }

    @Test
    void searchDiscard_shouldFilterByItemType_whenSpecified() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        CardInstance itemCard = new CardInstance(UUID.randomUUID(), "item-1");
        CardInstance otherCard = new CardInstance(UUID.randomUUID(), "other-1");
        player.getDiscard().add(itemCard);
        player.getDiscard().add(otherCard);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        mockState(player, opponent);

        CardDefinition itemDef = new CardDefinition() {
            @Override public String getName() { return "Item"; }
            @Override public String getSupertype() { return "TRAINER"; }
            @Override public List<String> getSubtypes() { return List.of("ITEM"); }
        };
        CardDefinition otherDef = new CardDefinition() {
            @Override public String getName() { return "Other"; }
            @Override public String getSupertype() { return "POKEMON"; }
            @Override public List<String> getSubtypes() { return List.of(); }
        };
        when(cardLookup.getCardById("item-1")).thenReturn(itemDef);
        when(cardLookup.getCardById("other-1")).thenReturn(otherDef);

        SearchDiscardEffect effect = new SearchDiscardEffect(2, "ITEM");
        effect.apply(ctx, attackCtx);

        assertEquals(1, player.getHand().size());
        assertEquals("item-1", player.getHand().get(0).getCardDefinitionId());
    }

    @Test
    void searchDiscard_getTiming_returnsAfterDamage() {
        SearchDiscardEffect effect = new SearchDiscardEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // SetHpEffect
    // ──────────────────────────────────────────────

    @Test
    void setHp_shouldSetBothPokemonHp_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setHp(100);
        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        SetHpEffect effect = new SetHpEffect(70);
        effect.apply(ctx, attackCtx);

        assertEquals(3, attacker.getDamageCounters());
        assertEquals(3, defender.getDamageCounters());
    }

    @Test
    void setHp_shouldNotSetNegativeDamage_whenTargetHpExceedsMaxHp() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setHp(100);
        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        SetHpEffect effect = new SetHpEffect(120);
        effect.apply(ctx, attackCtx);

        assertEquals(0, attacker.getDamageCounters());
        assertEquals(0, defender.getDamageCounters());
    }

    @Test
    void setHp_getTiming_returnsAfterDamage() {
        SetHpEffect effect = new SetHpEffect(70);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // SupporterLockEffect
    // ──────────────────────────────────────────────

    @Test
    void supporterLock_shouldBlockOpponentSupporters_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        PlayerState player = createPlayer(playerId, attacker);
        mockState(player, opponent);

        SupporterLockEffect effect = new SupporterLockEffect();
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.isCannotPlaySupportersNextTurn());
    }

    @Test
    void supporterLock_shouldNotFail_whenOpponentNotFound() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{createPlayer(UUID.randomUUID(), attacker)});

        SupporterLockEffect effect = new SupporterLockEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void supporterLock_getTiming_returnsAfterDamage() {
        SupporterLockEffect effect = new SupporterLockEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // SwitchDefenderEffect
    // ──────────────────────────────────────────────

    @Test
    void switchDefender_shouldSwitchDefenderWithBench_whenBenchAvailable() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PokemonInPlay benchPkm = createPokemon(playerId);
        PlayerState owner = createPlayer(playerId, defender);
        owner.setBench(new ArrayList<>(List.of(benchPkm)));
        when(ctx.getPlayer(playerId)).thenReturn(owner);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SwitchDefenderEffect effect = new SwitchDefenderEffect();
        effect.apply(ctx, attackCtx);

        assertEquals(benchPkm, owner.getActivePokemon());
        assertTrue(owner.getBench().contains(defender));
    }

    @Test
    void switchDefender_shouldNotSwitch_whenNoBench() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState owner = createPlayer(playerId, defender);
        owner.setBench(new ArrayList<>());
        when(ctx.getPlayer(playerId)).thenReturn(owner);

        SwitchDefenderEffect effect = new SwitchDefenderEffect();
        effect.apply(ctx, attackCtx);

        assertEquals(defender, owner.getActivePokemon());
    }

    @Test
    void switchDefender_shouldSwitchAttacker_whenSwitchAttackerIsTrue() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PokemonInPlay benchPkm = createPokemon(playerId);
        PlayerState owner = createPlayer(playerId, attacker);
        owner.setBench(new ArrayList<>(List.of(benchPkm)));
        when(ctx.getPlayer(playerId)).thenReturn(owner);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SwitchDefenderEffect effect = new SwitchDefenderEffect(true);
        effect.apply(ctx, attackCtx);

        assertEquals(benchPkm, owner.getActivePokemon());
        assertTrue(owner.getBench().contains(attacker));
    }

    @Test
    void switchDefender_shouldNotSwitch_whenConditionalOnEnergyAndNoEnergyAttached() {
        when(attackCtx.isEnergyAttachedThisAttack()).thenReturn(false);

        SwitchDefenderEffect effect = new SwitchDefenderEffect(false, true);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any(GameEvent.class));
    }

    @Test
    void switchDefender_shouldSwitch_whenConditionalOnEnergyAndEnergyAttached() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        PokemonInPlay defender = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);
        when(attackCtx.isEnergyAttachedThisAttack()).thenReturn(true);

        PokemonInPlay benchPkm = createPokemon(playerId);
        PlayerState owner = createPlayer(playerId, defender);
        owner.setBench(new ArrayList<>(List.of(benchPkm)));
        when(ctx.getPlayer(playerId)).thenReturn(owner);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        SwitchDefenderEffect effect = new SwitchDefenderEffect(false, true);
        effect.apply(ctx, attackCtx);

        assertEquals(benchPkm, owner.getActivePokemon());
    }

    @Test
    void switchDefender_getTiming_returnsAfterDamage() {
        SwitchDefenderEffect effect = new SwitchDefenderEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }

    // ──────────────────────────────────────────────
    // OpponentShuffleDrawEffect
    // ──────────────────────────────────────────────

    @Test
    void opponentShuffleDraw_shouldShuffleHandIntoDeckAndDraw_whenApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = createPlayer(playerId, attacker);
        PlayerState opponent = createPlayer(UUID.randomUUID(), defender);
        opponent.getHand().add(new CardInstance(UUID.randomUUID(), "card-1"));
        opponent.getHand().add(new CardInstance(UUID.randomUUID(), "card-2"));
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "deck-1"));
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "deck-2"));
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "deck-3"));
        opponent.getDeck().add(new CardInstance(UUID.randomUUID(), "deck-4"));
        mockState(player, opponent);

        when(ctx.getRandomizer()).thenReturn(randomizer);

        OpponentShuffleDrawEffect effect = new OpponentShuffleDrawEffect(4);
        effect.apply(ctx, attackCtx);

        assertTrue(opponent.getHand().size() <= 4);
        assertTrue(opponent.getDeck().isEmpty() || opponent.getDeck().size() + opponent.getHand().size() == 6);
        verify(randomizer).shuffle(opponent.getDeck());
    }

    @Test
    void opponentShuffleDraw_shouldNotFail_whenOpponentIsNull() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{createPlayer(UUID.randomUUID(), attacker)});

        OpponentShuffleDrawEffect effect = new OpponentShuffleDrawEffect();
        assertDoesNotThrow(() -> effect.apply(ctx, attackCtx));
    }

    @Test
    void opponentShuffleDraw_getTiming_returnsAfterDamage() {
        OpponentShuffleDrawEffect effect = new OpponentShuffleDrawEffect();
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
