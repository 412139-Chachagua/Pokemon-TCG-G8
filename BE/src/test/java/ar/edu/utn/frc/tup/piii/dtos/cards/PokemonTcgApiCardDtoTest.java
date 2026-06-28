package ar.edu.utn.frc.tup.piii.dtos.cards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokemonTcgApiCardDtoTest {

    @Test
    void shouldCreateCardDtoWithAllFields() {
        AbilityDto ability = new AbilityDto("Overgrow", "Does x2", "Ability");
        AttackDto attack = new AttackDto(0, "Tackle", List.of("Grass"), 1, "10", "Basic attack", 10);
        WeaknessDto weakness = new WeaknessDto("Fire", "x2");
        ResistanceDto resistance = new ResistanceDto("Water", "-20");
        SetInfoDto set = new SetInfoDto("xy1");
        ImagesDto images = new ImagesDto("http://small.png", "http://large.png");

        PokemonTcgApiCardDto card = new PokemonTcgApiCardDto(
                "xy1-1", "Venusaur", "POKEMON", List.of("Basic"),
                "100", List.of("Grass"), List.of("Rule1"),
                null, null, List.of(ability), List.of(attack),
                List.of(weakness), List.of(resistance),
                List.of("Colorless"), 1, set, images, "Rare"
        );

        assertEquals("xy1-1", card.id());
        assertEquals("Venusaur", card.name());
        assertEquals("POKEMON", card.supertype());
        assertEquals("100", card.hp());
        assertEquals(1, card.abilities().size());
        assertEquals("Overgrow", card.abilities().get(0).name());
        assertEquals(1, card.attacks().size());
        assertEquals("Tackle", card.attacks().get(0).name());
        assertEquals("Fire", card.weaknesses().get(0).type());
        assertEquals("Water", card.resistances().get(0).type());
        assertEquals("xy1", card.set().id());
        assertEquals("http://small.png", card.images().small());
        assertEquals("Rare", card.rarity());
    }

    @Test
    void shouldCreateCardDtoWithNulls() {
        PokemonTcgApiCardDto card = new PokemonTcgApiCardDto(
                null, null, null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null, null
        );

        assertNull(card.id());
        assertNull(card.name());
        assertNull(card.hp());
        assertNull(card.types());
        assertNull(card.abilities());
    }

    @Test
    void shouldHandleAbilityDto() {
        AbilityDto ability = new AbilityDto("Rain Dance", "Attach energy", "Pokemon Power");

        assertEquals("Rain Dance", ability.name());
        assertEquals("Attach energy", ability.text());
        assertEquals("Pokemon Power", ability.type());
    }

    @Test
    void shouldHandleAttackDto() {
        AttackDto attack = new AttackDto(0, "Thunderbolt", List.of("Lightning", "Lightning"), 2, "100", "Discard energy", 100);

        assertEquals(0, attack.index());
        assertEquals("Thunderbolt", attack.name());
        assertEquals(2, attack.convertedEnergyCost());
        assertEquals("100", attack.damage());
        assertEquals("Discard energy", attack.text());
        assertEquals(100, attack.baseDamage());
    }

    @Test
    void shouldHandleAttackDtoWithNullCost() {
        AttackDto attack = new AttackDto(1, "Pound", null, 0, "10", null, 10);

        assertEquals(1, attack.index());
        assertNull(attack.cost());
        assertEquals(0, attack.convertedEnergyCost());
    }

    @Test
    void shouldHandleWeaknessAndResistance() {
        WeaknessDto weakness = new WeaknessDto("Psychic", "x2");
        ResistanceDto resistance = new ResistanceDto("Fighting", "-20");

        assertEquals("Psychic", weakness.type());
        assertEquals("x2", weakness.value());
        assertEquals("Fighting", resistance.type());
        assertEquals("-20", resistance.value());
    }

    @Test
    void shouldHandleSetInfoAndImages() {
        SetInfoDto set = new SetInfoDto("sv1");
        ImagesDto images = new ImagesDto("http://small.png", "http://large.png");

        assertEquals("sv1", set.id());
        assertEquals("http://small.png", images.small());
        assertEquals("http://large.png", images.large());
    }

    @Test
    void shouldCompareDifferentCards() {
        PokemonTcgApiCardDto card1 = new PokemonTcgApiCardDto(
                "xy1-1", "Bulbasaur", "POKEMON", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );
        PokemonTcgApiCardDto card2 = new PokemonTcgApiCardDto(
                "xy1-2", "Charmander", "POKEMON", null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null
        );

        assertNotEquals(card1, card2);
        assertNotEquals(card1.id(), card2.id());
        assertNotEquals(card1.name(), card2.name());
    }
}
