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
class SetupRemoveActiveHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;

    private SetupRemoveActiveHandler handler;
    private UUID playerId;
    private UUID activeInstanceId;
    private PokemonInPlay active;

    @BeforeEach
    void setUp() {
        handler = new SetupRemoveActiveHandler();
        playerId = UUID.randomUUID();
        activeInstanceId = UUID.randomUUID();

        active = new PokemonInPlay();
        active.setInstanceId(activeInstanceId);
        active.setCardDefinitionId("pkm-pikachu");
    }

    @Test
    void shouldRemoveActiveAndReturnToHand() {
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getHand()).thenReturn(new ArrayList<>());
        when(player.getPlayerId()).thenReturn(playerId);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        verify(player).setActivePokemon(null);
        verify(player).setSetupConfirmed(false);
        assertEquals(1, player.getHand().size());
        CardInstance returned = player.getHand().get(0);
        assertEquals(activeInstanceId, returned.getInstanceId());
        assertEquals("pkm-pikachu", returned.getCardDefinitionId());
    }

    @Test
    void shouldSetErrorWhenPlayerNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        verify(ctx).setError(argThat(e ->
                "PLAYER_NOT_FOUND".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenNoActivePokemon() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        verify(ctx).setError(argThat(e ->
                "NO_ACTIVE_POKEMON".equals(e.getCode())
        ));
    }
}
