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
}
