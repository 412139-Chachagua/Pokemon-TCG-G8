package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PlayerJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlayerJpaRepository repository;

    @Test
    void save_and_findById() {
        PlayerEntity player = new PlayerEntity();
        player.setDisplayName("Ash Ketchum");
        entityManager.persistAndFlush(player);

        Optional<PlayerEntity> found = repository.findById(player.getId());
        assertTrue(found.isPresent());
        assertEquals("Ash Ketchum", found.get().getDisplayName());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<PlayerEntity> players = repository.findAll();
        assertTrue(players.isEmpty());
    }

    @Test
    void findAll_multiplePlayers() {
        PlayerEntity p1 = new PlayerEntity();
        p1.setDisplayName("Ash");
        entityManager.persist(p1);

        PlayerEntity p2 = new PlayerEntity();
        p2.setDisplayName("Misty");
        entityManager.persistAndFlush(p2);

        List<PlayerEntity> players = repository.findAll();
        assertEquals(2, players.size());
    }

    @Test
    void delete() {
        PlayerEntity player = new PlayerEntity();
        player.setDisplayName("Ash");
        entityManager.persistAndFlush(player);

        repository.deleteById(player.getId());
        entityManager.flush();

        Optional<PlayerEntity> found = repository.findById(player.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        PlayerEntity player = new PlayerEntity();
        player.setDisplayName("Ash");
        entityManager.persistAndFlush(player);

        PlayerEntity toUpdate = repository.findById(player.getId()).orElseThrow();
        toUpdate.setDisplayName("Ash Ketchum");
        repository.flush();

        PlayerEntity updated = entityManager.find(PlayerEntity.class, player.getId());
        assertEquals("Ash Ketchum", updated.getDisplayName());
    }

    @Test
    void save_withUser() {
        UserEntity user = new UserEntity();
        user.setUsername("ash");
        user.setEmail("ash@example.com");
        user.setPassword("p@ss");
        entityManager.persist(user);

        PlayerEntity player = new PlayerEntity();
        player.setDisplayName("Ash");
        player.setUser(user);
        entityManager.persistAndFlush(player);

        Optional<PlayerEntity> found = repository.findById(player.getId());
        assertTrue(found.isPresent());
        assertNotNull(found.get().getUser());
        assertEquals("ash", found.get().getUser().getUsername());
    }
}
