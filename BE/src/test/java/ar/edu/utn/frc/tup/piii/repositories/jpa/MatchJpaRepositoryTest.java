package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MatchJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchJpaRepository repository;

    @Test
    void save_and_findById() {
        MatchEntity match = new MatchEntity();
        match.setStatus("WAITING");
        entityManager.persistAndFlush(match);

        Optional<MatchEntity> found = repository.findById(match.getId());
        assertTrue(found.isPresent());
        assertEquals("WAITING", found.get().getStatus());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<MatchEntity> matches = repository.findAll();
        assertTrue(matches.isEmpty());
    }

    @Test
    void findAll_multipleMatches() {
        MatchEntity m1 = new MatchEntity();
        m1.setStatus("WAITING");
        entityManager.persist(m1);

        MatchEntity m2 = new MatchEntity();
        m2.setStatus("IN_PROGRESS");
        entityManager.persistAndFlush(m2);

        List<MatchEntity> matches = repository.findAll();
        assertEquals(2, matches.size());
    }

    @Test
    void delete() {
        MatchEntity match = new MatchEntity();
        match.setStatus("WAITING");
        entityManager.persistAndFlush(match);

        repository.deleteById(match.getId());
        entityManager.flush();

        Optional<MatchEntity> found = repository.findById(match.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        MatchEntity match = new MatchEntity();
        match.setStatus("WAITING");
        entityManager.persistAndFlush(match);

        MatchEntity toUpdate = repository.findById(match.getId()).orElseThrow();
        toUpdate.setStatus("IN_PROGRESS");
        repository.flush();

        MatchEntity updated = entityManager.find(MatchEntity.class, match.getId());
        assertEquals("IN_PROGRESS", updated.getStatus());
    }

    @Test
    void findByStatus() {
        MatchEntity m1 = new MatchEntity();
        m1.setStatus("WAITING");
        entityManager.persist(m1);

        MatchEntity m2 = new MatchEntity();
        m2.setStatus("IN_PROGRESS");
        entityManager.persistAndFlush(m2);

        List<MatchEntity> waiting = repository.findByStatus("WAITING");
        assertEquals(1, waiting.size());

        List<MatchEntity> inProgress = repository.findByStatus("IN_PROGRESS");
        assertEquals(1, inProgress.size());

        List<MatchEntity> finished = repository.findByStatus("FINISHED");
        assertTrue(finished.isEmpty());
    }
//
//    @Test
//    void findByStatusAndCreatedAtBefore() throws Exception {
//        MatchEntity oldMatch = new MatchEntity();
//        oldMatch.setStatus("FINISHED");
//        entityManager.persist(oldMatch);
//        entityManager.flush();
//
//        Thread.sleep(200);
//
//        Instant threshold = Instant.now();
//
//        MatchEntity newMatch = new MatchEntity();
//        newMatch.setStatus("FINISHED");
//        entityManager.persist(newMatch);
//        entityManager.flush();
//
//        List<MatchEntity> beforeThreshold = repository.findByStatusAndCreatedAtBefore("FINISHED", threshold);
//        assertEquals(1, beforeThreshold.size());
//        assertEquals(oldMatch.getId(), beforeThreshold.get(0).getId());
//    }

    @Test
    void findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc() {
        UUID playerId = UUID.randomUUID();

        MatchEntity match1 = new MatchEntity();
        match1.setStatus("FINISHED");
        entityManager.persist(match1);

        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setMatch(match1);
        mp1.setPlayerId(playerId);
        mp1.setPlayerKind("HUMAN");
        mp1.setSide("PLAYER_1");
        mp1.setDisplayName("Player 1");
        match1.getPlayers().add(mp1);

        MatchEntity match2 = new MatchEntity();
        match2.setStatus("FINISHED");
        entityManager.persist(match2);

        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setMatch(match2);
        mp2.setPlayerId(playerId);
        mp2.setPlayerKind("HUMAN");
        mp2.setSide("PLAYER_1");
        mp2.setDisplayName("Player 1");
        match2.getPlayers().add(mp2);

        entityManager.flush();

        List<MatchEntity> matches = repository
                .findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc("FINISHED", playerId);
        assertEquals(2, matches.size());
        assertTrue(matches.get(0).getCreatedAt().isAfter(matches.get(1).getCreatedAt())
                || matches.get(0).getCreatedAt().equals(matches.get(1).getCreatedAt()));
    }
}
