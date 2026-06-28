package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class DeckCardJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DeckCardJpaRepository repository;

    private DeckEntity createDeck() {
        DeckEntity deck = new DeckEntity();
        deck.setName("Test Deck");
        entityManager.persist(deck);
        return deck;
    }

    @Test
    void save_and_findById() {
        DeckEntity deck = createDeck();
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setDeck(deck);
        deckCard.setCardId("xy1-1");
        deckCard.setQuantity(2);
        entityManager.persistAndFlush(deckCard);

        Optional<DeckCardEntity> found = repository.findById(deckCard.getId());
        assertTrue(found.isPresent());
        assertEquals("xy1-1", found.get().getCardId());
        assertEquals(2, found.get().getQuantity());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<DeckCardEntity> entries = repository.findAll();
        assertTrue(entries.isEmpty());
    }

    @Test
    void findAll_multiple() {
        DeckEntity deck = createDeck();

        DeckCardEntity dc1 = new DeckCardEntity();
        dc1.setDeck(deck);
        dc1.setCardId("xy1-1");
        dc1.setQuantity(2);
        entityManager.persist(dc1);

        DeckCardEntity dc2 = new DeckCardEntity();
        dc2.setDeck(deck);
        dc2.setCardId("xy1-2");
        dc2.setQuantity(1);
        entityManager.persistAndFlush(dc2);

        List<DeckCardEntity> entries = repository.findAll();
        assertEquals(2, entries.size());
    }

    @Test
    void delete() {
        DeckEntity deck = createDeck();
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setDeck(deck);
        deckCard.setCardId("xy1-1");
        deckCard.setQuantity(1);
        entityManager.persistAndFlush(deckCard);

        repository.deleteById(deckCard.getId());
        entityManager.flush();

        Optional<DeckCardEntity> found = repository.findById(deckCard.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        DeckEntity deck = createDeck();
        DeckCardEntity deckCard = new DeckCardEntity();
        deckCard.setDeck(deck);
        deckCard.setCardId("xy1-1");
        deckCard.setQuantity(1);
        entityManager.persistAndFlush(deckCard);

        DeckCardEntity toUpdate = repository.findById(deckCard.getId()).orElseThrow();
        toUpdate.setQuantity(3);
        repository.flush();

        DeckCardEntity updated = entityManager.find(DeckCardEntity.class, deckCard.getId());
        assertEquals(3, updated.getQuantity());
    }
}
