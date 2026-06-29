package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void publicDiscardCard_constructorAndGetters() {
        String instanceId = "inst-123";
        String cardId = "card-456";
        PublicDiscardCard card = new PublicDiscardCard(instanceId, cardId);

        assertEquals(instanceId, card.instanceId());
        assertEquals(cardId, card.cardId());
    }

    @Test
    void deckValidationError_allValuesExist() {
        assertNotNull(DeckValidationError.valueOf("DECK_SIZE_INVALID"));
        assertNotNull(DeckValidationError.valueOf("DUPLICATE_CARDS"));
        assertNotNull(DeckValidationError.valueOf("MISSING_BASIC_POKEMON"));
        assertNotNull(DeckValidationError.valueOf("MORE_THAN_4_COPIES"));
        assertNotNull(DeckValidationError.valueOf("INVALID_DECK_FORMAT"));
        assertNotNull(DeckValidationError.valueOf("ACE_SPEC_LIMIT_EXCEEDED"));
    }

    @Test
    void deckValidationError_hasExpectedCount() {
        assertEquals(6, DeckValidationError.values().length);
    }
}
