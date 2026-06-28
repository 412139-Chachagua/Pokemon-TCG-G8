package ar.edu.utn.frc.tup.piii.engine.ports;

import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;

import java.util.List;
import java.util.UUID;

public interface EventPublisherPort {
    void publishEvents(UUID matchId, List<GameEvent> events);
}
