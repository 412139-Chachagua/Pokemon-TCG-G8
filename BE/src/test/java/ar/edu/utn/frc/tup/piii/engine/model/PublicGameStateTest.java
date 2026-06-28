package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PublicGameStateTest {

    @Test
    void defaultConstructor() {
        PublicGameState pgs = new PublicGameState();

        assertNull(pgs.getMatchId());
        assertNull(pgs.getStatus());
        assertNull(pgs.getPlayers());
    }

    @Test
    void fullConstructor() {
        UUID matchId = UUID.randomUUID();
        UUID currentId = UUID.randomUUID();
        UUID firstId = UUID.randomUUID();
        PublicGameState.PublicPlayerState[] players = new PublicGameState.PublicPlayerState[0];

        PublicGameState pgs = new PublicGameState(matchId, "ACTIVE", "MAIN", 3, currentId, firstId, players);

        assertEquals(matchId, pgs.getMatchId());
        assertEquals("ACTIVE", pgs.getStatus());
        assertEquals("MAIN", pgs.getPhase());
        assertEquals(3, pgs.getTurnNumber());
        assertEquals(currentId, pgs.getCurrentPlayerId());
        assertEquals(firstId, pgs.getFirstPlayerId());
        assertEquals(0, pgs.getPlayers().length);
    }

    @Test
    void settersAndGetters() {
        PublicGameState pgs = new PublicGameState();
        pgs.setMulliganDrawPending(true);
        pgs.setPendingKOReplacement(true);

        assertTrue(pgs.isMulliganDrawPending());
        assertTrue(pgs.isPendingKOReplacement());
    }

    @Test
    void publicPlayerState_defaultConstructor() {
        PublicGameState.PublicPlayerState pps = new PublicGameState.PublicPlayerState();

        assertNull(pps.getPlayerId());
        assertNull(pps.getSide());
    }

    @Test
    void publicPlayerState_fullConstructor() {
        UUID playerId = UUID.randomUUID();
        PublicGameState.PublicPokemonSlot active = new PublicGameState.PublicPokemonSlot(
                "i1", "c1", 30, new String[]{"BURNED"}, new String[]{"ELECTRIC"}, false);
        PublicGameState.PublicPokemonSlot[] bench = new PublicGameState.PublicPokemonSlot[0];
        String[] prizes = new String[]{"FACE_DOWN", "FACE_DOWN"};

        PublicGameState.PublicPlayerState pps = new PublicGameState.PublicPlayerState(
                playerId, "PLAYER_ONE", active, bench, prizes, true);

        assertEquals(playerId, pps.getPlayerId());
        assertEquals("PLAYER_ONE", pps.getSide());
        assertNotNull(pps.getActivePokemon());
        assertEquals(2, pps.getPrizes().length);
        assertTrue(pps.isSetupConfirmed());
    }

    @Test
    void setters_stadiumAndMulligan() {
        PublicGameState pgs = new PublicGameState();
        pgs.setStadiumCardDefinitionId("stadium-1");
        pgs.setWinnerPlayerId(UUID.randomUUID());
        pgs.setKnockedOutPlayerId(UUID.randomUUID().toString());

        assertEquals("stadium-1", pgs.getStadiumCardDefinitionId());
        assertNotNull(pgs.getWinnerPlayerId());
        assertNotNull(pgs.getKnockedOutPlayerId());
    }

    @Test
    void publicPokemonSlot_constructors() {
        PublicGameState.PublicPokemonSlot slot1 = new PublicGameState.PublicPokemonSlot(
                "i1", "c1", 50, new String[]{"CONFUSED"}, new String[]{"WATER", "FIGHTING"}, true);

        assertEquals("i1", slot1.getInstanceId());
        assertEquals("c1", slot1.getCardId());
        assertEquals(50, slot1.getDamageCounters());
        assertTrue(slot1.isEvolvedThisTurn());
        assertEquals(2, slot1.getAttachedCards().length);

        PublicGameState.PublicPokemonSlot slot2 = new PublicGameState.PublicPokemonSlot(
                "i2", "c2", 0, new String[0], new String[0], false,
                0, new String[]{"e-1"}, "tool-1", "tool-def-1");

        assertEquals("e-1", slot2.getAttachedEnergyInstanceIds()[0]);
        assertEquals("tool-1", slot2.getAttachedToolCardInstanceId());
        assertEquals("tool-def-1", slot2.getAttachedToolCardDefinitionId());
    }
}
