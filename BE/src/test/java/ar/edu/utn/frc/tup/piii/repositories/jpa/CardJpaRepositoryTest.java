package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CardJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardJpaRepository repository;

    @Test
    void save_and_findById() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");
        entityManager.persistAndFlush(card);

        Optional<CardEntity> found = repository.findById("xy1-1");

        assertTrue(found.isPresent());
        assertEquals("Pikachu", found.get().getName());
        assertEquals("Pokémon", found.get().getSupertype());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<CardEntity> cards = repository.findAll();
        assertTrue(cards.isEmpty());
    }

    @Test
    void findAll_multipleCards() {
        CardEntity card1 = new CardEntity();
        card1.setId("xy1-1");
        card1.setName("Pikachu");
        card1.setSupertype("Pokémon");
        card1.setSetCode("xy1");
        card1.setNumber("1");

        CardEntity card2 = new CardEntity();
        card2.setId("xy1-2");
        card2.setName("Charmander");
        card2.setSupertype("Pokémon");
        card2.setSetCode("xy1");
        card2.setNumber("2");

        entityManager.persist(card1);
        entityManager.persistAndFlush(card2);

        List<CardEntity> cards = repository.findAll();
        assertEquals(2, cards.size());
    }

    @Test
    void delete() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");
        entityManager.persistAndFlush(card);

        repository.deleteById("xy1-1");
        entityManager.flush();

        Optional<CardEntity> found = repository.findById("xy1-1");
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");
        entityManager.persistAndFlush(card);

        CardEntity toUpdate = repository.findById("xy1-1").orElseThrow();
        toUpdate.setName("Raichu");
        repository.flush();

        CardEntity updated = entityManager.find(CardEntity.class, "xy1-1");
        assertEquals("Raichu", updated.getName());
    }

    @Test
    void findByNameIgnoreCase() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");
        entityManager.persistAndFlush(card);

        List<CardEntity> found = repository.findByNameIgnoreCase("pikachu");
        assertEquals(1, found.size());
        assertEquals("xy1-1", found.get(0).getId());

        List<CardEntity> notFound = repository.findByNameIgnoreCase("raichu");
        assertTrue(notFound.isEmpty());
    }

    @Test
    void cascadePersist_attacks() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");

        CardAttackEntity attack = new CardAttackEntity();
        attack.setAttackIndex(0);
        attack.setName("Thunder Shock");
        attack.setConvertedEnergyCost(1);
        attack.setCard(card);
        card.getAttacks().add(attack);

        entityManager.persistAndFlush(card);
        entityManager.clear();

        CardEntity found = entityManager.find(CardEntity.class, "xy1-1");
        assertNotNull(found);
        assertEquals(1, found.getAttacks().size());
        assertEquals("Thunder Shock", found.getAttacks().get(0).getName());
    }
}
