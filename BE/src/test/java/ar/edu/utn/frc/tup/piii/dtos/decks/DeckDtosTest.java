package ar.edu.utn.frc.tup.piii.dtos.decks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DeckDtosTest {

    @Test
    void shouldCreateCreateDeckRequest() {
        var cardRequest = new CreateDeckRequest.DeckCardRequest("xy1-1", 4);
        CreateDeckRequest request = new CreateDeckRequest("My Deck", "player-1", List.of(cardRequest));

        assertEquals("My Deck", request.name());
        assertEquals("player-1", request.playerId());
        assertEquals(1, request.cards().size());
        assertEquals("xy1-1", request.cards().get(0).cardId());
        assertEquals(4, request.cards().get(0).quantity());
    }

    @Test
    void shouldCreateDeckCardResponse() {
        DeckCardResponse response = new DeckCardResponse(
                "xy1-1", "Venusaur", 4, "POKEMON", false,
                List.of("Stage 2"), "Stage 2"
        );

        assertEquals("xy1-1", response.cardId());
        assertEquals("Venusaur", response.name());
        assertEquals(4, response.quantity());
        assertFalse(response.isBasicEnergy());
        assertEquals("Stage 2", response.stage());
    }

    @Test
    void shouldCreateDeckResponseWithOverloadedConstructor() {
        DeckValidationResponse validation = new DeckValidationResponse(true, List.of());
        DeckCardResponse card = new DeckCardResponse("xy1-1", "Pikachu", 1, "POKEMON", false, List.of("Basic"), "Basic");
        DeckResponse response = new DeckResponse(
                "deck-1", "My Deck", "player-1", "MANUAL",
                60, true, List.of(card), validation, "2025-01-01"
        );

        assertEquals("deck-1", response.id());
        assertEquals("My Deck", response.name());
        assertEquals(60, response.totalCards());
        assertTrue(response.valid());
        assertNull(response.mainCardId());
        assertNull(response.mainCardImageUrl());
    }

    @Test
    void shouldCreateDeckResponseWithMainCard() {
        DeckValidationResponse validation = new DeckValidationResponse(true, List.of());
        DeckResponse response = new DeckResponse(
                "deck-1", "My Deck", "player-1", "MANUAL",
                60, true, "xy1-1", "http://img.png",
                List.of(), validation, "2025-01-01"
        );

        assertEquals("xy1-1", response.mainCardId());
        assertEquals("http://img.png", response.mainCardImageUrl());
    }

    @Test
    void shouldHandleDeckValidationResponse() {
        var error = new DeckValidationResponse.DeckValidationError("TOO_FEW_CARDS", "Minimum 60 cards", null);
        DeckValidationResponse response = new DeckValidationResponse(false, List.of(error));

        assertFalse(response.valid());
        assertEquals(1, response.errors().size());
        assertEquals("TOO_FEW_CARDS", response.errors().get(0).code());
        assertEquals("Minimum 60 cards", response.errors().get(0).message());
    }

    @Test
    void shouldHandleEmptyValidationErrors() {
        DeckValidationResponse response = new DeckValidationResponse(true, List.of());

        assertTrue(response.valid());
        assertTrue(response.errors().isEmpty());
    }

    @Test
    void shouldCreateImportDeckRequest() {
        var card = new CreateDeckRequest.DeckCardRequest("xy1-1", 4);
        ImportDeckRequest request = new ImportDeckRequest("Imported Deck", List.of(card));

        assertEquals("Imported Deck", request.name());
        assertEquals(1, request.cards().size());
    }

    @Test
    void shouldCreatePredefinedDeckCardEntry() {
        PredefinedDeckCardEntry entry = new PredefinedDeckCardEntry("xy1-1", "Pikachu", "POKEMON", 4);

        assertEquals("xy1-1", entry.cardId());
        assertEquals("Pikachu", entry.name());
        assertEquals(4, entry.quantity());
    }

    @Test
    void shouldCreatePredefinedDeckTemplate() {
        UUID id = UUID.randomUUID();
        var card = new PredefinedDeckCardEntry("xy1-1", "Pikachu", "POKEMON", 4);
        PredefinedDeckTemplate template = new PredefinedDeckTemplate(id, "Starter Deck", "xy1-1", List.of(card));

        assertEquals(id, template.id());
        assertEquals("Starter Deck", template.name());
        assertEquals("xy1-1", template.mainCardId());
        assertEquals(1, template.cards().size());
    }

    @Test
    void shouldCreateUpdateDeckRequest() {
        var card = new CreateDeckRequest.DeckCardRequest("xy1-2", 2);
        UpdateDeckRequest request = new UpdateDeckRequest("Updated Deck", List.of(card));

        assertEquals("Updated Deck", request.name());
        assertEquals(1, request.cards().size());
    }

    @Test
    void shouldCreateValidateDeckRequest() {
        var entry = new ValidateDeckRequest.ValidateCardEntry("xy1-1", 4);
        ValidateDeckRequest request = new ValidateDeckRequest(List.of(entry));

        assertEquals(1, request.cards().size());
        assertEquals("xy1-1", request.cards().get(0).cardId());
        assertEquals(4, request.cards().get(0).quantity());
    }
}
