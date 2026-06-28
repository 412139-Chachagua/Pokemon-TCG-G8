package ar.edu.utn.frc.tup.piii.engine.trainer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EffectTypeTest {

    @Test
    void shouldContainDRAW_CARDS() {
        assertNotNull(EffectType.valueOf("DRAW_CARDS"));
    }

    @Test
    void shouldContainHEAL() {
        assertNotNull(EffectType.valueOf("HEAL"));
    }

    @Test
    void shouldContainSEARCH_BASIC_POKEMON() {
        assertNotNull(EffectType.valueOf("SEARCH_BASIC_POKEMON"));
    }

    @Test
    void shouldContainSEARCH_ENERGY() {
        assertNotNull(EffectType.valueOf("SEARCH_ENERGY"));
    }

    @Test
    void shouldContainEVOLVE_SEARCH() {
        assertNotNull(EffectType.valueOf("EVOLVE_SEARCH"));
    }

    @Test
    void shouldContainDISCARD_AND_DRAW() {
        assertNotNull(EffectType.valueOf("DISCARD_AND_DRAW"));
    }

    @Test
    void shouldContainSWITCH_POKEMON() {
        assertNotNull(EffectType.valueOf("SWITCH_POKEMON"));
    }

    @Test
    void shouldContainSHUFFLE_HAND_INTO_DECK() {
        assertNotNull(EffectType.valueOf("SHUFFLE_HAND_INTO_DECK"));
    }

    @Test
    void shouldContainATTACH_EXTRA_ENERGY() {
        assertNotNull(EffectType.valueOf("ATTACH_EXTRA_ENERGY"));
    }

    @Test
    void shouldContainDAMAGE_MODIFY() {
        assertNotNull(EffectType.valueOf("DAMAGE_MODIFY"));
    }

    @Test
    void shouldContainCONDITION_REMOVE() {
        assertNotNull(EffectType.valueOf("CONDITION_REMOVE"));
    }

    @Test
    void shouldContainREVIVE() {
        assertNotNull(EffectType.valueOf("REVIVE"));
    }

    @Test
    void shouldContainTOOL_ATTACH() {
        assertNotNull(EffectType.valueOf("TOOL_ATTACH"));
    }

    @Test
    void shouldContainSTADIUM_PLAY() {
        assertNotNull(EffectType.valueOf("STADIUM_PLAY"));
    }

    @Test
    void shouldContainEVOLVE_DIRECT() {
        assertNotNull(EffectType.valueOf("EVOLVE_DIRECT"));
    }

    @Test
    void shouldContainLOOK_TOP_SEARCH() {
        assertNotNull(EffectType.valueOf("LOOK_TOP_SEARCH"));
    }

    @Test
    void shouldContainREVIVE_TO_DECK() {
        assertNotNull(EffectType.valueOf("REVIVE_TO_DECK"));
    }

    @Test
    void shouldContainSEARCH_ENERGY_TO_HAND() {
        assertNotNull(EffectType.valueOf("SEARCH_ENERGY_TO_HAND"));
    }

    @Test
    void shouldContainOPPONENT_SHUFFLE_HAND_DRAW() {
        assertNotNull(EffectType.valueOf("OPPONENT_SHUFFLE_HAND_DRAW"));
    }

    @Test
    void shouldContainCOIN_FLIP_DRAW() {
        assertNotNull(EffectType.valueOf("COIN_FLIP_DRAW"));
    }

    @Test
    void shouldContainHEAL_WITH_DISCARD() {
        assertNotNull(EffectType.valueOf("HEAL_WITH_DISCARD"));
    }

    @Test
    void shouldContainRETURN_POKEMON_TO_DECK() {
        assertNotNull(EffectType.valueOf("RETURN_POKEMON_TO_DECK"));
    }

    @Test
    void shouldContainDISCARD_OPPONENT_ENERGY() {
        assertNotNull(EffectType.valueOf("DISCARD_OPPONENT_ENERGY"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(23, EffectType.values().length);
    }
}
