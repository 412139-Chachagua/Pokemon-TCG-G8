package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DeckCardJpaRepository extends JpaRepository<DeckCardEntity, UUID> {
}
