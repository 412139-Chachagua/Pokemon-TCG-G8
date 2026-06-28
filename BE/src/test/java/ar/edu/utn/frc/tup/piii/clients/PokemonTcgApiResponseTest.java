package ar.edu.utn.frc.tup.piii.clients;

import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PokemonTcgApiResponseTest {

    @Test
    void shouldCreateEmptyResponse() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();

        assertNull(response.getData());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    void shouldSetAndGetData() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        List<PokemonTcgApiCardDto> cards = List.of(
            new PokemonTcgApiCardDto("xy1-1", "Venusaur", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
            new PokemonTcgApiCardDto("xy1-2", "Charmander", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        );

        response.setData(cards);

        assertNotNull(response.getData());
        assertEquals(2, response.getData().size());
        assertEquals("xy1-1", response.getData().get(0).id());
        assertEquals("Venusaur", response.getData().get(0).name());
    }

    @Test
    void shouldSetAndGetTotalCount() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();

        response.setTotalCount(150);

        assertEquals(150, response.getTotalCount());
    }

    @Test
    void shouldHandleNullData() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setData(null);
        response.setTotalCount(0);

        assertNull(response.getData());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    void shouldHandleEmptyDataList() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setData(Collections.emptyList());

        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty());
    }

    @Test
    void shouldHandleLargeTotalCount() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setTotalCount(Integer.MAX_VALUE);

        assertEquals(Integer.MAX_VALUE, response.getTotalCount());
    }

    @Test
    void shouldHandleNegativeTotalCount() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setTotalCount(-1);

        assertEquals(-1, response.getTotalCount());
    }

    @Test
    void shouldHandleMultipleMutations() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();

        response.setTotalCount(50);
        response.setData(List.of(
            new PokemonTcgApiCardDto("xy1-1", "Bulbasaur", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        assertEquals(50, response.getTotalCount());
        assertEquals(1, response.getData().size());

        response.setTotalCount(100);
        response.setData(List.of(
            new PokemonTcgApiCardDto("xy1-1", "Bulbasaur", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null),
            new PokemonTcgApiCardDto("xy1-2", "Ivysaur", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        assertEquals(100, response.getTotalCount());
        assertEquals(2, response.getData().size());
    }

    @Test
    void shouldHandleDataWithNullFields() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setData(List.of(
            new PokemonTcgApiCardDto(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        assertNotNull(response.getData());
        assertEquals(1, response.getData().size());
        assertNull(response.getData().get(0).id());
    }

    @Test
    void shouldHandleSingleCardInData() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setData(List.of(
            new PokemonTcgApiCardDto("xy1-1", "Pikachu", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        assertEquals("Pikachu", response.getData().get(0).name());
    }

    @Test
    void shouldHandleTotalCountWithoutData() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setTotalCount(100);
        response.setData(null);

        assertEquals(100, response.getTotalCount());
        assertNull(response.getData());
    }

    @Test
    void shouldHandleDataWithoutTotalCount() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();
        response.setData(List.of(
            new PokemonTcgApiCardDto("xy1-1", "Charizard", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        ));

        assertNotNull(response.getData());
        assertEquals(0, response.getTotalCount());
    }

    @Test
    void shouldHandleMultipleCallsToSetData() {
        PokemonTcgApiResponse response = new PokemonTcgApiResponse();

        List<PokemonTcgApiCardDto> firstBatch = List.of(
            new PokemonTcgApiCardDto("xy1-1", "Card1", "POKEMON", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        );
        response.setData(firstBatch);
        assertEquals(1, response.getData().size());

        List<PokemonTcgApiCardDto> secondBatch = List.of(
            new PokemonTcgApiCardDto("xy1-2", "Card2", "TRAINER", null, null, null, null, null, null, null, null, null, null, null, null, null, null, null)
        );
        response.setData(secondBatch);
        assertEquals(1, response.getData().size());
        assertEquals("TRAINER", response.getData().get(0).supertype());
    }
}
