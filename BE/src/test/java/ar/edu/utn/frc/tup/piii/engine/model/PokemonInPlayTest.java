package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PokemonInPlayTest {

    @Test
    void defaults_areEmpty() {
        PokemonInPlay pkm = new PokemonInPlay();

        assertNull(pkm.getInstanceId());
        assertEquals(0, pkm.getDamageCounters());
        assertTrue(pkm.getSpecialConditions().isEmpty());
        assertTrue(pkm.getAbilitiesUsedThisTurn().isEmpty());
    }

    @Test
    void settersAndGetters_roundTrip() {
        PokemonInPlay pkm = new PokemonInPlay();
        UUID id = UUID.randomUUID();

        pkm.setInstanceId(id);
        pkm.setCardDefinitionId("card-1");
        pkm.setDamageCounters(50);
        pkm.setFaceDown(true);
        pkm.setEvolvedThisTurn(true);

        assertEquals(id, pkm.getInstanceId());
        assertEquals("card-1", pkm.getCardDefinitionId());
        assertEquals(50, pkm.getDamageCounters());
        assertTrue(pkm.isFaceDown());
        assertTrue(pkm.isEvolvedThisTurn());
    }

    @Test
    void specialConditionsAndTool() {
        PokemonInPlay pkm = new PokemonInPlay();
        UUID toolId = UUID.randomUUID();
        CardInstance tool = new CardInstance(toolId, "tool-1");

        pkm.setSpecialConditions(java.util.List.of(SpecialCondition.PARALYZED));
        pkm.setAttachedTool(tool);
        pkm.setToolCardInstanceId(toolId);
        pkm.setPreventAllDamageNextTurn(true);

        assertEquals(1, pkm.getSpecialConditions().size());
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.PARALYZED));
        assertSame(tool, pkm.getAttachedTool());
        assertEquals(toolId, pkm.getToolCardInstanceId());
        assertTrue(pkm.isPreventAllDamageNextTurn());
    }

    @Test
    void ownerAndEnteredTurn() {
        PokemonInPlay pkm = new PokemonInPlay();
        UUID ownerId = UUID.randomUUID();
        pkm.setOwnerPlayerId(ownerId);
        pkm.setEnteredTurnNumber(3);

        assertEquals(ownerId, pkm.getOwnerPlayerId());
        assertEquals(3, pkm.getEnteredTurnNumber());
    }

    @Test
    void attackAndRetreatFlags() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setCannotAttackNextTurn(true);
        pkm.setCannotRetreatNextTurn(true);
        pkm.setMustFlipToAttackNextTurn(true);
        pkm.setRestrictedAttackName("Thunderbolt");

        assertTrue(pkm.isCannotAttackNextTurn());
        assertTrue(pkm.isCannotRetreatNextTurn());
        assertTrue(pkm.isMustFlipToAttackNextTurn());
        assertEquals("Thunderbolt", pkm.getRestrictedAttackName());
    }

    @Test
    void damageModifiers() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setNextTurnDamageBonus(20);
        pkm.setReduceDamageNextTurn(30);
        pkm.setPreventionDamageThreshold(100);
        pkm.setAbilitiesSuppressedNextTurn(true);

        assertEquals(20, pkm.getNextTurnDamageBonus());
        assertEquals(30, pkm.getReduceDamageNextTurn());
        assertEquals(100, pkm.getPreventionDamageThreshold());
        assertTrue(pkm.isAbilitiesSuppressedNextTurn());
    }

    @Test
    void attachedEnergies() {
        PokemonInPlay pkm = new PokemonInPlay();
        CardInstance energy = new CardInstance(UUID.randomUUID(), "energy-1");
        pkm.setAttachedEnergies(List.of(energy));

        assertEquals(1, pkm.getAttachedEnergies().size());
        assertSame(energy, pkm.getAttachedEnergies().get(0));
    }

    @Test
    void abilitiesUsedThisTurn() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.getAbilitiesUsedThisTurn().add("ability-1");

        assertTrue(pkm.getAbilitiesUsedThisTurn().contains("ability-1"));
        assertEquals(1, pkm.getAbilitiesUsedThisTurn().size());

        pkm.setAbilitiesUsedThisTurn(new HashSet<>(Set.of("ability-2")));
        assertTrue(pkm.getAbilitiesUsedThisTurn().contains("ability-2"));
    }
}
