package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.MatchStateEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.MatchStateJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatePersisterAdapterTest {

    @Mock private MatchJpaRepository matchJpaRepository;
    @Mock private MatchStateJpaRepository matchStateJpaRepository;
    @Mock private ObjectMapper objectMapper;

    private StatePersisterAdapter adapter;
    private UUID matchId;

    @BeforeEach
    void setUp() {
        adapter = new StatePersisterAdapter(matchJpaRepository, matchStateJpaRepository, objectMapper);
        matchId = UUID.randomUUID();
    }

    @Test
    void saveState_validState_savesVersion() throws Exception {
        GameState state = new GameState();
        MatchEntity matchEntity = new MatchEntity();
        matchEntity.setId(matchId);

        when(objectMapper.writeValueAsString(state)).thenReturn("{}");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.of(matchEntity));
        when(matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId)).thenReturn(Optional.empty());

        adapter.saveState(matchId, state);

        verify(matchStateJpaRepository).save(any(MatchStateEntity.class));
        verify(matchJpaRepository).save(matchEntity);
    }

    @Test
    void saveState_matchNotFound_throws() throws Exception {
        GameState state = new GameState();
        when(objectMapper.writeValueAsString(state)).thenReturn("{}");
        when(matchJpaRepository.findById(matchId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> adapter.saveState(matchId, state));
    }

    @Test
    void saveState_serializationException_throws() throws Exception {
        GameState state = new GameState();
        when(objectMapper.writeValueAsString(state)).thenThrow(new RuntimeException("Serialization error"));

        assertThrows(RuntimeException.class, () -> adapter.saveState(matchId, state));
    }

    @Test
    void loadState_existingState_deserializes() throws Exception {
        MatchStateEntity matchState = new MatchStateEntity();
        matchState.setVersion(1L);
        matchState.setSerializedState("{}");

        when(matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId))
                .thenReturn(Optional.of(matchState));
        when(objectMapper.readValue("{}", GameState.class)).thenReturn(new GameState());

        GameState result = adapter.loadState(matchId);

        assertNotNull(result);
    }

    @Test
    void loadState_noState_returnsNull() {
        when(matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId))
                .thenReturn(Optional.empty());

        GameState result = adapter.loadState(matchId);

        assertNull(result);
    }

    @Test
    void loadState_deserializationException_throws() throws Exception {
        MatchStateEntity matchState = new MatchStateEntity();
        matchState.setVersion(1L);
        matchState.setSerializedState("{}");

        when(matchStateJpaRepository.findTopByMatchIdOrderByVersionDesc(matchId))
                .thenReturn(Optional.of(matchState));
        when(objectMapper.readValue("{}", GameState.class))
                .thenThrow(new RuntimeException("Deserialization error"));

        assertThrows(RuntimeException.class, () -> adapter.loadState(matchId));
    }
}
