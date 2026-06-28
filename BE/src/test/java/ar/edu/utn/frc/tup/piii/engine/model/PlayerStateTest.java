package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.PlayerSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerStateTest {

    private PlayerState playerState;
    private UUID instanceId;

    @BeforeEach
    void setUp() {
        playerState = new PlayerState();
        instanceId = UUID.randomUUID();
    }

    @Test
    void setDiscard_null_createsEmptyList() {
        playerState.setDiscard(null);
        assertTrue(playerState.getDiscard().isEmpty());
    }

    @Test
    void pushToDiscard_addsCard() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        assertEquals(1, playerState.getDiscard().size());
        assertTrue(playerState.discardContains(instanceId));
    }

    @Test
    void pushToDiscard_nullCard_ignored() {
        playerState.pushToDiscard(null);
        assertTrue(playerState.getDiscard().isEmpty());
    }

    @Test
    void pushToDiscard_duplicateInstanceId_ignored() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        CardInstance duplicate = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);
        playerState.pushToDiscard(duplicate);

        assertEquals(1, playerState.getDiscard().size());
    }

    @Test
    void pushManyToDiscard_nullCollection_ignored() {
        playerState.pushManyToDiscard(null);
        assertTrue(playerState.getDiscard().isEmpty());
    }

    @Test
    void pushManyToDiscard_emptyCollection_ignored() {
        playerState.pushManyToDiscard(List.of());
        assertTrue(playerState.getDiscard().isEmpty());
    }

    @Test
    void pushManyToDiscard_multipleCards_addsAll() {
        CardInstance c1 = new CardInstance(UUID.randomUUID(), "card-1");
        CardInstance c2 = new CardInstance(UUID.randomUUID(), "card-2");
        CardInstance c3 = new CardInstance(UUID.randomUUID(), "card-3");

        playerState.pushManyToDiscard(List.of(c1, c2, c3));

        assertEquals(3, playerState.getDiscard().size());
    }

    @Test
    void peekTopDiscard_empty_returnsNull() {
        assertNull(playerState.peekTopDiscard());
    }

    @Test
    void peekTopDiscard_nonEmpty_returnsLast() {
        CardInstance c1 = new CardInstance(UUID.randomUUID(), "card-1");
        CardInstance c2 = new CardInstance(UUID.randomUUID(), "card-2");
        playerState.pushToDiscard(c1);
        playerState.pushToDiscard(c2);

        CardInstance top = playerState.peekTopDiscard();

        assertEquals(c2.getInstanceId(), top.getInstanceId());
        assertEquals(2, playerState.getDiscard().size()); // doesn't remove
    }

    @Test
    void popTopDiscard_empty_returnsNull() {
        assertNull(playerState.popTopDiscard());
    }

    @Test
    void popTopDiscard_nonEmpty_removesAndReturns() {
        CardInstance c1 = new CardInstance(UUID.randomUUID(), "card-1");
        CardInstance c2 = new CardInstance(UUID.randomUUID(), "card-2");
        playerState.pushToDiscard(c1);
        playerState.pushToDiscard(c2);

        CardInstance popped = playerState.popTopDiscard();

        assertEquals(c2.getInstanceId(), popped.getInstanceId());
        assertEquals(1, playerState.getDiscard().size());
    }

    @Test
    void discardContains_nullInstanceId_returnsFalse() {
        assertFalse(playerState.discardContains(null));
    }

    @Test
    void discardContains_notFound_returnsFalse() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        assertFalse(playerState.discardContains(UUID.randomUUID()));
    }

    @Test
    void findInDiscard_nullInstanceId_returnsEmpty() {
        assertEquals(Optional.empty(), playerState.findInDiscard(null));
    }

    @Test
    void findInDiscard_found_returnsCard() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        Optional<CardInstance> found = playerState.findInDiscard(instanceId);

        assertTrue(found.isPresent());
        assertEquals(instanceId, found.get().getInstanceId());
    }

    @Test
    void removeFromDiscard_nullInstanceId_returnsFalse() {
        assertFalse(playerState.removeFromDiscard(null));
    }

    @Test
    void removeFromDiscard_notFound_returnsFalse() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        assertFalse(playerState.removeFromDiscard(UUID.randomUUID()));
        assertEquals(1, playerState.getDiscard().size());
    }

    @Test
    void removeFromDiscard_found_removesAndReturnsTrue() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        boolean removed = playerState.removeFromDiscard(instanceId);

        assertTrue(removed);
        assertTrue(playerState.getDiscard().isEmpty());
    }

    @Test
    void findInDiscard_notFound_returnsEmpty() {
        CardInstance card = new CardInstance(instanceId, "card-1");
        playerState.pushToDiscard(card);

        assertEquals(Optional.empty(), playerState.findInDiscard(UUID.randomUUID()));
    }

    @Test
    void setters_simpleFields() {
        playerState.setSide(PlayerSide.PLAYER_ONE);
        playerState.setMulliganCount(3);
        playerState.setSetupConfirmed(true);
        playerState.setInitialMulliganResolved(true);
        playerState.setCannotPlaySupportersNextTurn(true);
        playerState.setMulliganRevealedCards(new ArrayList<>());

        assertEquals(PlayerSide.PLAYER_ONE, playerState.getSide());
        assertEquals(3, playerState.getMulliganCount());
        assertTrue(playerState.isSetupConfirmed());
        assertTrue(playerState.isInitialMulliganResolved());
        assertTrue(playerState.isCannotPlaySupportersNextTurn());
        assertNotNull(playerState.getMulliganRevealedCards());
    }

    @Test
    void addMulliganReveal_firstCall_initializesList() {
        assertNull(playerState.getMulliganRevealedCards());

        playerState.addMulliganReveal(List.of("card-1", "card-2"));

        assertNotNull(playerState.getMulliganRevealedCards());
        assertEquals(1, playerState.getMulliganRevealedCards().size());
    }

    @Test
    void addMulliganReveal_subsequentCall_appendsToList() {
        playerState.addMulliganReveal(List.of("card-1"));
        playerState.addMulliganReveal(List.of("card-2"));

        assertEquals(2, playerState.getMulliganRevealedCards().size());
    }
}
