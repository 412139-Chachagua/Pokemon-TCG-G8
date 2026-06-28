package ar.edu.utn.frc.tup.piii.engine.ports;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;

public interface CardLookupPort {
    CardDefinition getCardById(String cardId);
}
