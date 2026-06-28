package ar.edu.utn.frc.tup.piii.repositories.jpa;

import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardJpaRepository extends JpaRepository<CardEntity, String>, JpaSpecificationExecutor<CardEntity> {
    List<CardEntity> findByNameIgnoreCase(String name);
}
