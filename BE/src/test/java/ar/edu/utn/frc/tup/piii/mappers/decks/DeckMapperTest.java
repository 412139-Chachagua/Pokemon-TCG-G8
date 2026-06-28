package ar.edu.utn.frc.tup.piii.mappers.decks;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.dtos.decks.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DeckMapperTest {

    private CardLookupPort cardLookupPort;
    private DeckMapper deckMapper;

    @BeforeEach
    void setUp() {
        cardLookupPort = mock(CardLookupPort.class);
        deckMapper = new DeckMapper(cardLookupPort);
    }

    @Test
    void shouldMapEntityToResponse() {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        PlayerEntity playerEntity = new PlayerEntity();
        playerEntity.setId(ownerId);
        DeckEntity entity = new DeckEntity();
        entity.setId(id);
        entity.setName("Test Deck");
        entity.setOwnerPlayer(playerEntity);
        entity.setSource("USER");
        entity.setCards(List.of(createCardEntity(entity, "xy1-1", 4)));

        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setName("Slugma");
        def.setSupertype("POKEMON");
        when(cardLookupPort.getCardById("xy1-1")).thenReturn(def);

        DeckValidationResult validation = new DeckValidationResult(true, List.of());
        DeckResponse response = deckMapper.toResponse(entity, validation);

        assertEquals(id.toString(), response.id());
        assertEquals("Test Deck", response.name());
        assertEquals(ownerId.toString(), response.ownerPlayerId());
        assertEquals("USER", response.source());
        assertTrue(response.valid());
        assertEquals(1, response.cards().size());
        assertEquals("xy1-1", response.cards().getFirst().cardId());
        assertEquals("Slugma", response.cards().getFirst().name());
    }

    @Test
    void shouldMapCreateRequestToEntity() {
        String playerId = UUID.randomUUID().toString();
        CreateDeckRequest request = new CreateDeckRequest(
                "New Deck",
                playerId,
                List.of(new CreateDeckRequest.DeckCardRequest("xy1-1", 4))
        );

        DeckEntity entity = deckMapper.toEntity(request);

        assertEquals("New Deck", entity.getName());
        assertNull(entity.getOwnerPlayer());
    }

    @Test
    void shouldMapDomainAndBack() {
        UUID id = UUID.randomUUID();
        DeckEntity entity = new DeckEntity();
        entity.setId(id);
        entity.setName("Domain Deck");
        entity.setOwnerPlayer(null);
        entity.setSource("SEED");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        entity.setCards(List.of(createCardEntity(entity, "xy1-10", 2)));

        Deck deck = deckMapper.toDomain(entity);

        assertNotNull(deck);
        assertEquals(id, deck.getId());
        assertEquals("Domain Deck", deck.getName());
        assertEquals("SEED", deck.getSource());
        assertEquals(1, deck.getCards().size());
        assertEquals("xy1-10", deck.getCards().getFirst().getCardId());
    }

    @Test
    void shouldMapValidationResultToResponse() {
        DeckValidationResult result = new DeckValidationResult(false,
                List.of(DeckValidationError.DECK_SIZE_INVALID));

        DeckValidationResponse response = deckMapper.toValidationResponse(result);

        assertFalse(response.valid());
        assertEquals(1, response.errors().size());
        assertEquals("DECK_SIZE_INVALID", response.errors().getFirst().code());
    }

    @Test
    void shouldMapAceSpecError() {
        DeckValidationResult result = new DeckValidationResult(false,
                List.of(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED));

        DeckValidationResponse response = deckMapper.toValidationResponse(result);

        assertFalse(response.valid());
        assertEquals(1, response.errors().size());
        assertEquals("ACE_SPEC_LIMIT_EXCEEDED", response.errors().getFirst().code());
        assertEquals("El mazo no puede tener m\u00e1s de 1 carta AS T\u00c1CTICO.", response.errors().getFirst().message());
    }

    @Test
    void shouldMapMoreThan4CopiesError() {
        DeckValidationResult result = new DeckValidationResult(false,
                List.of(DeckValidationError.MORE_THAN_4_COPIES));

        DeckValidationResponse response = deckMapper.toValidationResponse(result);

        assertFalse(response.valid());
        assertEquals(1, response.errors().size());
        assertEquals("MORE_THAN_4_COPIES", response.errors().getFirst().code());
    }

    private DeckCardEntity createCardEntity(DeckEntity deck, String cardId, int quantity) {
        DeckCardEntity entity = new DeckCardEntity();
        entity.setId(UUID.randomUUID());
        entity.setDeck(deck);
        entity.setCardId(cardId);
        entity.setQuantity(quantity);
        return entity;
    }
}