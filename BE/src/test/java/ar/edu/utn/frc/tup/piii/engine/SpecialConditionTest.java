package ar.edu.utn.frc.tup.piii.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpecialConditionTest {

    @Test
    void shouldContainAllExpectedValues() {
        assertNotNull(SpecialCondition.valueOf("ASLEEP"));
        assertNotNull(SpecialCondition.valueOf("BURNED"));
        assertNotNull(SpecialCondition.valueOf("CONFUSED"));
        assertNotNull(SpecialCondition.valueOf("PARALYZED"));
        assertNotNull(SpecialCondition.valueOf("POISONED"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(5, SpecialCondition.values().length);
    }

    @Test
    void shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> SpecialCondition.valueOf("INVALID"));
    }
}
