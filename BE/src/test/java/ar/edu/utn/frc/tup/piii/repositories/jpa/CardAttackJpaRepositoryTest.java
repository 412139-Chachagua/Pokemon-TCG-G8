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
class CardAttackJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CardAttackJpaRepository repository;

    private CardEntity createCard() {
        CardEntity card = new CardEntity();
        card.setId("xy1-1");
        card.setName("Pikachu");
        card.setSupertype("Pokémon");
        card.setSetCode("xy1");
        card.setNumber("1");
        entityManager.persist(card);
        return card;
    }

    @Test
    void save_and_findById() {
        CardEntity card = createCard();
        CardAttackEntity attack = new CardAttackEntity();
        attack.setCard(card);
        attack.setAttackIndex(0);
        attack.setName("Thunder Shock");
        attack.setConvertedEnergyCost(1);
        attack.setBaseDamage(30);
        entityManager.persistAndFlush(attack);

        Optional<CardAttackEntity> found = repository.findById(attack.getId());
        assertTrue(found.isPresent());
        assertEquals("Thunder Shock", found.get().getName());
        assertEquals(30, found.get().getBaseDamage());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<CardAttackEntity> attacks = repository.findAll();
        assertTrue(attacks.isEmpty());
    }

    @Test
    void findAll_multiple() {
        CardEntity card = createCard();

        CardAttackEntity a1 = new CardAttackEntity();
        a1.setCard(card);
        a1.setAttackIndex(0);
        a1.setName("Thunder Shock");
        a1.setConvertedEnergyCost(1);
        entityManager.persist(a1);

        CardAttackEntity a2 = new CardAttackEntity();
        a2.setCard(card);
        a2.setAttackIndex(1);
        a2.setName("Quick Attack");
        a2.setConvertedEnergyCost(2);
        entityManager.persistAndFlush(a2);

        List<CardAttackEntity> attacks = repository.findAll();
        assertEquals(2, attacks.size());
    }

    @Test
    void delete() {
        CardEntity card = createCard();
        CardAttackEntity attack = new CardAttackEntity();
        attack.setCard(card);
        attack.setAttackIndex(0);
        attack.setName("Thunder Shock");
        attack.setConvertedEnergyCost(1);
        entityManager.persistAndFlush(attack);

        repository.deleteById(attack.getId());
        entityManager.flush();

        Optional<CardAttackEntity> found = repository.findById(attack.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        CardEntity card = createCard();
        CardAttackEntity attack = new CardAttackEntity();
        attack.setCard(card);
        attack.setAttackIndex(0);
        attack.setName("Thunder Shock");
        attack.setConvertedEnergyCost(1);
        entityManager.persistAndFlush(attack);

        CardAttackEntity toUpdate = repository.findById(attack.getId()).orElseThrow();
        toUpdate.setBaseDamage(60);
        repository.flush();

        CardAttackEntity updated = entityManager.find(CardAttackEntity.class, attack.getId());
        assertEquals(60, updated.getBaseDamage());
    }

    @Test
    void findByCardId() {
        CardEntity card1 = createCard();
        CardEntity card2 = new CardEntity();
        card2.setId("xy1-2");
        card2.setName("Charmander");
        card2.setSupertype("Pokémon");
        card2.setSetCode("xy1");
        card2.setNumber("2");
        entityManager.persistAndFlush(card2);

        CardAttackEntity attack1 = new CardAttackEntity();
        attack1.setCard(card1);
        attack1.setAttackIndex(0);
        attack1.setName("Thunder Shock");
        attack1.setConvertedEnergyCost(1);
        entityManager.persist(attack1);

        CardAttackEntity attack2 = new CardAttackEntity();
        attack2.setCard(card2);
        attack2.setAttackIndex(0);
        attack2.setName("Scratch");
        attack2.setConvertedEnergyCost(1);
        entityManager.persistAndFlush(attack2);

        List<CardAttackEntity> found = repository.findByCardId("xy1-1");
        assertEquals(1, found.size());
        assertEquals("Thunder Shock", found.get(0).getName());

        List<CardAttackEntity> notFound = repository.findByCardId("nonexistent");
        assertTrue(notFound.isEmpty());
    }
}
