package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EngineContext {
    private GameState state;
    private CardLookupPort cardLookup;
    private RandomizerPort randomizer;
    private StatePersisterPort persister;
    private EventPublisherPort eventPublisher;
    private final List<GameEvent> pendingEvents;
    private EnergyService energyService;
    private GameError error;

    public EngineContext(GameState state,
                         CardLookupPort cardLookup,
                         RandomizerPort randomizer,
                         StatePersisterPort persister,
                         EventPublisherPort eventPublisher) {
        this.state = state;
        this.cardLookup = cardLookup;
        this.randomizer = randomizer;
        this.persister = persister;
        this.eventPublisher = eventPublisher;
        this.pendingEvents = new ArrayList<>();
    }

    public GameError getError() { return error; }
    public void setError(GameError error) { this.error = error; }

    public void addEvent(GameEvent event) {
        pendingEvents.add(event);
    }

    public List<GameEvent> getPendingEvents() {
        return pendingEvents;
    }

    public PlayerState getPlayer(UUID playerId) {
        if (state == null || state.getPlayers() == null) return null;

        for (PlayerState p : state.getPlayers()) {
            if (p != null && p.getPlayerId().equals(playerId)) return p;
        }
        return null;
    }

    public PlayerState getOpponent(UUID playerId) {
        if (state == null || state.getPlayers() == null) return null;

        for (PlayerState p : state.getPlayers()) {
            if (p != null && !p.getPlayerId().equals(playerId)) return p;
        }
        return null;
    }

    public GameState getState() { return state; }
    public void setState(GameState state) { this.state = state; }

    public CardLookupPort getCardLookup() { return cardLookup; }
    public void setCardLookup(CardLookupPort cardLookup) { this.cardLookup = cardLookup; }

    public RandomizerPort getRandomizer() { return randomizer; }
    public void setRandomizer(RandomizerPort randomizer) { this.randomizer = randomizer; }

    public StatePersisterPort getPersister() { return persister; }
    public void setPersister(StatePersisterPort persister) { this.persister = persister; }

    public EventPublisherPort getEventPublisher() { return eventPublisher; }
    public void setEventPublisher(EventPublisherPort eventPublisher) { this.eventPublisher = eventPublisher; }

    public EnergyService getEnergyService() { return energyService; }
    public void setEnergyService(EnergyService energyService) { this.energyService = energyService; }
}
