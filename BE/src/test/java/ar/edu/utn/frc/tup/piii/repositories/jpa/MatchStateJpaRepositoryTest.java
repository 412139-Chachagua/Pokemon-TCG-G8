package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchStateEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MatchStateJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MatchStateJpaRepository repository;

    private MatchEntity createMatch() {
        MatchEntity match = new MatchEntity();
        match.setStatus("IN_PROGRESS");
        entityManager.persist(match);
        return match;
    }

    @Test
    void save_and_findById() {
        MatchEntity match = createMatch();
        MatchStateEntity state = new MatchStateEntity();
        state.setMatch(match);
        state.setVersion(1L);
        state.setSerializedState("{\"turn\":1}");
        entityManager.persistAndFlush(state);

        Optional<MatchStateEntity> found = repository.findById(state.getId());
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getVersion());
        assertEquals("{\"turn\":1}", found.get().getSerializedState());
        assertNotNull(found.get().getCreatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<MatchStateEntity> states = repository.findAll();
        assertTrue(states.isEmpty());
    }

    @Test
    void findAll_multiple() {
        MatchEntity match = createMatch();

        MatchStateEntity s1 = new MatchStateEntity();
        s1.setMatch(match);
        s1.setVersion(1L);
        s1.setSerializedState("{\"turn\":1}");
        entityManager.persist(s1);

        MatchStateEntity s2 = new MatchStateEntity();
        s2.setMatch(match);
        s2.setVersion(2L);
        s2.setSerializedState("{\"turn\":2}");
        entityManager.persistAndFlush(s2);

        List<MatchStateEntity> states = repository.findAll();
        assertEquals(2, states.size());
    }

    @Test
    void delete() {
        MatchEntity match = createMatch();
        MatchStateEntity state = new MatchStateEntity();
        state.setMatch(match);
        state.setVersion(1L);
        state.setSerializedState("{\"turn\":1}");
        entityManager.persistAndFlush(state);

        repository.deleteById(state.getId());
        entityManager.flush();

        Optional<MatchStateEntity> found = repository.findById(state.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void findTopByMatchIdOrderByVersionDesc() {
        MatchEntity match = createMatch();

        MatchStateEntity s1 = new MatchStateEntity();
        s1.setMatch(match);
        s1.setVersion(1L);
        s1.setSerializedState("{\"turn\":1}");
        entityManager.persist(s1);

        MatchStateEntity s2 = new MatchStateEntity();
        s2.setMatch(match);
        s2.setVersion(2L);
        s2.setSerializedState("{\"turn\":2}");
        entityManager.persistAndFlush(s2);

        Optional<MatchStateEntity> latest = repository.findTopByMatchIdOrderByVersionDesc(match.getId());
        assertTrue(latest.isPresent());
        assertEquals(2L, latest.get().getVersion());
    }
}
