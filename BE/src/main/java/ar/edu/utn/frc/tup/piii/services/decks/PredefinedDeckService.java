package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.dtos.decks.PredefinedDeckCardEntry;
import ar.edu.utn.frc.tup.piii.dtos.decks.PredefinedDeckTemplate;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckCardResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PredefinedDeckService {

    private static final UUID DESTRUCTION_RUSH_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID RESILIENT_LIFE_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private final Map<UUID, PredefinedDeckTemplate> templates;

    private final DeckJpaRepository deckJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;

    public PredefinedDeckService(DeckJpaRepository deckJpaRepository,
                                 PlayerJpaRepository playerJpaRepository) {
        this.deckJpaRepository = deckJpaRepository;
        this.playerJpaRepository = playerJpaRepository;
        this.templates = buildTemplates();
    }

    public List<PredefinedDeckTemplate> getAllTemplates() {
        return List.copyOf(templates.values());
    }

    public PredefinedDeckTemplate getTemplateById(UUID id) {
        if (id == null) {
            throw new NotFoundException("Predefined deck template not found: null");
        }
        PredefinedDeckTemplate template = templates.get(id);
        if (template == null) {
            throw new NotFoundException("Predefined deck template not found: " + id);
        }
        return template;
    }

    public List<DeckResponse> getAllAsResponse() {
        return templates.values().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DeckResponse copyToPlayer(UUID templateId, UUID playerId) {
        PredefinedDeckTemplate template = getTemplateById(templateId);
        if (!playerJpaRepository.existsById(playerId)) {
            throw new NotFoundException("Player not found: " + playerId);
        }

        DeckEntity entity = new DeckEntity();
        entity.setName(template.name());
        entity.setOwnerPlayer(playerJpaRepository.getReferenceById(playerId));
        entity.setSource("USER");
        entity.setValid(false);
        entity.setValidationErrors("[]");

        List<DeckCardEntity> cardEntities = template.cards().stream()
                .map(entry -> {
                    DeckCardEntity ce = new DeckCardEntity();
                    ce.setDeck(entity);
                    ce.setCardId(entry.cardId());
                    ce.setQuantity(entry.quantity());
                    return ce;
                })
                .collect(Collectors.toList());
        entity.setCards(cardEntities);
        entity.setMainCardId(template.mainCardId());

        DeckEntity saved = deckJpaRepository.save(entity);
        return toResponse(saved);
    }

    private DeckResponse toResponse(PredefinedDeckTemplate template) {
        List<DeckCardResponse> cardResponses = template.cards().stream()
                .map(entry -> new DeckCardResponse(
                        entry.cardId(),
                        entry.name(),
                        entry.quantity(),
                        entry.supertype(),
                        "ENERGY".equals(entry.supertype()),
                        Collections.emptyList(),
                        null
                ))
                .collect(Collectors.toList());
        return new DeckResponse(
                template.id().toString(),
                template.name(),
                null,
                "PREDEFINED",
                cardResponses.stream().mapToInt(DeckCardResponse::quantity).sum(),
                true,
                template.mainCardId(),
                buildImageUrl(template.mainCardId()),
                cardResponses,
                new DeckValidationResponse(true, Collections.emptyList()),
                null
        );
    }

    private DeckResponse toResponse(DeckEntity entity) {
        List<DeckCardResponse> cardResponses = entity.getCards() != null
                ? entity.getCards().stream()
                  .map(ce -> new DeckCardResponse(
                          ce.getCardId(),
                          ce.getCardId(),
                          ce.getQuantity(),
                          supertypeOf(ce.getCardId()),
                          false,
                          Collections.emptyList(),
                          null
                  ))
                  .collect(Collectors.toList())
                : Collections.emptyList();
        return new DeckResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getOwnerPlayer() != null ? entity.getOwnerPlayer().getId().toString() : null,
                entity.getSource(),
                cardResponses.stream().mapToInt(DeckCardResponse::quantity).sum(),
                entity.getValid(),
                entity.getMainCardId(),
                entity.getMainCardId() != null ? buildImageUrl(entity.getMainCardId()) : null,
                cardResponses,
                new DeckValidationResponse(entity.getValid(), Collections.emptyList()),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    private static String supertypeOf(String cardId) {
        if (cardId == null) return "UNKNOWN";
        int num = Integer.parseInt(cardId.replaceAll("\\D", ""));
        if (num >= 130) return "ENERGY";
        if (num >= 100) return "TRAINER";
        return "POKEMON";
    }

    private static String buildImageUrl(String cardId) {
        if (cardId == null || !cardId.contains("-")) return null;
        String[] parts = cardId.split("-", 2);
        return "https://images.pokemontcg.io/" + parts[0] + "/" + parts[1] + ".png";
    }

    private static Map<UUID, PredefinedDeckTemplate> buildTemplates() {
        Map<UUID, PredefinedDeckTemplate> map = new LinkedHashMap<>();
        map.put(DESTRUCTION_RUSH_ID, new PredefinedDeckTemplate(
                DESTRUCTION_RUSH_ID, "Destruction Rush", "xy1-78",
                List.of(
                        entry("xy1-78", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-71", "Dark Pokémon", "POKEMON", 1),
                        entry("xy1-70", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-69", "Dark Pokémon", "POKEMON", 3),
                        entry("xy1-76", "Dark Pokémon", "POKEMON", 1),
                        entry("xy1-74", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-72", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-59", "Fighting Pokémon", "POKEMON", 1),
                        entry("xy1-58", "Fighting Pokémon", "POKEMON", 2),
                        entry("xy1-66", "Fighting Pokémon", "POKEMON", 2),
                        entry("xy1-65", "Fighting Pokémon", "POKEMON", 2),
                        entry("xy1-60", "Fighting Pokémon", "POKEMON", 2),
                        entry("xy1-109", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-108", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-102", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-101", "Dark Pokémon", "POKEMON", 2),
                        entry("xy1-127", "Tierno", "TRAINER", 3),
                        entry("xy1-116", "Shauna", "TRAINER", 1),
                        entry("xy1-118", "Great Ball", "TRAINER", 1),
                        entry("xy1-128", "Trainer", "TRAINER", 2),
                        entry("xy1-120", "Super Potion", "TRAINER", 1),
                        entry("xy1-123", "Trainer", "TRAINER", 2),
                        entry("xy1-125", "Trainer", "TRAINER", 1),
                        entry("xy1-126", "Trainer", "TRAINER", 1),
                        entry("xy1-138", "Darkness Energy", "ENERGY", 12),
                        entry("xy1-137", "Fighting Energy", "ENERGY", 6)
                )
        ));
        map.put(RESILIENT_LIFE_ID, new PredefinedDeckTemplate(
                RESILIENT_LIFE_ID, "Resilient Life", "xy1-96",
                List.of(
                        entry("xy1-96", "Psychic Pokémon", "POKEMON", 2),
                        entry("xy1-93", "Psychic Pokémon", "POKEMON", 1),
                        entry("xy1-92", "Psychic Pokémon", "POKEMON", 2),
                        entry("xy1-95", "Psychic Pokémon", "POKEMON", 1),
                        entry("xy1-94", "Psychic Pokémon", "POKEMON", 2),
                        entry("xy1-91", "Psychic Pokémon", "POKEMON", 2),
                        entry("xy1-87", "Psychic Pokémon", "POKEMON", 2),
                        entry("xy1-53", "Grass Pokémon", "POKEMON", 1),
                        entry("xy1-52", "Grass Pokémon", "POKEMON", 2),
                        entry("xy1-51", "Grass Pokémon", "POKEMON", 3),
                        entry("xy1-54", "Grass Pokémon", "POKEMON", 2),
                        entry("xy1-56", "Grass Pokémon", "POKEMON", 2),
                        entry("xy1-47", "Grass Pokémon", "POKEMON", 2),
                        entry("xy1-49", "Grass Pokémon", "POKEMON", 2),
                        entry("xy1-99", "Pokémon", "POKEMON", 2),
                        entry("xy1-98", "Pokémon", "POKEMON", 2),
                        entry("xy1-127", "Tierno", "TRAINER", 2),
                        entry("xy1-116", "Shauna", "TRAINER", 1),
                        entry("xy1-117", "Shauna", "TRAINER", 1),
                        entry("xy1-118", "Great Ball", "TRAINER", 2),
                        entry("xy1-120", "Super Potion", "TRAINER", 1),
                        entry("xy1-123", "Trainer", "TRAINER", 2),
                        entry("xy1-122", "Trainer", "TRAINER", 1),
                        entry("xy1-125", "Trainer", "TRAINER", 1),
                        entry("xy1-128", "Trainer", "TRAINER", 1),
                        entry("xy1-140", "Grass Energy", "ENERGY", 11),
                        entry("xy1-136", "Psychic Energy", "ENERGY", 7)
                )
        ));
        return map;
    }

    private static PredefinedDeckCardEntry entry(String cardId, String name, String supertype, int quantity) {
        return new PredefinedDeckCardEntry(cardId, name, supertype, quantity);
    }
}