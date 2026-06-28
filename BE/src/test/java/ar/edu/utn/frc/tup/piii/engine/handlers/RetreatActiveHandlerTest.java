package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.model.TurnFlags;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RetreatActiveHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EnergyService energyService;

    private RetreatActiveHandler handler;
    private PokemonInPlay active;
    private PokemonInPlay benched;
    private TurnFlags flags;
    private UUID activeId;
    private UUID benchedId;
    private List<PokemonInPlay> bench;
    private List<CardInstance> discard;

    @BeforeEach
    void setUp() {
        handler = new RetreatActiveHandler();
        activeId = UUID.randomUUID();
        benchedId = UUID.randomUUID();

        flags = new TurnFlags();
        active = new PokemonInPlay();
        active.setInstanceId(activeId);
        active.setCardDefinitionId("pkm-active");
        active.setEnteredTurnNumber(1);
        active.setDamageCounters(0);
        active.setAttachedEnergies(new ArrayList<>());

        benched = new PokemonInPlay();
        benched.setInstanceId(benchedId);
        benched.setCardDefinitionId("pkm-bench");
        benched.setEnteredTurnNumber(2);
        benched.setDamageCounters(0);
        benched.setAttachedEnergies(new ArrayList<>());

        bench = new ArrayList<>(List.of(benched));
        discard = new ArrayList<>();
    }

    @Test
    void shouldReturnEarlyWhenAlreadyRetreated() {
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnFlags()).thenReturn(flags);
        flags.setHasRetreated(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());

        handler.handle(ctx, action);

        verify(player, never()).getActivePokemon();
    }

    @Test
    void shouldReturnEarlyWhenNoActivePokemon() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());

        handler.handle(ctx, action);

        verify(player, never()).getBench();
    }

    @Test
    void shouldReturnEarlyWhenCannotRetreatNextTurn() {
        active.setCannotRetreatNextTurn(true);

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getActivePokemon()).thenReturn(active);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());

        handler.handle(ctx, action);

        verify(player, never()).getBench();
    }

    @Test
    void shouldReturnEarlyWhenBenchIndexNull() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getActivePokemon()).thenReturn(active);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldReturnEarlyWhenBenchIndexOutOfBounds() {
        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(flags);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getBench()).thenReturn(bench);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());
        action.setPayload(Map.of("benchIndex", 5));

        handler.handle(ctx, action);

        verify(player, never()).setActivePokemon(any());
    }

    @Test
    void shouldSetEnteredTurnNumberOnRetreatedPokemon() {
        PokemonCardDefinition activeDef = new PokemonCardDefinition();
        activeDef.setRetreatCost(List.of());
        activeDef.setName("Pikachu");

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(any())).thenReturn(player);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        lenient().when(ctx.getEnergyService()).thenReturn(energyService);
        when(state.getTurnFlags()).thenReturn(flags);
        when(state.getTurnNumber()).thenReturn(5);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getBench()).thenReturn(bench);
        lenient().when(player.getDiscard()).thenReturn(discard);
        when(cardLookup.getCardById("pkm-active")).thenReturn(activeDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(UUID.randomUUID());
        action.setPayload(Map.of(
                "benchIndex", 0,
                "energyCardInstanceIdsToDiscard", List.of()
        ));

        handler.handle(ctx, action);

        assertEquals(5, active.getEnteredTurnNumber());
        assertSame(active, bench.get(0));
        assertEquals(1, bench.size());
    }
}
