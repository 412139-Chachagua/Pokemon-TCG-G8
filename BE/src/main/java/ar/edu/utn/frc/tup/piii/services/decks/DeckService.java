package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.dtos.decks.*;
import ar.edu.utn.frc.tup.piii.exceptions.NotFoundException;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.mappers.decks.DeckMapper;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.DeckJpaRepository;
import ar.edu.utn.frc.tup.piii.repositories.jpa.PlayerJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeckService {

    private static final Pattern NAME_ONLY_PATTERN =
            Pattern.compile("^(\\d+)\\s+(.+)$");

    private final DeckJpaRepository deckJpaRepository;
    private final DeckValidator deckValidator;
    private final DeckMapper deckMapper;
    private final PlayerJpaRepository playerJpaRepository;
    private final PdfImportService pdfImportService;
    private final PdfExportService pdfExportService;
    private final CardJpaRepository cardJpaRepository;

    public DeckService(DeckJpaRepository deckJpaRepository, DeckValidator deckValidator,
                       DeckMapper deckMapper, PlayerJpaRepository playerJpaRepository,
                       PdfImportService pdfImportService, PdfExportService pdfExportService,
                       CardJpaRepository cardJpaRepository) {
        this.deckJpaRepository = deckJpaRepository;
        this.deckValidator = deckValidator;
        this.deckMapper = deckMapper;
        this.playerJpaRepository = playerJpaRepository;
        this.pdfImportService = pdfImportService;
        this.pdfExportService = pdfExportService;
        this.cardJpaRepository = cardJpaRepository;
    }

    @Transactional
    public DeckResponse createDeck(CreateDeckRequest request) {
        if (request.playerId() != null) {
            UUID playerId = UUID.fromString(request.playerId());
            if (!playerJpaRepository.existsById(playerId)) {
                throw new ValidationException("Player not found: " + playerId);
            }
        }
        DeckEntity entity = deckMapper.toEntity(request);
        if (request.playerId() != null) {
            entity.setOwnerPlayer(playerJpaRepository.getReferenceById(UUID.fromString(request.playerId())));
        }
        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
        if (!validation.isValid()) {
            String msg = "Cannot create deck: " + validation.getErrors().stream()
                    .map(Enum::name).collect(Collectors.joining(", "));
            throw new ValidationException(msg);
        }
        entity.setValid(true);
        entity = deckJpaRepository.save(entity);
        return deckMapper.toResponse(entity, validation);
    }

    public DeckResponse getDeck(UUID deckId) {
        DeckEntity entity = findEntity(deckId);
        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
        return deckMapper.toResponse(entity, validation);
    }

    @Transactional
    public DeckResponse updateDeck(UUID deckId, UpdateDeckRequest request) {
        DeckEntity entity = findEntity(deckId);
        deckMapper.updateEntity(entity, request);
        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
        if (!validation.isValid()) {
            String msg = "Cannot update deck: " + validation.getErrors().stream()
                    .map(Enum::name).collect(Collectors.joining(", "));
            throw new ValidationException(msg);
        }
        entity.setValid(true);
        entity = deckJpaRepository.save(entity);
        return deckMapper.toResponse(entity, validation);
    }

    @Transactional
    public void deleteDeck(UUID deckId) {
        DeckEntity entity = findEntity(deckId);
        deckJpaRepository.delete(entity);
    }

    public List<DeckResponse> listDecksByPlayer(UUID playerId) {
        return deckJpaRepository.findByOwnerPlayerId(playerId).stream()
                .map(entity -> {
                    DeckValidationResult validation = deckValidator.validate(
                            entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
                    return deckMapper.toResponse(entity, validation);
                })
                .collect(Collectors.toList());
    }

    public DeckValidationResponse validateDeck(UUID deckId) {
        DeckEntity entity = findEntity(deckId);
        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
        return deckMapper.toValidationResponse(validation);
    }

    public DeckValidationResponse validateCards(ValidateDeckRequest request) {
        List<DeckCard> deckCards = request.cards().stream()
                .map(c -> {
                    DeckCard card = new DeckCard();
                    card.setCardId(c.cardId());
                    card.setQuantity(c.quantity());
                    return card;
                })
                .collect(Collectors.toList());
        DeckValidationResult validation = deckValidator.validate(deckCards);
        return deckMapper.toValidationResponse(validation);
    }

    public Deck loadDeckDomain(UUID deckId) {
        DeckEntity entity = findEntity(deckId);
        return deckMapper.toDomain(entity);
    }

    private DeckEntity findEntity(UUID deckId) {
        return deckJpaRepository.findById(deckId)
                .orElseThrow(() -> new NotFoundException("Deck not found: " + deckId));
    }

    @Transactional
    public List<DeckResponse> importDecks(String content, String format, UUID playerId, String filename) {
        ensureCardCatalog();
        List<ImportDeckRequest> parsed = switch (format.toLowerCase()) {
            case "json" -> parseJson(content);
            case "txt" -> parseTxt(content, filename);
            default -> throw new IllegalArgumentException("Unsupported format: " + format);
        };
        return parsed.stream()
                .map(req -> createDeckFromImport(req, playerId))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<DeckResponse> importPdfDecks(MultipartFile file, UUID playerId) {
        ensureCardCatalog();
        List<ImportDeckRequest> parsed = pdfImportService.extractDecksFromPdf(file);
        return parsed.stream()
                .map(req -> createDeckFromImport(req, playerId))
                .collect(Collectors.toList());
    }

    public byte[] exportDeckPdf(UUID deckId) {
        DeckEntity entity = findEntity(deckId);
        return pdfExportService.export(entity);
    }

    private void ensureCardCatalog() {
        if (cardJpaRepository.count() == 0) {
            throw new ValidationException(
                    "Card catalog is not available. Please wait for card synchronization to complete and try again.");
        }
    }

    private DeckResponse createDeckFromImport(ImportDeckRequest req, UUID playerId) {
        CreateDeckRequest createReq = new CreateDeckRequest(req.name(), playerId.toString(), req.cards());
        DeckEntity entity = deckMapper.toEntity(createReq);
        entity.setOwnerPlayer(playerJpaRepository.getReferenceById(playerId));
        DeckValidationResult validation = deckValidator.validate(
                entity.getCards().stream().map(deckMapper::toDomainCard).collect(Collectors.toList()));
        entity.setValid(validation.isValid());
        entity.setValidationErrors(serializeValidationErrors(validation.getErrors()));
        entity = deckJpaRepository.save(entity);
        return deckMapper.toResponse(entity, validation);
    }

    private String serializeValidationErrors(List<DeckValidationError> errors) {
        return errors.stream()
                .map(Enum::name)
                .collect(Collectors.joining("\",\"", "[\"", "\"]"));
    }

    private List<ImportDeckRequest> parseJson(String content) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(content);
            List<ImportDeckRequest> result = new ArrayList<>();
            if (root.isArray()) {
                for (JsonNode node : root) {
                    result.add(readSingleDeckFromJson(node));
                }
            } else {
                result.add(readSingleDeckFromJson(root));
            }
            return result;
        } catch (Exception e) {
            throw new ValidationException("Invalid JSON format: " + e.getMessage());
        }
    }

    private ImportDeckRequest readSingleDeckFromJson(JsonNode node) {
        String name = node.has("name") ? node.get("name").asText() : "Imported Deck";
        List<CreateDeckRequest.DeckCardRequest> cards = new ArrayList<>();
        JsonNode cardsNode = node.get("cards");
        if (cardsNode != null && cardsNode.isArray()) {
            for (JsonNode card : cardsNode) {
                String cardId = card.get("cardId").asText();
                int quantity = card.has("quantity") ? card.get("quantity").asInt() : 1;
                cards.add(new CreateDeckRequest.DeckCardRequest(cardId, quantity));
            }
        }
        return new ImportDeckRequest(name, cards);
    }

    private List<ImportDeckRequest> parseTxt(String content, String filename) {
        List<ImportDeckRequest> result = new ArrayList<>();
        List<CreateDeckRequest.DeckCardRequest> currentCards = new ArrayList<>();
        String currentName = filename != null ? filename.replaceFirst("\\.[^.]+$", "") : "Imported Deck";
        String[] lines = content.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("*")) continue;
            if (trimmed.startsWith("# ")) {
                if (!currentCards.isEmpty()) {
                    result.add(new ImportDeckRequest(currentName, currentCards));
                    currentCards = new ArrayList<>();
                }
                currentName = trimmed.substring(2).trim();
                continue;
            }
            String[] parts = trimmed.split(":");
            if (parts.length == 2 && !parts[0].trim().isEmpty()) {
                String cardId = parts[0].trim();
                int quantity = parseIntSafely(parts[1].trim(), 1);
                currentCards.add(new CreateDeckRequest.DeckCardRequest(cardId, quantity));
            } else {
                Matcher matcher = NAME_ONLY_PATTERN.matcher(trimmed);
                if (matcher.matches()) {
                    int quantity = parseIntSafely(matcher.group(1), 1);
                    String name = matcher.group(2).trim();
                    List<CardEntity> cards = cardJpaRepository.findByNameIgnoreCase(name);
                    if (cards.isEmpty()) {
                        throw new ValidationException("Card not found: " + name);
                    }
                    currentCards.add(new CreateDeckRequest.DeckCardRequest(cards.get(0).getId(), quantity));
                }
            }
        }
        if (!currentCards.isEmpty()) {
            result.add(new ImportDeckRequest(currentName, currentCards));
        }
        if (result.isEmpty()) {
            result.add(new ImportDeckRequest(currentName, currentCards));
        }
        return result;
    }

    private int parseIntSafely(String s, int defaultValue) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}