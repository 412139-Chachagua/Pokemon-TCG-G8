package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.MatchStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MatchStateJpaRepository extends JpaRepository<MatchStateEntity, UUID> {
    Optional<MatchStateEntity> findTopByMatchIdOrderByVersionDesc(UUID matchId);
}
