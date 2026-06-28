package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchPlayerEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MatchPlayerJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchPlayerJpaRepository repository;

    private MatchEntity createMatch() {
        MatchEntity match = new MatchEntity();
        match.setStatus("WAITING");
        entityManager.persist(match);
        return match;
    }

    @Test
    void save_and_findById() {
        MatchEntity match = createMatch();
        MatchPlayerEntity mp = new MatchPlayerEntity();
        mp.setMatch(match);
        mp.setPlayerId(UUID.randomUUID());
        mp.setPlayerKind("HUMAN");
        mp.setSide("PLAYER_1");
        mp.setDisplayName("Player 1");
        entityManager.persistAndFlush(mp);

        Optional<MatchPlayerEntity> found = repository.findById(mp.getId());
        assertTrue(found.isPresent());
        assertEquals("Player 1", found.get().getDisplayName());
        assertNotNull(found.get().getJoinedAt());
    }

    @Test
    void findAll_emptyList() {
        List<MatchPlayerEntity> entries = repository.findAll();
        assertTrue(entries.isEmpty());
    }

    @Test
    void findAll_multiple() {
        MatchEntity match = createMatch();

        MatchPlayerEntity mp1 = new MatchPlayerEntity();
        mp1.setMatch(match);
        mp1.setPlayerId(UUID.randomUUID());
        mp1.setPlayerKind("HUMAN");
        mp1.setSide("PLAYER_1");
        mp1.setDisplayName("Player 1");
        entityManager.persist(mp1);

        MatchPlayerEntity mp2 = new MatchPlayerEntity();
        mp2.setMatch(match);
        mp2.setPlayerId(UUID.randomUUID());
        mp2.setPlayerKind("HUMAN");
        mp2.setSide("PLAYER_2");
        mp2.setDisplayName("Player 2");
        entityManager.persistAndFlush(mp2);

        List<MatchPlayerEntity> entries = repository.findAll();
        assertEquals(2, entries.size());
    }

    @Test
    void delete() {
        MatchEntity match = createMatch();
        MatchPlayerEntity mp = new MatchPlayerEntity();
        mp.setMatch(match);
        mp.setPlayerId(UUID.randomUUID());
        mp.setPlayerKind("HUMAN");
        mp.setSide("PLAYER_1");
        mp.setDisplayName("Player 1");
        entityManager.persistAndFlush(mp);

        repository.deleteById(mp.getId());
        entityManager.flush();

        Optional<MatchPlayerEntity> found = repository.findById(mp.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void findByMatch_Id() {
        MatchEntity match1 = createMatch();
        MatchEntity match2 = createMatch();
        entityManager.flush();

        MatchPlayerEntity mp = new MatchPlayerEntity();
        mp.setMatch(match1);
        mp.setPlayerId(UUID.randomUUID());
        mp.setPlayerKind("HUMAN");
        mp.setSide("PLAYER_1");
        mp.setDisplayName("Player 1");
        entityManager.persistAndFlush(mp);

        List<MatchPlayerEntity> found = repository.findByMatch_Id(match1.getId());
        assertEquals(1, found.size());

        List<MatchPlayerEntity> notFound = repository.findByMatch_Id(match2.getId());
        assertTrue(notFound.isEmpty());
    }
}
