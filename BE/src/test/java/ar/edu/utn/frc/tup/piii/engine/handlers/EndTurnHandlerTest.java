package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EndTurnHandlerTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private TurnManager turnManager;

    private EndTurnHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EndTurnHandler(turnManager);
    }

    @Test
    void shouldEndTurnAndStartNext() {
        GameAction action = new GameAction();
        action.setPlayerId(UUID.randomUUID());
        action.setPayload(new HashMap<>());

        handler.handle(ctx, action);

        verify(turnManager).endTurn(ctx);
        verify(turnManager).startTurn(ctx);
    }
}
