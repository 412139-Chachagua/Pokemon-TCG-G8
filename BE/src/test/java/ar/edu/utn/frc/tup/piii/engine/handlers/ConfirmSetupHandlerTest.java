package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmSetupHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private PlayerState otherPlayer;
    @Mock
    private RandomizerPort randomizer;

    private ConfirmSetupHandler handler;
    private UUID playerId;
    private UUID otherPlayerId;

    @BeforeEach
    void setUp() {
        handler = new ConfirmSetupHandler();
        playerId = UUID.randomUUID();
        otherPlayerId = UUID.randomUUID();
    }

    @Test
    void shouldConfirmSetupForPlayer() {
        // Use real PlayerState objects so state mutations are visible
        PlayerState realPlayer = new PlayerState();
        realPlayer.setPlayerId(playerId);
        realPlayer.setActivePokemon(new PokemonInPlay());
        realPlayer.setSetupConfirmed(false);

        PlayerState realOther = new PlayerState();
        realOther.setPlayerId(otherPlayerId);

        when(ctx.getPlayer(playerId)).thenReturn(realPlayer);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getPlayers()).thenReturn(new PlayerState[]{realPlayer, realOther});

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        assertTrue(realPlayer.isSetupConfirmed());
        verify(ctx, times(1)).addEvent(argThat(e ->
                "SETUP_CONFIRMED".equals(e.getType())
        ));
    }

    @Test
    void shouldTransitionToActiveWhenBothConfirm() {
        // Use real PlayerState objects so state mutations are visible
        PlayerState realPlayer = new PlayerState();
        realPlayer.setPlayerId(playerId);
        realPlayer.setActivePokemon(new PokemonInPlay());
        realPlayer.setSetupConfirmed(false);

        PlayerState realOther = new PlayerState();
        realOther.setPlayerId(otherPlayerId);
        realOther.setSetupConfirmed(true);

        when(ctx.getPlayer(playerId)).thenReturn(realPlayer);
        when(ctx.getState()).thenReturn(state);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getPlayers()).thenReturn(new PlayerState[]{realPlayer, realOther});

        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.hasPendingInitialMulligan()).thenReturn(false);
        when(state.isMulliganDrawPending()).thenReturn(false);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(0);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        assertTrue(realPlayer.isSetupConfirmed());
        verify(state).setFirstPlayerId(playerId);
        verify(state).setCurrentPlayerId(playerId);
        verify(state).setStatus(MatchStatus.ACTIVE);
        verify(state).setPhase(TurnPhase.MAIN);
        verify(state).setTurnNumber(1);
    }

    @Test
    void shouldSetErrorWhenPlayerNotFound() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);

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

        handler.handle(ctx, action);

        verify(ctx).setError(argThat(e ->
                "NO_ACTIVE_POKEMON".equals(e.getCode())
        ));
    }

    @Test
    void shouldSetErrorWhenAlreadyConfirmed() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay());
        when(player.isSetupConfirmed()).thenReturn(true);

        GameAction action = new GameAction();
        action.setPlayerId(playerId);

        handler.handle(ctx, action);

        verify(ctx).setError(argThat(e ->
                "ALREADY_CONFIRMED".equals(e.getCode())
        ));
    }

    @Test
    void tryTransitionToActive_shouldReturnFalseWhenStatusNotSetup() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.ACTIVE);

        boolean result = ConfirmSetupHandler.tryTransitionToActive(ctx);

        assertFalse(result);
    }

    @Test
    void tryTransitionToActive_shouldReturnFalseWhenPendingMulligan() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.hasPendingInitialMulligan()).thenReturn(true);

        boolean result = ConfirmSetupHandler.tryTransitionToActive(ctx);

        assertFalse(result);
    }

    @Test
    void tryTransitionToActive_shouldReturnFalseWhenMulliganDrawPending() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.hasPendingInitialMulligan()).thenReturn(false);
        when(state.isMulliganDrawPending()).thenReturn(true);

        boolean result = ConfirmSetupHandler.tryTransitionToActive(ctx);

        assertFalse(result);
    }

    @Test
    void tryTransitionToActive_shouldReturnFalseWhenNotAllConfirmed() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.hasPendingInitialMulligan()).thenReturn(false);
        when(state.isMulliganDrawPending()).thenReturn(false);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, otherPlayer});
        when(player.isSetupConfirmed()).thenReturn(true);
        when(otherPlayer.isSetupConfirmed()).thenReturn(false);

        boolean result = ConfirmSetupHandler.tryTransitionToActive(ctx);

        assertFalse(result);
    }

    @Test
    void tryTransitionToActive_shouldSucceedWhenAllConditionsMet() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.hasPendingInitialMulligan()).thenReturn(false);
        when(state.isMulliganDrawPending()).thenReturn(false);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, otherPlayer});
        when(player.isSetupConfirmed()).thenReturn(true);
        when(otherPlayer.isSetupConfirmed()).thenReturn(true);
        when(otherPlayer.getPlayerId()).thenReturn(otherPlayerId);
        when(ctx.getRandomizer()).thenReturn(randomizer);
        when(randomizer.nextInt(2)).thenReturn(1);

        boolean result = ConfirmSetupHandler.tryTransitionToActive(ctx);

        assertTrue(result);
        verify(state).setFirstPlayerId(otherPlayerId);
        verify(state).setStatus(MatchStatus.ACTIVE);
    }
}
