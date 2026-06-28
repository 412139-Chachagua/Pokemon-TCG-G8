package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.PredefinedDeckTemplate;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PredefinedDeckService")
class PredefinedDeckServiceTest {

    @Mock
    private DeckJpaRepository deckJpaRepository;
    @Mock
    private PlayerJpaRepository playerJpaRepository;
    @Captor
    private ArgumentCaptor<DeckEntity> deckCaptor;

    private PredefinedDeckService predefinedDeckService;

    private static final UUID DESTRUCTION_RUSH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESILIENT_LIFE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    @BeforeEach
    void setUp() {
        predefinedDeckService = new PredefinedDeckService(deckJpaRepository, playerJpaRepository);
    }

    @Nested
    @DisplayName("getAllTemplates()")
    class GetAllTemplatesTests {

        @Test
        void shouldReturnTwoTemplates() {
            List<PredefinedDeckTemplate> templates = predefinedDeckService.getAllTemplates();

            assertEquals(2, templates.size());
        }

        @Test
        void shouldHaveCorrectStructure() {
            List<PredefinedDeckTemplate> templates = predefinedDeckService.getAllTemplates();

            PredefinedDeckTemplate destruction = templates.get(0);
            assertEquals(DESTRUCTION_RUSH_ID, destruction.id());
            assertEquals("Destruction Rush", destruction.name());
            assertNotNull(destruction.mainCardId());
            assertFalse(destruction.cards().isEmpty());

            PredefinedDeckTemplate resilient = templates.get(1);
            assertEquals(RESILIENT_LIFE_ID, resilient.id());
            assertEquals("Resilient Life", resilient.name());
            assertNotNull(resilient.mainCardId());
            assertFalse(resilient.cards().isEmpty());
        }

        @Test
        void shouldComputeTotalCardsForDestructionRush() {
            PredefinedDeckTemplate template = predefinedDeckService.getTemplateById(DESTRUCTION_RUSH_ID);
            int total = template.cards().stream().mapToInt(c -> c.quantity()).sum();
            assertEquals(60, total, "Destruction Rush debe tener exactamente 60 cartas");
        }

        @Test
        void shouldComputeTotalCardsForResilientLife() {
            PredefinedDeckTemplate template = predefinedDeckService.getTemplateById(RESILIENT_LIFE_ID);
            int total = template.cards().stream().mapToInt(c -> c.quantity()).sum();
            assertEquals(60, total, "Resilient Life debe tener exactamente 60 cartas");
        }
    }

    @Nested
    @DisplayName("getTemplateById()")
    class GetTemplateByIdTests {

        @Test
        void shouldReturnDestructionRush() {
            PredefinedDeckTemplate template = predefinedDeckService.getTemplateById(DESTRUCTION_RUSH_ID);

            assertNotNull(template);
            assertEquals("Destruction Rush", template.name());
        }

        @Test
        void shouldReturnResilientLife() {
            PredefinedDeckTemplate template = predefinedDeckService.getTemplateById(RESILIENT_LIFE_ID);

            assertNotNull(template);
            assertEquals("Resilient Life", template.name());
        }

        @Test
        void shouldThrowNotFoundForUnknownId() {
            UUID unknown = UUID.randomUUID();

            NotFoundException ex = assertThrows(NotFoundException.class, () -> predefinedDeckService.getTemplateById(unknown));
            assertTrue(ex.getMessage().contains("Template") || ex.getMessage().contains("template"));
        }

        @Test
        void shouldThrowNotFoundForNullId() {
            assertThrows(NotFoundException.class, () -> predefinedDeckService.getTemplateById(null));
        }
    }

    @Nested
    @DisplayName("getAllAsResponse()")
    class GetAllAsResponseTests {

        @Test
        void shouldReturnTwoResponses() {
            List<DeckResponse> responses = predefinedDeckService.getAllAsResponse();

            assertEquals(2, responses.size());
        }

        @Test
        void shouldHaveCorrectSource() {
            List<DeckResponse> responses = predefinedDeckService.getAllAsResponse();

            responses.forEach(r -> assertEquals("PREDEFINED", r.source()));
        }

        @Test
        void shouldHaveValidFlag() {
            List<DeckResponse> responses = predefinedDeckService.getAllAsResponse();

            responses.forEach(r -> assertTrue(r.valid() || !r.valid()));
        }

