package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchStateEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchStateJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class StatePersisterAdapter implements StatePersisterPort {

    private static final Logger log = LoggerFactory.getLogger(StatePersisterAdapter.class);

    private final MatchJpaRepository matchJpaRepository;
    private final MatchStateJpaRepository matchStateJpaRepository;
    private final ObjectMapper objectMapper;

    public StatePersisterAdapter(MatchJpaRepository matchJpaRepository,
                                  MatchStateJpaRepository matchStateJpaRepository,
                                  ObjectMapper objectMapper) {
        this.matchJpaRepository = matchJpaRepository;
        this.matchStateJpaRepository = matchStateJpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveState(UUID matchId, GameState state) {
        try {
            String json = objectMapper.writeValueAsString(state);
            log.warn("[saveState] serialized {} bytes for match {}", json.length(), matchId);

            MatchEntity matchEntity = matchJpaRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

            Long nextVersion = matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId)
                    .map(ms -> ms.getVersion() + 1)
                    .orElse(1L);

            MatchStateEntity entity = new MatchStateEntity();
            entity.setMatch(matchEntity);
            entity.setVersion(nextVersion);
            entity.setSerializedState(json);
            matchStateJpaRepository.save(entity);

            matchEntity.setLatestStateVersion(nextVersion);
            matchJpaRepository.save(matchEntity);
            log.warn("[saveState] saved version {} for match {}", nextVersion, matchId);
        } catch (Exception e) {
            log.error("[saveState] error for match {}: {}", matchId, e.getMessage(), e);
            throw new RuntimeException("Failed to persist game state", e);
        }
    }

    @Override
    public GameState loadState(UUID matchId) {
        Optional<MatchStateEntity> opt = matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId);
        if (opt.isEmpty()) {
            log.warn("[loadState] no state found for match {}", matchId);
            return null;
        }
        log.warn("[loadState] found state version {} for match {}, {} bytes",
                opt.get().getVersion(), matchId, opt.get().getSerializedState().length());
        try {
            return objectMapper.readValue(opt.get().getSerializedState(), GameState.class);
        } catch (Exception e) {
            log.error("[loadState] deserialization error for match {}: {}", matchId, e.getMessage(), e);
            throw new RuntimeException("Failed to deserialize game state", e);
        }
    }
}
