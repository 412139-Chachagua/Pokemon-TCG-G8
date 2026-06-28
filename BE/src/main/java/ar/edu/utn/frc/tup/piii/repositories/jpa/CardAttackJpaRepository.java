package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.CardAttackEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardAttackJpaRepository extends JpaRepository<CardAttackEntity, UUID> {
    List<CardAttackEntity> findByCardId(String cardId);
}
