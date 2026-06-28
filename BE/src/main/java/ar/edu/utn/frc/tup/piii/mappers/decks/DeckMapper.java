package ar.edu.utn.frc.tup.piii.mappers.decks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.decks.Deck;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.dtos.decks.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.repositories.entities.DeckEntity;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeckMapper {

    private final CardLookupPort cardLookupPort;

    public DeckMapper(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    public DeckResponse toResponse(DeckEntity entity, DeckValidationResult validation) {
        List<DeckCardResponse> cardResponses = entity.getCards() != null
                ? entity.getCards().stream().map(this::toCardResponse).collect(Collectors.toList())
                : Collections.emptyList();
        return new DeckResponse(
                entity.getId().toString(),
                entity.getName(),
                entity.getOwnerPlayer() != null ? entity.getOwnerPlayer().getId().toString() : null,
                entity.getSource(),
                cardResponses.stream().mapToInt(DeckCardResponse::quantity).sum(),
                validation.isValid(),
                cardResponses,
                toValidationResponse(validation),
                entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null
        );
    }

    public DeckCardResponse toCardResponse(DeckCardEntity entity) {
        CardDefinition def = cardLookupPort.getCardById(entity.getCardId());
        String name = def != null ? def.getName() : entity.getCardId();
        String supertype = def != null ? def.getSupertype() : "UNKNOWN";
        boolean isBasicEnergy = def instanceof EnergyCardDefinition;
        List<String> subtypes = def != null && def.getSubtypes() != null ? def.getSubtypes() : List.of();
        String stage = def instanceof PokemonCardDefinition pokemon ? pokemon.getStage() : null;
        return new DeckCardResponse(entity.getCardId(), name, entity.getQuantity(), supertype, isBasicEnergy, subtypes, stage);
    }

    public DeckEntity toEntity(CreateDeckRequest request) {
        DeckEntity entity = new DeckEntity();
        entity.setName(request.name());
        // ownerPlayer se setea en DeckService.createDeck()
        entity.setSource("USER");
        entity.setValid(false);
        entity.setValidationErrors("[]");
        if (request.cards() != null) {
            entity.setCards(request.cards().stream()
                    .map(c -> toCardEntity(entity, c))
                    .collect(Collectors.toList()));
        }
        return entity;
    }

    public DeckCardEntity toCardEntity(DeckEntity deck, CreateDeckRequest.DeckCardRequest request) {
        DeckCardEntity entity = new DeckCardEntity();
        entity.setDeck(deck);
        entity.setCardId(request.cardId());
        entity.setQuantity(request.quantity());
        return entity;
    }

    public void updateEntity(DeckEntity entity, UpdateDeckRequest request) {
        entity.setName(request.name());
        entity.getCards().clear();
        if (request.cards() != null) {
            request.cards().forEach(c -> entity.getCards().add(toCardEntity(entity, c)));
        }
    }

    public Deck toDomain(DeckEntity entity) {
        Deck deck = new Deck();
        deck.setId(entity.getId());
        deck.setName(entity.getName());
        deck.setOwnerPlayerId(entity.getOwnerPlayer() != null ? entity.getOwnerPlayer().getId() : null);
        deck.setSource(entity.getSource());
        deck.setCreatedAt(entity.getCreatedAt());
        deck.setUpdatedAt(entity.getUpdatedAt());
        deck.setCards(entity.getCards() != null
                ? entity.getCards().stream().map(this::toDomainCard).collect(Collectors.toList())
                : Collections.emptyList());
        return deck;
    }

    public DeckCard toDomainCard(DeckCardEntity entity) {
        DeckCard card = new DeckCard();
        card.setId(entity.getId());
        card.setDeckId(entity.getDeck() != null ? entity.getDeck().getId() : null);
        card.setCardId(entity.getCardId());
        card.setQuantity(entity.getQuantity());
        return card;
    }

    public DeckValidationResponse toValidationResponse(DeckValidationResult result) {
        List<DeckValidationResponse.DeckValidationError> errors = result.getErrors().stream()
                .map(this::toValidationError)
                .collect(Collectors.toList());
        return new DeckValidationResponse(result.isValid(), errors);
    }

    private DeckValidationResponse.DeckValidationError toValidationError(DeckValidationError error) {
        return switch (error) {
            case DECK_SIZE_INVALID -> new DeckValidationResponse.DeckValidationError(
                    "DECK_SIZE_INVALID",
                    "El mazo debe tener exactamente 60 cartas.",
                    null);
            case MORE_THAN_4_COPIES -> new DeckValidationResponse.DeckValidationError(
                    "MORE_THAN_4_COPIES",
                    "No puede haber m\u00e1s de 4 copias de la misma carta.",
                    null);
            case MISSING_BASIC_POKEMON -> new DeckValidationResponse.DeckValidationError(
                    "MISSING_BASIC_POKEMON",
                    "El mazo debe tener al menos 1 Pok\u00e9mon B\u00e1sico.",
                    null);
            case ACE_SPEC_LIMIT_EXCEEDED -> new DeckValidationResponse.DeckValidationError(
                    "ACE_SPEC_LIMIT_EXCEEDED",
                    "El mazo no puede tener m\u00e1s de 1 carta AS T\u00c1CTICO.",
                    null);
            default -> new DeckValidationResponse.DeckValidationError(
                    "INVALID_DECK",
                    "El mazo no es v\u00e1lido.",
                    null);
        };
    }
}