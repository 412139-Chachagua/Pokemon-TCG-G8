package ar.edu.utn.frc.tup.piii.dtos.cards;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CardSearchRequestTest {

    @Test
    void shouldCreateSearchRequestWithAllFields() {
        CardSearchRequest request = new CardSearchRequest("charizard", "POKEMON", "xy1", "Stage 2", 0, 20);

        assertEquals("charizard", request.query());
        assertEquals("POKEMON", request.supertype());
        assertEquals("xy1", request.setCode());
        assertEquals("Stage 2", request.stage());
        assertEquals(0, request.page());
        assertEquals(20, request.size());
    }

    @Test
    void shouldCreateSearchRequestWithNulls() {
        CardSearchRequest request = new CardSearchRequest(null, null, null, null, null, null);

        assertNull(request.query());
        assertNull(request.supertype());
        assertNull(request.page());
    }

    @Test
    void shouldHandleSearchResponse() {
        CardSummaryResponse summary = new CardSummaryResponse(
                "xy1-1", "Venusaur", "POKEMON", "xy1", "1",
                "http://small.png", List.of("Stage 2"), "Stage 2"
        );
        CardSearchResponse response = new CardSearchResponse(List.of(summary), 0, 20, 1);

        assertEquals(1, response.items().size());
        assertEquals(0, response.page());
        assertEquals(20, response.size());
        assertEquals(1, response.totalItems());
        assertEquals("Venusaur", response.items().get(0).name());
    }

    @Test
    void shouldHandleEmptySearchResponse() {
        CardSearchResponse response = new CardSearchResponse(List.of(), 0, 20, 0);

        assertTrue(response.items().isEmpty());
        assertEquals(0, response.totalItems());
    }

    @Test
    void shouldHandleCardSummaryResponse() {
        CardSummaryResponse summary = new CardSummaryResponse(
                "xy1-1", "Pikachu", "POKEMON", "xy1", "1",
                "http://small.png", List.of("Basic"), "Basic"
        );

        assertEquals("xy1-1", summary.id());
        assertEquals("Pikachu", summary.name());
        assertEquals("POKEMON", summary.supertype());
        assertEquals("Basic", summary.stage());
        assertEquals("http://small.png", summary.imageSmallUrl());
    }

    @Test
    void shouldHandleCardSyncResponse() {
        CardSyncResponse response = new CardSyncResponse(true, "Sync complete", 10, 5);

        assertTrue(response.success());
        assertEquals("Sync complete", response.message());
        assertEquals(10, response.newCards());
        assertEquals(5, response.updatedCards());
    }

    @Test
    void shouldHandleCardSyncResponseFailure() {
        CardSyncResponse response = new CardSyncResponse(false, "Sync failed", 0, 0);

        assertFalse(response.success());
        assertEquals(0, response.newCards());
        assertEquals(0, response.updatedCards());
    }

    @Test
    void shouldHandleCardAbilityResponse() {
        CardAbilityResponse ability = new CardAbilityResponse("Overgrow", "Does x2", "Ability", true);

        assertEquals("Overgrow", ability.name());
        assertEquals("Does x2", ability.text());
        assertEquals("Ability", ability.type());
        assertTrue(ability.isActivable());
    }

    @Test
    void shouldHandleCardAbilityResponseNotActivable() {
        CardAbilityResponse ability = new CardAbilityResponse("Stench", "Poison on contact", "Pokemon Power", false);

        assertFalse(ability.isActivable());
    }
}
