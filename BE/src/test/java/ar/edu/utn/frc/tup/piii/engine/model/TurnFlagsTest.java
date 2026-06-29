package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TurnFlagsTest {

    @Test
    void defaults_areFalse() {
        TurnFlags flags = new TurnFlags();

        assertFalse(flags.hasDrawnForTurn());
        assertFalse(flags.hasAttachedEnergy());
        assertFalse(flags.hasRetreated());
        assertFalse(flags.hasPlayedSupporter());
        assertFalse(flags.hasPlayedStadium());
        assertFalse(flags.hasAttacked());
        assertNull(flags.getDamageModifiers());
    }

    @Test
    void settersAndGetters_roundTrip() {
        TurnFlags flags = new TurnFlags();

        flags.setHasDrawnForTurn(true);
        flags.setHasAttachedEnergy(true);
        flags.setHasRetreated(true);
        flags.setHasPlayedSupporter(true);
        flags.setHasPlayedStadium(true);
        flags.setHasAttacked(true);

        assertTrue(flags.hasDrawnForTurn());
        assertTrue(flags.hasAttachedEnergy());
        assertTrue(flags.hasRetreated());
        assertTrue(flags.hasPlayedSupporter());
        assertTrue(flags.hasPlayedStadium());
        assertTrue(flags.hasAttacked());
    }

    @Test
    void damageModifiers() {
        TurnFlags flags = new TurnFlags();
        Map<String, Object> modifiers = Map.of("mod", 10);

        flags.setDamageModifiers(modifiers);

        assertEquals(modifiers, flags.getDamageModifiers());
    }

    @Test
    void shouldStartHasAttachedEnergyFalse_thenSetTrue() {
        TurnFlags flags = new TurnFlags();

        assertFalse(flags.hasAttachedEnergy());

        flags.setHasAttachedEnergy(true);

        assertTrue(flags.hasAttachedEnergy());
    }

    @Test
    void shouldSetHasPlayedSupporterIndependently() {
        TurnFlags flags = new TurnFlags();

        flags.setHasPlayedSupporter(true);

        assertTrue(flags.hasPlayedSupporter());
        assertFalse(flags.hasPlayedStadium());
        assertFalse(flags.hasAttacked());
    }

    @Test
    void shouldSetHasPlayedStadiumIndependently() {
        TurnFlags flags = new TurnFlags();

        flags.setHasPlayedStadium(true);

        assertTrue(flags.hasPlayedStadium());
        assertFalse(flags.hasPlayedSupporter());
        assertFalse(flags.hasRetreated());
    }

    @Test
    void shouldHandleMultipleFlagsActiveSimultaneously() {
        TurnFlags flags = new TurnFlags();

        flags.setHasDrawnForTurn(true);
        flags.setHasAttachedEnergy(true);
        flags.setHasAttacked(true);

        assertTrue(flags.hasDrawnForTurn());
        assertTrue(flags.hasAttachedEnergy());
        assertTrue(flags.hasAttacked());
        assertFalse(flags.hasRetreated());
        assertFalse(flags.hasPlayedSupporter());
        assertFalse(flags.hasPlayedStadium());
    }

    @Test
    void shouldSetDamageModifiersWithMutableMap() {
        TurnFlags flags = new TurnFlags();
        Map<String, Object> modifiers = new java.util.HashMap<>();
        modifiers.put("key1", 10);
        modifiers.put("key2", 20);

        flags.setDamageModifiers(modifiers);

        assertEquals(2, flags.getDamageModifiers().size());
        assertEquals(10, flags.getDamageModifiers().get("key1"));
    }
}
