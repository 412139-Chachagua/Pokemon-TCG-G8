package ar.edu.utn.frc.tup.piii.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorCodeTest {

    @Test
    void shouldContainAllErrorCodes() {
        assertNotNull(ErrorCode.valueOf("NOT_YOUR_TURN"));
        assertNotNull(ErrorCode.valueOf("WRONG_PHASE"));
        assertNotNull(ErrorCode.valueOf("MATCH_NOT_ACTIVE"));
        assertNotNull(ErrorCode.valueOf("ENERGY_ALREADY_ATTACHED"));
        assertNotNull(ErrorCode.valueOf("BENCH_FULL"));
        assertNotNull(ErrorCode.valueOf("INSUFFICIENT_ENERGY"));
        assertNotNull(ErrorCode.valueOf("CANNOT_ATTACK_FIRST_TURN"));
        assertNotNull(ErrorCode.valueOf("POKEMON_ASLEEP"));
        assertNotNull(ErrorCode.valueOf("POKEMON_PARALYZED"));
        assertNotNull(ErrorCode.valueOf("RETREAT_ALREADY_USED"));
        assertNotNull(ErrorCode.valueOf("SUPPORTER_ALREADY_PLAYED"));
        assertNotNull(ErrorCode.valueOf("EVOLVE_NOT_ALLOWED"));
        assertNotNull(ErrorCode.valueOf("CARD_NOT_IN_HAND"));
        assertNotNull(ErrorCode.valueOf("INVALID_TARGET"));
        assertNotNull(ErrorCode.valueOf("KNOCKOUT_REPLACEMENT_REQUIRED"));
        assertNotNull(ErrorCode.valueOf("TOOL_ALREADY_EQUIPPED"));
        assertNotNull(ErrorCode.valueOf("UNKNOWN_EFFECT_CODE"));
        assertNotNull(ErrorCode.valueOf("MISSING_TARGET"));
        assertNotNull(ErrorCode.valueOf("ABILITY_NOT_FOUND"));
        assertNotNull(ErrorCode.valueOf("ABILITY_ALREADY_USED"));
        assertNotNull(ErrorCode.valueOf("POKEMON_CANNOT_USE_ABILITY"));
        assertNotNull(ErrorCode.valueOf("STADIUM_ALREADY_PLAYED"));
    }

    @Test
    void shouldHaveCorrectCount() {
        assertEquals(22, ErrorCode.values().length);
    }

    @Test
    void shouldContainAllMatchStatuses() {
        assertNotNull(MatchStatus.valueOf("WAITING"));
        assertNotNull(MatchStatus.valueOf("SETUP"));
        assertNotNull(MatchStatus.valueOf("ACTIVE"));
        assertNotNull(MatchStatus.valueOf("FINISHED"));
    }

    @Test
    void shouldHaveCorrectMatchStatusCount() {
        assertEquals(4, MatchStatus.values().length);
    }

    @Test
    void shouldContainAllPlayerSides() {
        assertNotNull(PlayerSide.valueOf("PLAYER_ONE"));
        assertNotNull(PlayerSide.valueOf("PLAYER_TWO"));
    }

    @Test
    void shouldHaveCorrectPlayerSideCount() {
        assertEquals(2, PlayerSide.values().length);
    }

    @Test
    void shouldContainAllSpecialConditions() {
        assertNotNull(SpecialCondition.valueOf("ASLEEP"));
        assertNotNull(SpecialCondition.valueOf("BURNED"));
        assertNotNull(SpecialCondition.valueOf("CONFUSED"));
        assertNotNull(SpecialCondition.valueOf("PARALYZED"));
        assertNotNull(SpecialCondition.valueOf("POISONED"));
    }

    @Test
    void shouldHaveCorrectSpecialConditionCount() {
        assertEquals(5, SpecialCondition.values().length);
    }

    @Test
    void shouldThrowForInvalidErrorCode() {
        assertThrows(IllegalArgumentException.class, () -> ErrorCode.valueOf("INVALID_CODE"));
    }
}
