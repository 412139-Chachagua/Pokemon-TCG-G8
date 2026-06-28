package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HealUserEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;

    private PokemonInPlay createPokemon(UUID ownerId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setOwnerPlayerId(ownerId);
        p.setDamageCounters(5);
        return p;
    }

    @Test
    void apply_healAttacker_healsByCount() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(3);
        effect.apply(ctx, attackCtx);

        assertEquals(2, attacker.getDamageCounters());
    }

    @Test
    void apply_healTargetBench_healsFirstBench() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PokemonInPlay benchPkm = createPokemon(playerId);
        benchPkm.setDamageCounters(8);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        player.setBench(new ArrayList<>(List.of(benchPkm)));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(4, true);
        effect.apply(ctx, attackCtx);

        assertEquals(4, benchPkm.getDamageCounters());
    }

    @Test
    void apply_healAll_healsActiveAndBench() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PokemonInPlay bench1 = createPokemon(playerId);
        bench1.setDamageCounters(3);
        PokemonInPlay bench2 = createPokemon(playerId);
        bench2.setDamageCounters(7);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        player.setBench(new ArrayList<>(List.of(bench1, bench2)));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(5, false, true, false, false);
        effect.apply(ctx, attackCtx);

        assertEquals(0, attacker.getDamageCounters());
        assertEquals(0, bench1.getDamageCounters());
        assertEquals(2, bench2.getDamageCounters());
    }

    @Test
    void apply_healFull_removesAllDamage() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        attacker.setDamageCounters(8);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(3, false, false, false, true);
        effect.apply(ctx, attackCtx);

        assertEquals(0, attacker.getDamageCounters());
    }

    @Test
    void apply_clearConditions_removesSpecialConditions() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        attacker.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.BURNED, SpecialCondition.POISONED)));
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(2, false, false, true, false);
        effect.apply(ctx, attackCtx);

        assertEquals(3, attacker.getDamageCounters());
        assertTrue(attacker.getSpecialConditions().isEmpty());
    }

    @Test
    void apply_clearDefenderConditions_clearsDefenderConditions() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        defender.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.ASLEEP)));
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        when(ctx.getPlayer(playerId)).thenReturn(player);
        HealUserEffect effect = new HealUserEffect(0, false, false, false, false, true);
        effect.apply(ctx, attackCtx);

        assertTrue(defender.getSpecialConditions().isEmpty());
    }

    @Test
    void apply_noDamageToHeal_stillHealsZero() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        attacker.setDamageCounters(0);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(3);
        effect.apply(ctx, attackCtx);

        assertEquals(0, attacker.getDamageCounters());
        verify(ctx).addEvent(any(GameEvent.class));
    }

    @Test
    void apply_overheal_clampsToZero() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        attacker.setDamageCounters(1);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(null);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(10);
        effect.apply(ctx, attackCtx);

        assertEquals(0, attacker.getDamageCounters());
    }

    @Test
    void apply_nullOwner_noAction() {
        PokemonInPlay attacker = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(ctx.getPlayer(attacker.getOwnerPlayerId())).thenReturn(null);

        HealUserEffect effect = new HealUserEffect(3);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_healTargetById_healsSpecificPokemon() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay benchPkm = createPokemon(playerId);
        benchPkm.setDamageCounters(7);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getHealTargetId()).thenReturn(benchPkm.getInstanceId());

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        player.setBench(new ArrayList<>(List.of(benchPkm)));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getPlayer(playerId)).thenReturn(player);

        HealUserEffect effect = new HealUserEffect(5);
        effect.apply(ctx, attackCtx);

        assertEquals(2, benchPkm.getDamageCounters());
    }

    @Test
    void getTiming_returnsAfterDamage() {
        HealUserEffect effect = new HealUserEffect(3);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}

