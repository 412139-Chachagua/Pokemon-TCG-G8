package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MatchJpaRepository extends JpaRepository<MatchEntity, UUID> {

    List<MatchEntity> findByStatus(String status);

    List<MatchEntity> findByStatusAndCreatedAtBefore(String status, Instant threshold);

    List<MatchEntity> findByStatusAndPlayers_PlayerIdOrderByCreatedAtDesc(String status, UUID playerId);
}
