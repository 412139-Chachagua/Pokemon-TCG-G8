package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PrivatePlayerStateTest {

    @Test
    void defaultConstructor_createsEmptyFields() {
        PrivatePlayerState pps = new PrivatePlayerState();

        assertNull(pps.getPlayerId());
        assertNull(pps.getHand());
        assertEquals(0, pps.getDeckCount());
        assertEquals(0, pps.getDiscardCount());
        assertNull(pps.getPrizes());
    }

    @Test
    void fullConstructor_setsFields() {
        UUID playerId = UUID.randomUUID();
        List<PrivatePlayerState.PrivateHandCard> hand = List.of(
                new PrivatePlayerState.PrivateHandCard("i1", "c1", "Pikachu", "Pokemon"));
        List<PrivatePlayerState.PrizeSlot> prizes = List.of(
                new PrivatePlayerState.PrizeSlot(1, true, "c1"));

        PrivatePlayerState pps = new PrivatePlayerState(playerId, hand, 30, 5, prizes);

        assertEquals(playerId, pps.getPlayerId());
        assertEquals(1, pps.getHand().size());
        assertEquals(30, pps.getDeckCount());
        assertEquals(5, pps.getDiscardCount());
        assertEquals(1, pps.getPrizes().size());
    }

    @Test
    void settersAndGetters() {
        PrivatePlayerState pps = new PrivatePlayerState();
        pps.setPendingMulliganDrawCount(3);
        pps.setDeckCount(20);

        assertEquals(3, pps.getPendingMulliganDrawCount());
        assertEquals(20, pps.getDeckCount());
    }

    @Test
    void privateHandCard_constructors() {
        PrivatePlayerState.PrivateHandCard card1 = new PrivatePlayerState.PrivateHandCard(
                "i1", "c1", "Pikachu", "Pokemon");
        PrivatePlayerState.PrivateHandCard card2 = new PrivatePlayerState.PrivateHandCard(
                "i2", "c2", "Charmander", "Pokemon", "DRAW_3");

        assertEquals("i1", card1.getInstanceId());
        assertNull(card1.getEffectCode());
        assertEquals("i2", card2.getInstanceId());
        assertEquals("DRAW_3", card2.getEffectCode());
    }

    @Test
    void prizeSlot_constructors() {
        PrivatePlayerState.PrizeSlot slot = new PrivatePlayerState.PrizeSlot(1, false, null);

        assertEquals(1, slot.getSlot());
        assertFalse(slot.isKnown());
        assertNull(slot.getCardId());
    }

    @Test
    void setDeckAndPlayerId() {
        PrivatePlayerState pps = new PrivatePlayerState();
        UUID pid = UUID.randomUUID();
        pps.setPlayerId(pid);
        pps.setHand(List.of(new PrivatePlayerState.PrivateHandCard("i1", "c1", "n", "s")));
        pps.setPrizes(List.of(new PrivatePlayerState.PrizeSlot(1, false, null)));
        pps.setDeck(List.of(new PrivatePlayerState.PrivateHandCard("i2", "c2", "n2", "s2")));
        pps.setDiscardCount(3);

        assertEquals(pid, pps.getPlayerId());
        assertEquals(1, pps.getHand().size());
        assertEquals(1, pps.getPrizes().size());
        assertEquals(1, pps.getDeck().size());
        assertEquals(3, pps.getDiscardCount());
    }

    @Test
    void prizeSlot_setters() {
        PrivatePlayerState.PrizeSlot slot = new PrivatePlayerState.PrizeSlot();
        slot.setSlot(2);
        slot.setKnown(true);
        slot.setCardId("card-1");

        assertEquals(2, slot.getSlot());
        assertTrue(slot.isKnown());
        assertEquals("card-1", slot.getCardId());
    }

    @Test
    void privateHandCard_setters() {
        PrivatePlayerState.PrivateHandCard card = new PrivatePlayerState.PrivateHandCard();
        card.setInstanceId("i1");
        card.setCardId("c1");
        card.setName("Pikachu");
        card.setSupertype("Pokemon");
        card.setEffectCode("DRAW_3");

        assertEquals("i1", card.getInstanceId());
        assertEquals("c1", card.getCardId());
        assertEquals("Pikachu", card.getName());
        assertEquals("Pokemon", card.getSupertype());
        assertEquals("DRAW_3", card.getEffectCode());
    }
}
