package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CardInstanceTest {

    @Test
    void shouldCreateWithNoArgsConstructor() {
        CardInstance card = new CardInstance();
        assertNull(card.getInstanceId());
        assertNull(card.getCardDefinitionId());
    }

    @Test
    void shouldCreateWithParameterizedConstructor() {
        UUID id = UUID.randomUUID();
        String defId = "base1-4";
        CardInstance card = new CardInstance(id, defId);

        assertEquals(id, card.getInstanceId());
        assertEquals(defId, card.getCardDefinitionId());
    }

    @Test
    void shouldSetAndGetInstanceId() {
        CardInstance card = new CardInstance();
        UUID id = UUID.randomUUID();
        card.setInstanceId(id);
        assertEquals(id, card.getInstanceId());
    }

    @Test
    void shouldSetAndGetCardDefinitionId() {
        CardInstance card = new CardInstance();
        card.setCardDefinitionId("swsh1-1");
        assertEquals("swsh1-1", card.getCardDefinitionId());
    }

    @Test
    void shouldHandleNullCardDefinitionId() {
        CardInstance card = new CardInstance(UUID.randomUUID(), null);
        assertNull(card.getCardDefinitionId());
    }

    @Test
    void shouldGenerateUniqueInstanceIds() {
        CardInstance card1 = new CardInstance(UUID.randomUUID(), "a");
        CardInstance card2 = new CardInstance(UUID.randomUUID(), "b");
        assertNotEquals(card1.getInstanceId(), card2.getInstanceId());
    }
}
