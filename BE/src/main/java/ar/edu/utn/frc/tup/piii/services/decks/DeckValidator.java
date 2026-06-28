package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.TrainerCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationError;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DeckValidator {

    private final CardLookupPort cardLookupPort;

    public DeckValidator(CardLookupPort cardLookupPort) {
        this.cardLookupPort = cardLookupPort;
    }

    public DeckValidationResult validate(List<DeckCard> cards) {
        List<DeckValidationError> errors = new ArrayList<>();

        int totalCards = cards.stream().mapToInt(DeckCard::getQuantity).sum();
        if (totalCards != 60) {
            errors.add(DeckValidationError.DECK_SIZE_INVALID);
        }

        int aceSpecCount = 0;
        for (DeckCard dc : cards) {
            CardDefinition def = cardLookupPort.getCardById(dc.getCardId());
            if (def instanceof TrainerCardDefinition trainer && trainer.isAceSpec()) {
                aceSpecCount += dc.getQuantity();
            }
        }
        if (aceSpecCount > 1) {
            errors.add(DeckValidationError.ACE_SPEC_LIMIT_EXCEEDED);
        }

        Map<String, Integer> cardCounts = cards.stream()
                .collect(Collectors.toMap(DeckCard::getCardId, DeckCard::getQuantity));
        for (Map.Entry<String, Integer> entry : cardCounts.entrySet()) {
            if (entry.getValue() > 4) {
                CardDefinition def = cardLookupPort.getCardById(entry.getKey());
                if (def instanceof EnergyCardDefinition energy && energy.getEnergyCardType() == EnergyCardType.BASIC) {
                    continue;
                }
                if (def instanceof PokemonCardDefinition) {
                    continue;
                }
                errors.add(DeckValidationError.MORE_THAN_4_COPIES);
                break;
            }
        }

        Map<String, Integer> pokemonNameCounts = new java.util.HashMap<>();
        for (DeckCard dc : cards) {
            CardDefinition def = cardLookupPort.getCardById(dc.getCardId());
            if (def instanceof PokemonCardDefinition) {
                String canonicalName = CardNameNormalizer.normalize(def.getName());
                pokemonNameCounts.merge(canonicalName, dc.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> nameEntry : pokemonNameCounts.entrySet()) {
            if (nameEntry.getValue() > 4) {
                errors.add(DeckValidationError.MORE_THAN_4_COPIES);
                break;
            }
        }

        // Special Energy: also validate by card name (same card may have different IDs)
        Map<String, Integer> specialEnergyNameCounts = new java.util.HashMap<>();
        for (DeckCard dc : cards) {
            CardDefinition def = cardLookupPort.getCardById(dc.getCardId());
            if (def instanceof EnergyCardDefinition energy
                    && energy.getEnergyCardType() == EnergyCardType.SPECIAL) {
                String name = def.getName() != null ? def.getName() : def.getId();
                specialEnergyNameCounts.merge(name, dc.getQuantity(), Integer::sum);
            }
        }
        for (Map.Entry<String, Integer> nameEntry : specialEnergyNameCounts.entrySet()) {
            if (nameEntry.getValue() > 4) {
                errors.add(DeckValidationError.MORE_THAN_4_COPIES);
                break;
            }
        }

        boolean hasBasicPokemon = cards.stream().anyMatch(this::isBasicPokemon);
        if (!hasBasicPokemon) {
            errors.add(DeckValidationError.MISSING_BASIC_POKEMON);
        }

        return new DeckValidationResult(errors.isEmpty(), errors);
    }

    private boolean isBasicPokemon(DeckCard card) {
        CardDefinition def = cardLookupPort.getCardById(card.getCardId());
        if (def instanceof PokemonCardDefinition pokemon) {
            return pokemon.getStage() == null || "BASIC".equalsIgnoreCase(pokemon.getStage());
        }
        return false;
    }
}
