package ar.edu.utn.frc.tup.piii.engine.ports;

import ar.edu.utn.frc.tup.piii.domain.decks.Deck;

import java.util.UUID;

public interface DeckLoadPort {
    Deck loadDeck(UUID deckId);
}
