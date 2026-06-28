package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DeckJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeckJpaRepository repository;

    private PlayerEntity createPlayer() {
        UserEntity user = new UserEntity();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword("pass");
        entityManager.persist(user);

        PlayerEntity player = new PlayerEntity();
        player.setUser(user);
        player.setDisplayName("Test Player");
        entityManager.persist(player);
        return player;
    }

    @Test
    void save_and_findById() {
        DeckEntity deck = new DeckEntity();
        deck.setName("My Deck");
        deck.setValid(true);
        entityManager.persistAndFlush(deck);

        Optional<DeckEntity> found = repository.findById(deck.getId());
        assertTrue(found.isPresent());
        assertEquals("My Deck", found.get().getName());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<DeckEntity> decks = repository.findAll();
        assertTrue(decks.isEmpty());
    }

    @Test
    void findAll_multipleDecks() {
        DeckEntity deck1 = new DeckEntity();
        deck1.setName("Deck 1");
        entityManager.persist(deck1);

        DeckEntity deck2 = new DeckEntity();
        deck2.setName("Deck 2");
        entityManager.persistAndFlush(deck2);

        List<DeckEntity> decks = repository.findAll();
        assertEquals(2, decks.size());
    }

    @Test
    void delete() {
        DeckEntity deck = new DeckEntity();
        deck.setName("My Deck");
        entityManager.persistAndFlush(deck);

        repository.deleteById(deck.getId());
        entityManager.flush();

        Optional<DeckEntity> found = repository.findById(deck.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        DeckEntity deck = new DeckEntity();
        deck.setName("My Deck");
        entityManager.persistAndFlush(deck);

        DeckEntity toUpdate = repository.findById(deck.getId()).orElseThrow();
        toUpdate.setName("Updated Deck");
        repository.flush();

        DeckEntity updated = entityManager.find(DeckEntity.class, deck.getId());
        assertEquals("Updated Deck", updated.getName());
    }

    @Test
    void findByOwnerPlayerId() {
        PlayerEntity player = createPlayer();

        DeckEntity deck = new DeckEntity();
        deck.setName("Player Deck");
        deck.setOwnerPlayer(player);
        entityManager.persistAndFlush(deck);

        List<DeckEntity> found = repository.findByOwnerPlayerId(player.getId());
        assertEquals(1, found.size());
        assertEquals("Player Deck", found.get(0).getName());

        List<DeckEntity> notFound = repository.findByOwnerPlayerId(UUID.randomUUID());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void findByIdWithCards() {
        DeckEntity deck = new DeckEntity();
        deck.setName("Deck With Cards");

        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setCardId("xy1-1");
        deckCard.setQuantity(2);
        deckCard.setDeck(deck);
        deck.getCards().add(deckCard);

        entityManager.persistAndFlush(deck);
        entityManager.clear();

        Optional<DeckEntity> found = repository.findByIdWithCards(deck.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getCards());
        assertEquals(1, found.get().getCards().size());
        assertEquals("xy1-1", found.get().getCards().get(0).getCardId());
    }

    @Test
    void findPredefinedDecks() {
        DeckEntity predefined = new DeckEntity();
        predefined.setName("Predefined Deck");
        predefined.setSource("PREDEFINED");
        entityManager.persist(predefined);

        PlayerEntity player = createPlayer();
        DeckEntity userDeck = new DeckEntity();
        userDeck.setName("User Deck");
        userDeck.setOwnerPlayer(player);
        entityManager.persistAndFlush(userDeck);

        List<DeckEntity> predefinedDecks = repository.findPredefinedDecks();
        assertEquals(1, predefinedDecks.size());
        assertEquals("Predefined Deck", predefinedDecks.get(0).getName());
    }
}
