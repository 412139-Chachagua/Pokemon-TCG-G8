package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DamageBenchEffectTest {

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
        p.setDamageCounters(0);
        return p;
    }

    @Test
    void apply_damageToOpponentBench_appliesDamage() {
        UUID attackerId = UUID.randomUUID();
        UUID opponentId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(attackerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState attackerPlayer = new PlayerState();
        attackerPlayer.setPlayerId(attackerId);
        attackerPlayer.setActivePokemon(attacker);

        List<PokemonInPlay> bench = new ArrayList<>();
        PokemonInPlay bench1 = createPokemon(opponentId);
        PokemonInPlay bench2 = createPokemon(opponentId);
        bench.add(bench1);
        bench.add(bench2);

        PlayerState opponentPlayer = new PlayerState();
        opponentPlayer.setPlayerId(opponentId);
        opponentPlayer.setBench(bench);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{attackerPlayer, opponentPlayer});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        List<Map<String, Object>> benchTargets = List.of(
                Map.of("instanceId", bench1.getInstanceId().toString()),
                Map.of("instanceId", bench2.getInstanceId().toString())
        );
        when(attackCtx.getBenchTargets()).thenReturn(benchTargets);

        DamageBenchEffect effect = new DamageBenchEffect(20);
        effect.apply(ctx, attackCtx);

        assertEquals(2, bench1.getDamageCounters());
        assertEquals(2, bench2.getDamageCounters());
        ArgumentCaptor<GameEvent> eventCaptor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(eventCaptor.capture());
        assertEquals("BENCH_DAMAGE", eventCaptor.getValue().getType());
    }

    @Test
    void apply_damageToOwnBench_appliesDamageToOwn() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PokemonInPlay bench1 = createPokemon(playerId);
        PokemonInPlay bench2 = createPokemon(playerId);
        player.setBench(new ArrayList<>(List.of(bench1, bench2)));

        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        DamageBenchEffect effect = new DamageBenchEffect(30, true);
        effect.apply(ctx, attackCtx);

        assertEquals(3, bench1.getDamageCounters());
        assertEquals(3, bench2.getDamageCounters());
    }

    @Test
    void apply_emptyBench_noDamageApplied() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        player.setBench(new ArrayList<>());

        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        DamageBenchEffect effect = new DamageBenchEffect(10);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_nullBenchTargets_noDamage() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setBench(new ArrayList<>(List.of(createPokemon(UUID.randomUUID()))));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        DamageBenchEffect effect = new DamageBenchEffect(10);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_nullBenchOnPlayer_noDamage() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setBench(null);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        DamageBenchEffect effect = new DamageBenchEffect(10);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_benchTargetsWithNullInstanceId_skipped() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);

        PokemonInPlay benchPkm = createPokemon(UUID.randomUUID());
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setBench(new ArrayList<>(List.of(benchPkm)));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        List<Map<String, Object>> benchTargets = List.of(Map.of("instanceId", benchPkm.getInstanceId().toString()));
        when(attackCtx.getBenchTargets()).thenReturn(benchTargets);

        DamageBenchEffect effect = new DamageBenchEffect(10);
        effect.apply(ctx, attackCtx);

        assertEquals(1, benchPkm.getDamageCounters());
    }

    @Test
    void getTiming_returnsAfterDamage() {
        DamageBenchEffect effect = new DamageBenchEffect(10);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
