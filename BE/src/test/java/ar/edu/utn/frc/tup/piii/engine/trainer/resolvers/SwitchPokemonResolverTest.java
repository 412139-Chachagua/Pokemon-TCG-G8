package ar.edu.utn.frc.tup.piii.engine.trainer.resolvers;

import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SwitchPokemonResolverTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerCardDefinition card;
    @Mock
    private GameState state;

    @Captor
    private ArgumentCaptor<GameEvent> eventCaptor;

    private SwitchPokemonResolver resolver;
    private PokemonInPlay active;
    private PokemonInPlay benchTarget;
    private List<PokemonInPlay> bench;
    private UUID activeId;
    private UUID benchTargetId;

    @BeforeEach
    void setUp() {
        resolver = new SwitchPokemonResolver();

        activeId = UUID.randomUUID();
        benchTargetId = UUID.randomUUID();

        active = new PokemonInPlay();
        active.setInstanceId(activeId);
        active.setCardDefinitionId("active-pkm");

        benchTarget = new PokemonInPlay();
        benchTarget.setInstanceId(benchTargetId);
        benchTarget.setCardDefinitionId("bench-pkm");

        bench = new ArrayList<>(List.of(benchTarget));

        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(5);
        when(player.getBench()).thenReturn(bench);
        when(player.getActivePokemon()).thenReturn(active);
        doAnswer(inv -> when(player.getActivePokemon()).thenReturn(inv.getArgument(0))).when(player).setActivePokemon(any());
    }

    @Test
    void resolve_shouldSwitchActiveWithTargetBenchPokemon() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTargetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertSame(benchTarget, player.getActivePokemon());
        assertSame(active, bench.get(0));
    }

    @Test
    void resolve_shouldUpdateBenchAtCorrectIndex() {
        PokemonInPlay benchOther = new PokemonInPlay();
        benchOther.setInstanceId(UUID.randomUUID());
        bench.add(benchOther);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTargetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertSame(active, bench.get(0));
        assertSame(benchOther, bench.get(1));
    }

    @Test
    void resolve_withEmptyBench_shouldReturnEarly() {
        bench.clear();
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTargetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertSame(active, player.getActivePokemon());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withNullTargetId_shouldReturnEarly() {
        Map<String, Object> payload = new HashMap<>();

        resolver.resolve(ctx, player, card, payload);

        assertSame(active, player.getActivePokemon());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withTargetNotFoundOnBench_shouldReturnEarly() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        resolver.resolve(ctx, player, card, payload);

        assertSame(active, player.getActivePokemon());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void resolve_withActiveNull_shouldStillSwitch() {
        when(player.getActivePokemon()).thenReturn(null);
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTargetId.toString());

        resolver.resolve(ctx, player, card, payload);

        assertSame(benchTarget, player.getActivePokemon());
        assertNull(bench.get(0));
    }

    @Test
    void resolve_shouldPublishEvent() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTargetId.toString());

        resolver.resolve(ctx, player, card, payload);

        verify(ctx).addEvent(eventCaptor.capture());
        GameEvent event = eventCaptor.getValue();
        assertEquals(GameEventType.RETREAT_EXECUTED.name(), event.getType());
        assertEquals(activeId.toString(), event.getPayload().get("previousActiveInstanceId"));
        assertEquals(benchTargetId.toString(), event.getPayload().get("newActiveInstanceId"));
    }

    @Test
    void getType_shouldReturnSWITCH_POKEMON() {
        assertEquals(EffectType.SWITCH_POKEMON, resolver.getType());
    }
}
