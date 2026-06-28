package ar.edu.utn.frc.tup.piii.services.cards;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSyncResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;
import ar.edu.utn.frc.tup.piii.mappers.cards.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CardCacheSyncService")
class CardCacheSyncServiceTest {

    @Mock
    private PokemonTcgApiClient pokemonTcgApiClient;
    @Mock
    private CardJpaRepository cardJpaRepository;
    @Mock
    private CardMapper cardMapper;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private Cache cache;

    private CardCacheSyncService cardCacheSyncService;

    @BeforeEach
    void setUp() {
        cardCacheSyncService = new CardCacheSyncService(pokemonTcgApiClient, cardJpaRepository, cardMapper, cacheManager);
    }

    @Nested
    @DisplayName("syncAll() — cartas nuevas")
    class NewCardsTests {

        @Test
        void shouldProcessAllCardsAsNew() {
            PokemonTcgApiCardDto dto1 = mock(PokemonTcgApiCardDto.class);

            PokemonTcgApiCardDto dto2 = mock(PokemonTcgApiCardDto.class);


            CardEntity entity1 = new CardEntity();
            entity1.setId("xy1-1");
            CardEntity entity2 = new CardEntity();
            entity2.setId("xy1-2");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto1, dto2));
            when(cardMapper.toCardEntity(dto1)).thenReturn(entity1);
            when(cardMapper.toCardEntity(dto2)).thenReturn(entity2);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cardJpaRepository.existsById("xy1-2")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertAll("sync new cards",
                    () -> assertTrue(result.success()),
                    () -> assertEquals(2, result.newCards()),
                    () -> assertEquals(0, result.updatedCards())
            );
            verify(cardJpaRepository, times(2)).save(any(CardEntity.class));
            verify(cache).clear();
        }

        @Test
        void shouldHandleZeroCardsFromApi() {
            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(Collections.emptyList());
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertAll("zero cards",
                    () -> assertTrue(result.success()),
                    () -> assertEquals(0, result.newCards()),
                    () -> assertEquals(0, result.updatedCards())
            );
            verify(cardJpaRepository, never()).save(any());
            verify(cache).clear();
        }

        @Test
        void shouldHandleSingleCard() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertEquals(1, result.newCards());
            verify(cardJpaRepository).save(entity);
        }
    }

    @Nested
    @DisplayName("syncAll() — cartas existentes")
    class ExistingCardsTests {

        @Test
        void shouldProcessAllCardsAsUpdates() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(true);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertEquals(0, result.newCards());
            assertEquals(1, result.updatedCards());
            verify(cardJpaRepository).save(entity);
        }

        @Test
        void shouldMixNewAndUpdated() {
            PokemonTcgApiCardDto dto1 = mock(PokemonTcgApiCardDto.class);

            PokemonTcgApiCardDto dto2 = mock(PokemonTcgApiCardDto.class);


            CardEntity entity1 = new CardEntity();
            entity1.setId("xy1-1");
            CardEntity entity2 = new CardEntity();
            entity2.setId("xy1-2");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto1, dto2));
            when(cardMapper.toCardEntity(dto1)).thenReturn(entity1);
            when(cardMapper.toCardEntity(dto2)).thenReturn(entity2);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cardJpaRepository.existsById("xy1-2")).thenReturn(true);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertEquals(1, result.newCards());
            assertEquals(1, result.updatedCards());
            verify(cardJpaRepository, times(2)).save(any());
        }

        @Test
        void shouldUpdateAllWhenAllPreExist() {
            PokemonTcgApiCardDto dto1 = mock(PokemonTcgApiCardDto.class);

            PokemonTcgApiCardDto dto2 = mock(PokemonTcgApiCardDto.class);


            CardEntity entity1 = new CardEntity();
            entity1.setId("xy1-1");
            CardEntity entity2 = new CardEntity();
            entity2.setId("xy1-2");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto1, dto2));
            when(cardMapper.toCardEntity(dto1)).thenReturn(entity1);
            when(cardMapper.toCardEntity(dto2)).thenReturn(entity2);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(true);
            when(cardJpaRepository.existsById("xy1-2")).thenReturn(true);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertEquals(0, result.newCards());
            assertEquals(2, result.updatedCards());
        }
    }

    @Nested
    @DisplayName("syncAll() — cache")
    class CacheTests {

        @Test
        void shouldClearCacheAfterSuccessfulSync() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            cardCacheSyncService.syncAll();

            verify(cache).clear();
        }

        @Test
        void shouldNotClearCacheWhenApiFails() {
            when(pokemonTcgApiClient.fetchAllCards()).thenThrow(new RuntimeException("API failure"));

            cardCacheSyncService.syncAll();

            verify(cache, never()).clear();
            verifyNoInteractions(cacheManager);
        }

        @Test
        void shouldHandleCacheManagerReturningNull() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(null);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertTrue(result.success());
            verify(cache, never()).clear();
        }

        @Test
        void shouldHandleCacheGetCacheException() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenThrow(new RuntimeException("Cache unavailable"));

            assertDoesNotThrow(() -> cardCacheSyncService.syncAll());
        }
    }

    @Nested
    @DisplayName("syncAll() — manejo de errores")
    class ErrorHandlingTests {

        @Test
        void shouldReturnFailureWhenApiThrows() {
            when(pokemonTcgApiClient.fetchAllCards()).thenThrow(new RuntimeException("API is down"));

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertAll("API failure response",
                    () -> assertFalse(result.success()),
                    () -> assertTrue(result.message().contains("API is down")),
                    () -> assertEquals(0, result.newCards()),
                    () -> assertEquals(0, result.updatedCards())
            );
            verify(cardJpaRepository, never()).save(any());
        }

        @Test
        void shouldContinueOnPerCardMappingError() {
            PokemonTcgApiCardDto dto1 = mock(PokemonTcgApiCardDto.class);

            PokemonTcgApiCardDto dto2 = mock(PokemonTcgApiCardDto.class);


            CardEntity entity1 = new CardEntity();
            entity1.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto1, dto2));
            when(cardMapper.toCardEntity(dto1)).thenReturn(entity1);
            when(cardMapper.toCardEntity(dto2)).thenThrow(new RuntimeException("Mapping failed for dto2"));
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertTrue(result.success());
            assertEquals(1, result.newCards());
            assertEquals(0, result.updatedCards());
            verify(cardJpaRepository, times(1)).save(any());
        }

        @Test
        void shouldContinueAfterDbSaveError() {
            PokemonTcgApiCardDto dto1 = mock(PokemonTcgApiCardDto.class);

            PokemonTcgApiCardDto dto2 = mock(PokemonTcgApiCardDto.class);


            CardEntity entity1 = new CardEntity();
            entity1.setId("xy1-1");
            CardEntity entity2 = new CardEntity();
            entity2.setId("xy1-2");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto1, dto2));
            when(cardMapper.toCardEntity(dto1)).thenReturn(entity1);
            when(cardMapper.toCardEntity(dto2)).thenReturn(entity2);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            doThrow(new RuntimeException("DB error")).when(cardJpaRepository).save(entity1);
            when(cardJpaRepository.existsById("xy1-2")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertTrue(result.success());
            verify(cardJpaRepository, times(1)).save(entity2);
        }

        @Test
        void shouldHandleAllCardsFailingWithGracefulResponse() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);


            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenThrow(new RuntimeException("Total failure"));

            CardSyncResponse result = cardCacheSyncService.syncAll();

            assertTrue(result.success());
            assertEquals(0, result.newCards());
            assertEquals(0, result.updatedCards());
        }

        @Test
        void shouldHandleInterruptedSleepDuringRetry() {
            when(pokemonTcgApiClient.fetchAllCards()).thenThrow(new RuntimeException("Timeout"));

            assertDoesNotThrow(() -> cardCacheSyncService.syncAll());
        }
    }

    @Nested
    @DisplayName("synchronizeAllCards() — event listener")
    class SynchronizeAllCardsTests {

        @Test
        void shouldInvokeSyncAllOnEvent() {
            PokemonTcgApiCardDto dto = mock(PokemonTcgApiCardDto.class);

            CardEntity entity = new CardEntity();
            entity.setId("xy1-1");

            when(pokemonTcgApiClient.fetchAllCards()).thenReturn(List.of(dto));
            when(cardMapper.toCardEntity(dto)).thenReturn(entity);
            when(cardJpaRepository.existsById("xy1-1")).thenReturn(false);
            when(cacheManager.getCache("cards")).thenReturn(cache);

            cardCacheSyncService.syncAll();

            verify(pokemonTcgApiClient).fetchAllCards();
        }
    }
}
