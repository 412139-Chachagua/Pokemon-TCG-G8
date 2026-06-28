package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerStatsEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PlayerStatsJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PlayerStatsJpaRepository repository;

    @Test
    void save_and_findById() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setPlayerId(playerId);
        stats.setTotalWins(5);
        stats.setTotalLosses(3);
        entityManager.persistAndFlush(stats);

        Optional<PlayerStatsEntity> found = repository.findById(playerId);
        assertTrue(found.isPresent());
        assertEquals(5, found.get().getTotalWins());
        assertEquals(3, found.get().getTotalLosses());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<PlayerStatsEntity> stats = repository.findAll();
        assertTrue(stats.isEmpty());
    }

    @Test
    void findAll_multiple() {
        PlayerStatsEntity s1 = new PlayerStatsEntity();
        s1.setPlayerId(UUID.randomUUID());
        s1.setTotalWins(10);
        entityManager.persist(s1);

        PlayerStatsEntity s2 = new PlayerStatsEntity();
        s2.setPlayerId(UUID.randomUUID());
        s2.setTotalWins(5);
        entityManager.persistAndFlush(s2);

        List<PlayerStatsEntity> stats = repository.findAll();
        assertEquals(2, stats.size());
    }

    @Test
    void delete() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setPlayerId(playerId);
        entityManager.persistAndFlush(stats);

        repository.deleteById(playerId);
        entityManager.flush();

        Optional<PlayerStatsEntity> found = repository.findById(playerId);
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setPlayerId(playerId);
        stats.setTotalWins(5);
        entityManager.persistAndFlush(stats);

        PlayerStatsEntity toUpdate = repository.findById(playerId).orElseThrow();
        toUpdate.setTotalWins(10);
        toUpdate.setMaxWinStreak(3);
        repository.flush();

        PlayerStatsEntity updated = entityManager.find(PlayerStatsEntity.class, playerId);
        assertEquals(10, updated.getTotalWins());
        assertEquals(3, updated.getMaxWinStreak());
    }

    @Test
    void findByPlayerId() {
        UUID playerId = UUID.randomUUID();
        PlayerStatsEntity stats = new PlayerStatsEntity();
        stats.setPlayerId(playerId);
        stats.setTotalWins(7);
        entityManager.persistAndFlush(stats);

        Optional<PlayerStatsEntity> found = repository.findByPlayerId(playerId);
        assertTrue(found.isPresent());
        assertEquals(7, found.get().getTotalWins());

        Optional<PlayerStatsEntity> notFound = repository.findByPlayerId(UUID.randomUUID());
        assertFalse(notFound.isPresent());
    }

    @Test
    void findAllByOrderByTotalWinsDescMaxWinStreakDesc() {
        PlayerStatsEntity s1 = new PlayerStatsEntity();
        s1.setPlayerId(UUID.randomUUID());
        s1.setTotalWins(10);
        s1.setMaxWinStreak(5);
        entityManager.persist(s1);

        PlayerStatsEntity s2 = new PlayerStatsEntity();
        s2.setPlayerId(UUID.randomUUID());
        s2.setTotalWins(20);
        s2.setMaxWinStreak(8);
        entityManager.persist(s2);

        PlayerStatsEntity s3 = new PlayerStatsEntity();
        s3.setPlayerId(UUID.randomUUID());
        s3.setTotalWins(10);
        s3.setMaxWinStreak(3);
        entityManager.persistAndFlush(s3);

        List<PlayerStatsEntity> ranked = repository.findAllByOrderByTotalWinsDescMaxWinStreakDesc();
        assertEquals(3, ranked.size());
        assertEquals(20, ranked.get(0).getTotalWins());
        assertEquals(10, ranked.get(1).getTotalWins());
        assertEquals(5, ranked.get(1).getMaxWinStreak());
        assertEquals(10, ranked.get(2).getTotalWins());
        assertEquals(3, ranked.get(2).getMaxWinStreak());
    }
}
