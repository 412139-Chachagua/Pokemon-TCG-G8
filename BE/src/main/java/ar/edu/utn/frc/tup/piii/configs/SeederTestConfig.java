package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.dtos.decks.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.repositories.entities.PlayerEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.UserEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.UserJpaRepository;
import ar.edu.utn.frc.tup.piii.services.cards.CardCacheSyncService;
import ar.edu.utn.frc.tup.piii.services.decks.DeckService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class SeederTestConfig {

    private final UserJpaRepository userJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;
    private final DeckJpaRepository deckJpaRepository;
    private final CardJpaRepository cardJpaRepository;
    private final CardCacheSyncService cardCacheSyncService;
    private final DeckService deckService;

    public SeederTestConfig(UserJpaRepository userJpaRepository,
                            PlayerJpaRepository playerJpaRepository,
                            DeckJpaRepository deckJpaRepository,
                            CardJpaRepository cardJpaRepository,
                            CardCacheSyncService cardCacheSyncService,
                            DeckService deckService) {
        this.userJpaRepository = userJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
        this.deckJpaRepository = deckJpaRepository;
        this.cardJpaRepository = cardJpaRepository;
        this.cardCacheSyncService = cardCacheSyncService;
        this.deckService = deckService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        log.info("=== SeederTestConfig: ensuring seed users exist ===");

        ensureCardCatalog();

        ensureSeedUser("ash@pokemon.com", "password123", "Ash Ketchum", fireDeckCards());
        ensureSeedUser("misty@pokemon.com", "password456", "Misty", fireDeckCards());
    }

    private void ensureCardCatalog() {
        if (cardJpaRepository.count() > 0) {
            return;
        }
        log.info("Card catalog is empty, triggering synchronous sync...");
        try {
            cardCacheSyncService.syncAll();
        } catch (Exception e) {
            log.warn("Failed to sync card catalog: {}. Decks will be created on next startup.", e.getMessage());
        }
    }

    private void ensureSeedUser(String email, String password, String displayName,
                                List<CreateDeckRequest.DeckCardRequest> deckCards) {
        Optional<UserEntity> existing = userJpaRepository.findByEmail(email);

        UserEntity user;
        if (existing.isPresent()) {
            user = existing.get();
            log.info("User '{}' already exists (id={}).", email, user.getId());
        } else {
            log.info("Creating seed user '{}'...", email);
            user = createUser(email, password, displayName);
            log.info("User '{}' created with id={}.", email, user.getId());
        }

        if (!hasCards()) {
            log.warn("Card catalog unavailable. Deck for '{}' will be created on next startup.", email);
            return;
        }

        PlayerEntity player = user.getPlayer();
        if (player == null) {
            log.warn("User '{}' has no player profile. Skipping deck creation.", email);
            return;
        }

        UUID playerId = player.getId();
        if (!deckJpaRepository.findByOwnerPlayerId(playerId).isEmpty()) {
            log.info("User '{}' already has at least one deck. Skipping deck creation.", email);
            return;
        }

        CreateDeckRequest request = new CreateDeckRequest(
                displayName + "'s Deck",
                playerId.toString(),
                deckCards
        );
        deckService.createDeck(request);
        log.info("Deck created for user '{}' (playerId={}).", email, playerId);
    }

    private UserEntity createUser(String email, String password, String displayName) {
        UserEntity user = new UserEntity();
        user.setUsername(email);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("PLAYER");
        user.setStatus("ACTIVE");
        user = userJpaRepository.save(user);

        PlayerEntity player = new PlayerEntity();
        player.setUser(user);
        player.setDisplayName(displayName);
        playerJpaRepository.save(player);

        user.setPlayer(player);

        return user;
    }

    private boolean hasCards() {
        return cardJpaRepository.count() > 0;
    }

    private List<CreateDeckRequest.DeckCardRequest> fireDeckCards() {
        return List.of(
                new CreateDeckRequest.DeckCardRequest("xy1-10", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-11", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-12", 3),
                new CreateDeckRequest.DeckCardRequest("xy1-1", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-133", 18),
                new CreateDeckRequest.DeckCardRequest("xy1-2", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-3", 3),
                new CreateDeckRequest.DeckCardRequest("xy1-4", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-5", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-6", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-7", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-8", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-9", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-13", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-14", 4)
        );
    }

    private List<CreateDeckRequest.DeckCardRequest> waterDeckCards() {
        return List.of(
                new CreateDeckRequest.DeckCardRequest("xy1-15", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-16", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-17", 3),
                new CreateDeckRequest.DeckCardRequest("xy1-18", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-134", 18),
                new CreateDeckRequest.DeckCardRequest("xy1-19", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-20", 3),
                new CreateDeckRequest.DeckCardRequest("xy1-21", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-22", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-23", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-24", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-25", 2),
                new CreateDeckRequest.DeckCardRequest("xy1-26", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-27", 4),
                new CreateDeckRequest.DeckCardRequest("xy1-28", 2)
        );
    }
}
