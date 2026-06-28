package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EngineContextTest {

    @Mock private CardLookupPort cardLookup;
    @Mock private RandomizerPort randomizer;
    @Mock private StatePersisterPort persister;
    @Mock private EventPublisherPort eventPublisher;

    private EngineContext ctx;
    private UUID p1Id;
    private UUID p2Id;

    @BeforeEach
    void setUp() {
        GameState state = new GameState();
        p1Id = UUID.randomUUID();
        p2Id = UUID.randomUUID();

        PlayerState p1 = new PlayerState();
        p1.setPlayerId(p1Id);
        PlayerState p2 = new PlayerState();
        p2.setPlayerId(p2Id);

        state.setPlayers(new PlayerState[]{p1, p2});

        ctx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
    }

    @Test
    void getPlayer_existingId_returnsPlayer() {
        PlayerState result = ctx.getPlayer(p1Id);
        assertNotNull(result);
        assertEquals(p1Id, result.getPlayerId());
    }

    @Test
    void getPlayer_nonExistingId_returnsNull() {
        assertNull(ctx.getPlayer(UUID.randomUUID()));
    }

    @Test
    void getPlayer_nullState_returnsNull() {
        EngineContext nullCtx = new EngineContext(null, cardLookup, randomizer, persister, eventPublisher);
        assertNull(nullCtx.getPlayer(p1Id));
    }

    @Test
    void getPlayer_nullPlayers_returnsNull() {
        GameState state = new GameState();
        state.setPlayers(null);
        EngineContext nullCtx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);
        assertNull(nullCtx.getPlayer(p1Id));
    }

    @Test
    void getOpponent_existingId_returnsOtherPlayer() {
        PlayerState result = ctx.getOpponent(p1Id);
        assertNotNull(result);
        assertEquals(p2Id, result.getPlayerId());
    }

    @Test
    void getOpponent_nonExistingId_returnsFirstPlayer() {
        // getOpponent returns the first player whose ID doesn't match
        PlayerState result = ctx.getOpponent(UUID.randomUUID());
        assertNotNull(result);
        assertEquals(p1Id, result.getPlayerId());
    }

    @Test
    void getOpponent_nullState_returnsNull() {
        EngineContext nullCtx = new EngineContext(null, cardLookup, randomizer, persister, eventPublisher);
        assertNull(nullCtx.getOpponent(p1Id));
    }

    @Test
    void getPlayer_withNullPlayerInArray_skipsNull() {
        GameState state = new GameState();
        state.setPlayers(new PlayerState[]{null, null});
        EngineContext nullCtx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);

        assertNull(nullCtx.getPlayer(p1Id));
    }

    @Test
    void getOpponent_withNullPlayerInArray_skipsNull() {
        GameState state = new GameState();
        state.setPlayers(new PlayerState[]{null, null});
        EngineContext nullCtx = new EngineContext(state, cardLookup, randomizer, persister, eventPublisher);

        assertNull(nullCtx.getOpponent(p1Id));
    }

    @Test
    void addEvent_addsToPendingEvents() {
        GameEvent event = new GameEvent("TEST_EVENT", UUID.randomUUID(), 1, null, "test", null);
        ctx.addEvent(event);
        ctx.addEvent(event);

        assertEquals(2, ctx.getPendingEvents().size());
    }

    @Test
    void setError_getError_roundTrip() {
        GameError error = new GameError("TEST_ERROR", "Test message");
        ctx.setError(error);

        assertSame(error, ctx.getError());
    }
}
