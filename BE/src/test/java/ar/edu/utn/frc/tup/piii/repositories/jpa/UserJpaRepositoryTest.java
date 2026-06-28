package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserJpaRepository repository;

    @Test
    void save_and_findById() {
        UserEntity user = new UserEntity();
        user.setUsername("ashketchum");
        user.setEmail("ash@example.com");
        user.setPassword("p@ss");
        entityManager.persistAndFlush(user);

        Optional<UserEntity> found = repository.findById(user.getId());
        assertTrue(found.isPresent());
        assertEquals("ashketchum", found.get().getUsername());
        assertEquals("ash@example.com", found.get().getEmail());
        assertNotNull(found.get().getCreatedAt());
        assertNotNull(found.get().getUpdatedAt());
    }

    @Test
    void findAll_emptyList() {
        List<UserEntity> users = repository.findAll();
        assertTrue(users.isEmpty());
    }

    @Test
    void findAll_multipleUsers() {
        UserEntity u1 = new UserEntity();
        u1.setUsername("ash");
        u1.setEmail("ash@example.com");
        entityManager.persist(u1);

        UserEntity u2 = new UserEntity();
        u2.setUsername("misty");
        u2.setEmail("misty@example.com");
        entityManager.persistAndFlush(u2);

        List<UserEntity> users = repository.findAll();
        assertEquals(2, users.size());
    }

    @Test
    void delete() {
        UserEntity user = new UserEntity();
        user.setUsername("ash");
        user.setEmail("ash@example.com");
        entityManager.persistAndFlush(user);

        repository.deleteById(user.getId());
        entityManager.flush();

        Optional<UserEntity> found = repository.findById(user.getId());
        assertFalse(found.isPresent());
    }

    @Test
    void update() {
        UserEntity user = new UserEntity();
        user.setUsername("ash");
        user.setEmail("ash@example.com");
        entityManager.persistAndFlush(user);

        UserEntity toUpdate = repository.findById(user.getId()).orElseThrow();
        toUpdate.setStatus("INACTIVE");
        repository.flush();

        UserEntity updated = entityManager.find(UserEntity.class, user.getId());
        assertEquals("INACTIVE", updated.getStatus());
    }

    @Test
    void findByUsername() {
        UserEntity user = new UserEntity();
        user.setUsername("ashketchum");
        user.setEmail("ash@example.com");
        entityManager.persistAndFlush(user);

        Optional<UserEntity> found = repository.findByUsername("ashketchum");
        assertTrue(found.isPresent());
        assertEquals("ash@example.com", found.get().getEmail());

        Optional<UserEntity> notFound = repository.findByUsername("unknown");
        assertFalse(notFound.isPresent());
    }

    @Test
    void findByEmail() {
        UserEntity user = new UserEntity();
        user.setUsername("ashketchum");
        user.setEmail("ash@example.com");
        entityManager.persistAndFlush(user);

        Optional<UserEntity> found = repository.findByEmail("ash@example.com");
        assertTrue(found.isPresent());
        assertEquals("ashketchum", found.get().getUsername());

        Optional<UserEntity> notFound = repository.findByEmail("unknown@example.com");
        assertFalse(notFound.isPresent());
    }
}