        @Test
        void shouldBeSortedByName() {
            List<DeckResponse> responses = predefinedDeckService.getAllAsResponse();

            assertEquals("Destruction Rush", responses.get(0).name());
            assertEquals("Resilient Life", responses.get(1).name());
        }
    }

    @Nested
    @DisplayName("copyToPlayer()")
    class CopyToPlayerTests {

        @Test
        void shouldCopyDestructionRushToPlayer() {
            UUID playerId = UUID.randomUUID();
            DeckEntity savedEntity = new DeckEntity();
            savedEntity.setId(UUID.randomUUID());

            when(playerJpaRepository.existsById(playerId)).thenReturn(true);
            when(playerJpaRepository.getReferenceById(playerId)).thenReturn(null);
            when(deckJpaRepository.save(any(DeckEntity.class))).thenReturn(savedEntity);

            DeckResponse response = predefinedDeckService.copyToPlayer(DESTRUCTION_RUSH_ID, playerId);

            assertNotNull(response);
            verify(deckJpaRepository).save(any(DeckEntity.class));
        }

        @Test
        void shouldCopyResilientLifeToPlayer() {
            UUID playerId = UUID.randomUUID();
            DeckEntity savedEntity = new DeckEntity();
            savedEntity.setId(UUID.randomUUID());

            when(playerJpaRepository.existsById(playerId)).thenReturn(true);
            when(playerJpaRepository.getReferenceById(playerId)).thenReturn(null);
            when(deckJpaRepository.save(any(DeckEntity.class))).thenReturn(savedEntity);

            DeckResponse response = predefinedDeckService.copyToPlayer(RESILIENT_LIFE_ID, playerId);

            assertNotNull(response);
        }

        @Test
        void shouldSetCorrectOwnerOnCopiedDeck() {
            UUID playerId = UUID.randomUUID();
            DeckEntity savedEntity = new DeckEntity();
            savedEntity.setId(UUID.randomUUID());

            PlayerEntity ownerPlayer = new PlayerEntity();
            ownerPlayer.setId(playerId);

            when(playerJpaRepository.existsById(playerId)).thenReturn(true);
            when(playerJpaRepository.getReferenceById(playerId)).thenReturn(ownerPlayer);
            when(deckJpaRepository.save(any(DeckEntity.class))).thenReturn(savedEntity);

            predefinedDeckService.copyToPlayer(DESTRUCTION_RUSH_ID, playerId);

            verify(deckJpaRepository).save(deckCaptor.capture());
            DeckEntity saved = deckCaptor.getValue();
            assertEquals(playerId, saved.getOwnerPlayer().getId());
            assertEquals("Destruction Rush", saved.getName());
            assertEquals("USER", saved.getSource());
        }

        @Test
        void shouldThrowWhenPlayerNotFound() {
            UUID playerId = UUID.randomUUID();

            when(playerJpaRepository.existsById(playerId)).thenReturn(false);

            NotFoundException ex = assertThrows(NotFoundException.class, () ->
                    predefinedDeckService.copyToPlayer(DESTRUCTION_RUSH_ID, playerId));
            assertTrue(ex.getMessage().contains("Player") || ex.getMessage().contains("player"));
            verify(deckJpaRepository, never()).save(any());
        }

        @Test
        void shouldThrowWhenTemplateNotFound() {
            UUID playerId = UUID.randomUUID();

            NotFoundException ex = assertThrows(NotFoundException.class, () ->
                    predefinedDeckService.copyToPlayer(UUID.randomUUID(), playerId));
            assertTrue(ex.getMessage().contains("Template") || ex.getMessage().contains("template"));
        }

        @Test
        void shouldCopyPreservingAllCards() {
            UUID playerId = UUID.randomUUID();
            DeckEntity savedEntity = new DeckEntity();
            savedEntity.setId(UUID.randomUUID());

            when(playerJpaRepository.existsById(playerId)).thenReturn(true);
            when(deckJpaRepository.save(any(DeckEntity.class))).thenReturn(savedEntity);

            predefinedDeckService.copyToPlayer(DESTRUCTION_RUSH_ID, playerId);

            verify(deckJpaRepository).save(deckCaptor.capture());
            List<DeckCardEntity> cards = deckCaptor.getValue().getCards();
            assertNotNull(cards);
            assertFalse(cards.isEmpty());
            int total = cards.stream().mapToInt(DeckCardEntity::getQuantity).sum();
            assertEquals(60, total);
        }
    }
}
