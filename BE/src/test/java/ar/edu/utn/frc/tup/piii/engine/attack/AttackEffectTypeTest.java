package ar.edu.utn.frc.tup.piii.engine.attack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AttackEffectTypeTest {

    @Test
    void shouldContainAllExpectedValues() {
        assertNotNull(AttackEffectType.valueOf("APPLY_SPECIAL_CONDITION"));
        assertNotNull(AttackEffectType.valueOf("DISCARD_ENERGY"));
        assertNotNull(AttackEffectType.valueOf("DAMAGE_BENCH"));
        assertNotNull(AttackEffectType.valueOf("HEAL_USER"));
        assertNotNull(AttackEffectType.valueOf("DRAW_CARDS"));
        assertNotNull(AttackEffectType.valueOf("SWITCH_AFTER_DAMAGE"));
        assertNotNull(AttackEffectType.valueOf("COIN_FLIP_BEFORE_DAMAGE"));
        assertNotNull(AttackEffectType.valueOf("COIN_FLIP_AFTER_DAMAGE"));
        assertNotNull(AttackEffectType.valueOf("RECOIL"));
        assertNotNull(AttackEffectType.valueOf("SEARCH_DECK"));
        assertNotNull(AttackEffectType.valueOf("ATTACH_ENERGY"));
        assertNotNull(AttackEffectType.valueOf("MOVE_ENERGY"));
        assertNotNull(AttackEffectType.valueOf("DAMAGE_PREVENTION"));
        assertNotNull(AttackEffectType.valueOf("CANNOT_ATTACK_NEXT_TURN"));
        assertNotNull(AttackEffectType.valueOf("SUPPORTER_LOCK"));
        assertNotNull(AttackEffectType.valueOf("OPPONENT_DISCARD_HAND"));
        assertNotNull(AttackEffectType.valueOf("NEXT_TURN_DAMAGE_BONUS"));
        assertNotNull(AttackEffectType.valueOf("RETREAT_LOCK"));
        assertNotNull(AttackEffectType.valueOf("DAMAGE_REDUCTION"));
        assertNotNull(AttackEffectType.valueOf("DISCARD_OPPONENT_DECK"));
        assertNotNull(AttackEffectType.valueOf("SEARCH_DISCARD"));
        assertNotNull(AttackEffectType.valueOf("RECYCLE_FROM_DISCARD"));
        assertNotNull(AttackEffectType.valueOf("OPPONENT_SHUFFLE_DRAW"));
        assertNotNull(AttackEffectType.valueOf("DAMAGE_ALL_BENCH"));
        assertNotNull(AttackEffectType.valueOf("DEFENDER_CANNOT_ATTACK"));
        assertNotNull(AttackEffectType.valueOf("ABILITY_SUPPRESSION"));
        assertNotNull(AttackEffectType.valueOf("DISCARD_TOOL"));
        assertNotNull(AttackEffectType.valueOf("REORDER_DECK"));
        assertNotNull(AttackEffectType.valueOf("PEEK_OPPONENT_DECK"));
        assertNotNull(AttackEffectType.valueOf("OPPONENT_RANDOM_DISCARD"));
        assertNotNull(AttackEffectType.valueOf("SET_HP"));
        assertNotNull(AttackEffectType.valueOf("MENTAL_PANIC"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(32, AttackEffectType.values().length);
    }

    @Test
    void shouldThrowForInvalidValue() {
        assertThrows(IllegalArgumentException.class,
                () -> AttackEffectType.valueOf("INVALID_EFFECT"));
    }
}
