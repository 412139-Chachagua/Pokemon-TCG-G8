package ar.edu.utn.frc.tup.piii.services.cards;

import ar.edu.utn.frc.tup.piii.clients.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.dtos.cards.CardSyncResponse;
import ar.edu.utn.frc.tup.piii.dtos.cards.PokemonTcgApiCardDto;
import ar.edu.utn.frc.tup.piii.mappers.cards.CardMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardCacheSyncService {

    private final PokemonTcgApiClient pokemonTcgApiClient;
    private final CardJpaRepository cardJpaRepository;
    private final CardMapper cardMapper;
    private final CacheManager cacheManager;

    @EventListener(ApplicationReadyEvent.class)
    public void synchronizeAllCards() {
        if (cardJpaRepository.count() > 0) {
            log.info("Cartas ya sincronizadas, skip async sync");
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                syncAll();
            } catch (Exception e) {
                log.error("Async sync failed: {}", e.getMessage(), e);
            }
        });
    }

    @Scheduled(fixedRate = 86400000)
    public void scheduledSync() {
        log.info("=== Sincronizacion diaria de cartas ===");
        try {
            syncAll();
        } catch (Exception e) {
            log.error("Scheduled sync failed: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public boolean syncCardById(String cardId) {
        try {
            var dto = pokemonTcgApiClient.fetchCardById(cardId);
            if (dto == null) {
                log.warn("Card {} not found in external API", cardId);
                return false;
            }
            CardEntity entity = cardMapper.toCardEntity(dto);
            cardJpaRepository.save(entity);
            org.springframework.cache.Cache cardsCache = cacheManager.getCache("cards");
            if (cardsCache != null) {
                cardsCache.evictIfPresent(cardId);
            }
            log.info("Card {} synced successfully", cardId);
            return true;
        } catch (Exception e) {
            log.error("Error syncing card {}: {}", cardId, e.getMessage(), e);
            return false;
        }
    }

    @Transactional
    public CardSyncResponse syncAll() {
        log.info("=== Iniciando sincronizacion de cartas Pokemon TCG - Set XY1 ===");
        int totalNew = 0;
        int totalUpdated = 0;

        try {
            List<PokemonTcgApiCardDto> allCards = pokemonTcgApiClient.fetchAllCards();

            log.info("Total cartas obtenidas: {}", allCards.size());

            for (PokemonTcgApiCardDto dto : allCards) {
                try {
                    CardEntity entity = cardMapper.toCardEntity(dto);
                    if (cardJpaRepository.existsById(entity.getId())) {
                        cardJpaRepository.save(entity);
                        totalUpdated++;
                    } else {
                        cardJpaRepository.save(entity);
                        totalNew++;
                    }
                } catch (Exception e) {
                    log.error("Error guardando card {}: {}", dto.id(), e.getMessage(), e);
                }
            }

            org.springframework.cache.Cache cardsCache = cacheManager.getCache("cards");
            if (cardsCache != null) {
                cardsCache.clear();
            }
            log.info("=== Sincronizacion completada con exito ===");
            return new CardSyncResponse(true, "Sync completed.", totalNew, totalUpdated);
        } catch (Exception e) {
            log.error("=== FALLO la sincronizacion: {} ===", e.getMessage(), e);
            return new CardSyncResponse(false, "Sync failed: " + e.getMessage(), totalNew, totalUpdated);
        }
    }
}
