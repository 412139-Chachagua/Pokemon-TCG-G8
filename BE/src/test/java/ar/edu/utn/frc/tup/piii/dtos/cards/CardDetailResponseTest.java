package ar.edu.utn.frc.tup.piii.dtos.cards;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardDetailResponseTest {

    @Test
    void shouldCreateCardDetailResponse() {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-1", "Venusaur", "POKEMON", List.of("Basic"),
                "xy1", "1", "http://small.png", "http://large.png",
                List.of("Rule1"), 100, "Basic", null,
                List.of("Grass"), List.of(), List.of(), List.of(),
                List.of("Colorless"), true, false, List.of(), List.of()
        );

        assertEquals("xy1-1", response.id());
        assertEquals("Venusaur", response.name());
        assertEquals("POKEMON", response.supertype());
        assertEquals(List.of("Basic"), response.subtypes());
        assertEquals("xy1", response.setCode());
        assertEquals("1", response.number());
        assertEquals("http://small.png", response.imageSmallUrl());
        assertEquals("http://large.png", response.imageLargeUrl());
        assertEquals(List.of("Rule1"), response.rulesText());
        assertEquals(100, response.hp());
        assertEquals("Basic", response.stage());
        assertNull(response.evolvesFrom());
        assertEquals(List.of("Grass"), response.types());
        assertTrue(response.isEx());
        assertFalse(response.isMega());
    }

    @Test
    void shouldHandleNullLists() {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-1", "Venusaur", "POKEMON", null,
                "xy1", "1", null, null,
                null, null, null, null,
                null, null, null, null,
                null, null, null, null, null
        );

        assertEquals("xy1-1", response.id());
        assertNull(response.subtypes());
        assertNull(response.rulesText());
        assertNull(response.types());
        assertNull(response.attacks());
        assertNull(response.abilities());
    }

    @Test
    void shouldHandleBothExAndMegaFalse() {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-1", "Charizard", "POKEMON", List.of("Stage 2"),
                "xy1", "1", null, null,
                null, 150, "Stage 2", "Charmeleon",
                List.of("Fire"), List.of(), List.of(), List.of(),
                List.of("Colorless", "Lightning"), false, false, List.of(), List.of()
        );

        assertFalse(response.isEx());
        assertFalse(response.isMega());
        assertEquals("Charmeleon", response.evolvesFrom());
        assertEquals(150, response.hp());
    }

    @Test
    void shouldHandleEmptyCollections() {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-1", "Pikachu", "POKEMON", Collections.emptyList(),
                "xy1", "1", null, null,
                Collections.emptyList(), 60, "Basic", null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), false, false, Collections.emptyList(), Collections.emptyList()
        );

        assertTrue(response.subtypes().isEmpty());
        assertTrue(response.types().isEmpty());
        assertTrue(response.attacks().isEmpty());
    }

    @Test
    void shouldHandleFullTrainerCard() {
        CardDetailResponse response = new CardDetailResponse(
                "xy1-133", "Professor's Letter", "TRAINER", List.of("Item"),
                "xy1", "133", null, null,
                List.of("Search your deck"), null, null, null,
                null, null, null, null,
                null, false, false, null, null
        );

        assertEquals("TRAINER", response.supertype());
        assertNull(response.hp());
        assertNull(response.stage());
    }
}
