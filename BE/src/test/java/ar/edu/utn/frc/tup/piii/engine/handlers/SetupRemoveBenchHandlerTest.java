package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SetupRemoveBenchHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;

    private SetupRemoveBenchHandler handler;
    private UUID playerId;
    private UUID benchInstanceId;
    private PokemonInPlay benched;

    @BeforeEach
    void setUp() {
        handler = new SetupRemoveBenchHandler();
        playerId = UUID.randomUUID();
        benchInstanceId = UUID.randomUUID();

        benched = new PokemonInPlay();
        benched.setInstanceId(benchInstanceId);
        benched.setCardDefinitionId("pkm-charmander");
    }

    private GameAction createAction(String cardInstanceIdStr) {
        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        if (cardInstanceIdStr != null) {
            payload.put("cardInstanceId", cardInstanceIdStr);
        }
        action.setPayload(payload);
        return action;
    }

    @Test
    void shouldRemoveBenchPokemonAndReturnToHand() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>(List.of(benched)));
        when(player.getHand()).thenReturn(new ArrayList<>());
        when(player.getPlayerId()).thenReturn(playerId);

        handler.handle(ctx, createAction(benchInstanceId.toString()));

        assertTrue(player.getBench().isEmpty());
        verify(player).setSetupConfirmed(false);
        assertEquals(1, player.getHand().size());
        CardInstance returned = player.getHand().get(0);
        assertEquals(benchInstanceId, returned.getInstanceId());
        assertEquals("pkm-charmander", returned.getCardDefinitionId());
    }

    @Test
    void shouldSetErrorWhenCardInstanceIdIsNull() {
        handler.handle(ctx, createAction(null));

        verify(ctx).setError(argThat(e ->
                "INVALID_PAYLOAD".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenPlayerNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        handler.handle(ctx, createAction(benchInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "PLAYER_NOT_FOUND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenPokemonNotOnBench() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(new ArrayList<>());

        handler.handle(ctx, createAction(benchInstanceId.toString()));

        verify(ctx).setError(argThat(e ->
                "POKEMON_NOT_ON_BENCH".equals(e.getCode())
        ));
    }
}
