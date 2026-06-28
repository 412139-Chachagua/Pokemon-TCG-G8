package ar.edu.utn.frc.tup.piii.services.decks;

import ar.edu.utn.frc.tup.piii.domain.decks.DeckCard;
import ar.edu.utn.frc.tup.piii.domain.decks.DeckValidationResult;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckCardResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.decks.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.exceptions.ValidationException;
import ar.edu.utn.frc.tup.piii.repositories.entities.CardEntity;
import ar.edu.utn.frc.tup.piii.repositories.jpa.CardJpaRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RandomDeckService {

    private static final int MAX_RETRIES = 20;
    private static final int DECK_SIZE = 60;
    private static final int MAX_COPIES = 4;
    private static final int MIN_ENERGY = 10;
    private static final int MAX_ENERGY = 15;
    private static final int MIN_POKEMON = 12;
    private static final int MAX_POKEMON = 20;
    private static final int MIN_TRAINERS = 25;
    private static final int MAX_TRAINERS = 35;

    private final CardJpaRepository cardJpaRepository;
    private final CardLookupPort cardLookupPort;
    private final DeckValidator deckValidator;

    public RandomDeckService(CardJpaRepository cardJpaRepository,
                             CardLookupPort cardLookupPort,
                             DeckValidator deckValidator) {
        this.cardJpaRepository = cardJpaRepository;
        this.cardLookupPort = cardLookupPort;
        this.deckValidator = deckValidator;
    }

    public DeckResponse generateRandomDeck() {
        List<CardEntity> allCards = cardJpaRepository.findAll().stream()
                .filter(c -> "xy1".equals(c.getSetCode()))
                .toList();

        List<CardEntity> allPokemon = filterBySupertype(allCards, "POKEMON");
        List<CardEntity> basicPokemonList = allPokemon.stream()
                .filter(c -> "BASIC".equals(c.getPokemonStage()))
                .collect(Collectors.toList());
        List<CardEntity> stage1 = allPokemon.stream()
                .filter(c -> "STAGE_1".equals(c.getPokemonStage()))
                .collect(Collectors.toList());
        List<CardEntity> stage2 = allPokemon.stream()
                .filter(c -> "STAGE_2".equals(c.getPokemonStage()))
                .collect(Collectors.toList());
        List<CardEntity> trainers = filterBySupertype(allCards, "TRAINER");
        List<CardEntity> basicEnergies = filterBySupertype(allCards, "ENERGY").stream()
                .filter(c -> "BASIC".equals(c.getEnergyCardType()))
                .collect(Collectors.toList());

        if (basicPokemonList.isEmpty() || basicEnergies.isEmpty()) {
            throw new ValidationException("No hay suficientes cartas (Pokémon básicos o Energía básica) disponibles para generar un mazo aleatorio.");
        }

        Random random = new Random();

        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                List<DeckCard> deckCards = new ArrayList<>();

                int energyCount = MIN_ENERGY + random.nextInt(MAX_ENERGY - MIN_ENERGY + 1);
                List<EnergyAllocation> energyAllocs = pickEnergy(energyCount, basicEnergies, random);
                for (EnergyAllocation ea : energyAllocs) {
                    addOrIncrement(deckCards, ea.cardId(), ea.count());
                }

                Set<String> energyTypes = energyAllocs.stream()
                        .flatMap(ea -> ea.types().stream())
                        .collect(Collectors.toSet());

                int pokemonTarget = MIN_POKEMON + random.nextInt(MAX_POKEMON - MIN_POKEMON + 1);
                pickPokemon(deckCards, basicPokemonList, stage1, stage2, energyTypes, pokemonTarget, random);

                int currentCount = deckCards.stream().mapToInt(DeckCard::getQuantity).sum();
                int trainerTarget = DECK_SIZE - currentCount;

                if (trainerTarget >= MIN_TRAINERS && trainerTarget <= MAX_TRAINERS) {
                    pickTrainers(deckCards, trainers, trainerTarget, random);
                } else {
                    continue;
                }

                DeckValidationResult validation = deckValidator.validate(deckCards);
                if (validation.isValid()) {
                    return toDeckResponse(deckCards, validation);
                }
            } catch (Exception e) {
            }
        }

        throw new ValidationException("No se pudo generar un mazo valido despues de " + MAX_RETRIES + " intentos.");
    }


    private record EnergyAllocation(String cardId, int count, Set<String> types) {}

    private List<EnergyAllocation> pickEnergy(int totalCount, List<CardEntity> basicEnergies, Random random) {
        Map<String, CardEntity> energyByType = new LinkedHashMap<>();
        for (CardEntity e : basicEnergies) {
            String type = extractEnergyType(e);
            if (type != null && !type.isBlank()) {
                energyByType.putIfAbsent(type, e);
            }
        }

        boolean hasColorless = energyByType.containsKey("COLORLESS");
        int typeCount;
        if (energyByType.size() == 1) {
            typeCount = 1;
        } else if (energyByType.size() == 2) {
            typeCount = hasColorless ? random.nextInt(2) + 1 : 2;
        } else {
            typeCount = random.nextInt(3) + 1;
            if (typeCount == 3 && !hasColorless) {
                typeCount = 2;
            }
        }

        if (typeCount == 3 && !hasColorless) {
            typeCount = 2;
        }
        if (typeCount > energyByType.size()) {
            typeCount = energyByType.size();
        }

        List<String> available = new ArrayList<>(energyByType.keySet());
        Collections.shuffle(available, random);

        List<String> chosenTypes;
        if (typeCount == 1) {
            chosenTypes = List.of(available.get(0));
        } else if (typeCount == 2) {
            chosenTypes = available.subList(0, 2);
        } else {
            List<String> nonColorless = available.stream()
                    .filter(t -> !"COLORLESS".equals(t))
                    .collect(Collectors.toList());
            Collections.shuffle(nonColorless, random);
            chosenTypes = new ArrayList<>(nonColorless.subList(0, Math.min(2, nonColorless.size())));
            chosenTypes.add("COLORLESS");
        }

        List<Integer> counts;
        if (typeCount == 1) {
            counts = List.of(totalCount);
        } else if (typeCount == 2) {
            int[][] splits = {{8, 4}, {7, 5}, {6, 6}};
            int[] split = splits[random.nextInt(splits.length)];
            if (!hasColorless && chosenTypes.contains("COLORLESS") && random.nextBoolean()) {
                counts = List.of(split[1], split[0]);
            } else {
                counts = List.of(split[0], split[1]);
            }
        } else {
            int[][] splits = {{6, 4, 2}, {5, 4, 3}};
            int[] split = splits[random.nextInt(splits.length)];
            counts = new ArrayList<>();
            for (int s : split) counts.add(s);
        }

        List<EnergyAllocation> result = new ArrayList<>();
        for (int i = 0; i < chosenTypes.size(); i++) {
            String type = chosenTypes.get(i);
            int count = counts.get(i);
            CardEntity energy = energyByType.get(type);
            if (energy != null) {
                result.add(new EnergyAllocation(energy.getId(), count, Set.of(type)));
            }
        }
        return result;
    }

    private String extractEnergyType(CardEntity energy) {
        String name = energy.getName();
        if (name != null) {
            return name.replace(" Energy", "").trim().toUpperCase();
        }
        String provides = energy.getProvidesEnergyTypes();
        if (provides != null && !provides.isBlank()) {
            return provides.split(",")[0].trim().toUpperCase();
        }
        return null;
    }

    private Set<String> extractPokemonTypes(CardEntity pokemon) {
        String types = pokemon.getPokemonTypes();
        if (types == null || types.isBlank()) return Set.of();
        return Arrays.stream(types.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }


    private void pickPokemon(List<DeckCard> deckCards, List<CardEntity> basicPool,
                             List<CardEntity> stage1Pool, List<CardEntity> stage2Pool,
                             Set<String> energyTypes, int targetCount, Random random) {
        if (basicPool.isEmpty()) return;

        List<CardEntity> compatibleBasics = basicPool.stream()
                .filter(p -> isPokemonCompatible(p, energyTypes))
                .collect(Collectors.toList());

        if (compatibleBasics.isEmpty()) return;

        Map<String, List<CardEntity>> stage1ByEvolves = stage1Pool.stream()
                .filter(s1 -> s1.getEvolvesFrom() != null)
                .collect(Collectors.groupingBy(CardEntity::getEvolvesFrom));

        Map<String, List<CardEntity>> stage2ByEvolves = stage2Pool.stream()
                .filter(s2 -> s2.getEvolvesFrom() != null)
                .collect(Collectors.groupingBy(CardEntity::getEvolvesFrom));

        int numMainLines = 1 + random.nextInt(Math.min(2, compatibleBasics.size()));
        List<CardEntity> shuffledBasics = new ArrayList<>(compatibleBasics);
        Collections.shuffle(shuffledBasics, random);

        int stage2LineCount = 0;
        int colorlessSupportCount = 0;
        int pokemonCardsPlaced = 0;

        for (int i = 0; i < Math.min(numMainLines, shuffledBasics.size()) && pokemonCardsPlaced < targetCount; i++) {
            CardEntity basic = shuffledBasics.get(i);
            String basicName = basic.getName();
            boolean isColorless = extractPokemonTypes(basic).contains("COLORLESS");

            if (isColorless && colorlessSupportCount >= 3) continue;

            List<CardEntity> s1ForThis = stage1ByEvolves.getOrDefault(basicName, List.of())
                    .stream().filter(s1 -> isPokemonCompatible(s1, energyTypes))
                    .collect(Collectors.toList());
            List<CardEntity> s2ForThis = stage2ByEvolves.getOrDefault(basicName, List.of())
                    .stream().filter(s2 -> isPokemonCompatible(s2, energyTypes))
                    .collect(Collectors.toList());

            boolean isStage2Line = !s2ForThis.isEmpty() && stage2LineCount < 2;

            if (isStage2Line) {
                int basicQty = 4;
                int s1Qty = 2;
                int s2Qty = 3;
                if (pokemonCardsPlaced + basicQty + s1Qty + s2Qty > targetCount) {
                    int remaining = targetCount - pokemonCardsPlaced;
                    basicQty = Math.min(basicQty, remaining);
                    s1Qty = 0;
                    s2Qty = 0;
                }
                addOrIncrement(deckCards, basic.getId(), basicQty);
                pokemonCardsPlaced += basicQty;
                if (!s1ForThis.isEmpty() && s1Qty > 0) {
                    CardEntity s1 = s1ForThis.get(random.nextInt(s1ForThis.size()));
                    addOrIncrement(deckCards, s1.getId(), s1Qty);
                    pokemonCardsPlaced += s1Qty;
                }
                if (!s2ForThis.isEmpty() && s2Qty > 0) {
                    CardEntity s2 = s2ForThis.get(random.nextInt(s2ForThis.size()));
                    addOrIncrement(deckCards, s2.getId(), s2Qty);
                    pokemonCardsPlaced += s2Qty;
                }
                stage2LineCount++;
                if (isColorless) colorlessSupportCount++;
            } else if (!s1ForThis.isEmpty()) {
                int basicQty = 4;
                int s1Qty = 3;
                if (pokemonCardsPlaced + basicQty + s1Qty > targetCount) {
                    int remaining = targetCount - pokemonCardsPlaced;
                    basicQty = Math.min(basicQty, remaining);
                    s1Qty = 0;
                }
                addOrIncrement(deckCards, basic.getId(), basicQty);
                pokemonCardsPlaced += basicQty;
                CardEntity s1 = s1ForThis.get(random.nextInt(s1ForThis.size()));
                addOrIncrement(deckCards, s1.getId(), s1Qty);
                pokemonCardsPlaced += s1Qty;
                if (isColorless) colorlessSupportCount++;
            } else {
                int basicQty = 3 + random.nextInt(2);
                if (pokemonCardsPlaced + basicQty > targetCount) {
                    basicQty = targetCount - pokemonCardsPlaced;
                }
                if (basicQty > 0) {
                    addOrIncrement(deckCards, basic.getId(), basicQty);
                    pokemonCardsPlaced += basicQty;
                    if (isColorless) colorlessSupportCount++;
                }
            }
        }

        if (pokemonCardsPlaced < targetCount) {
            List<CardEntity> supportPool = shuffledBasics.subList(
                    Math.min(numMainLines, shuffledBasics.size()),
                    shuffledBasics.size());
            for (CardEntity support : supportPool) {
                if (pokemonCardsPlaced >= targetCount) break;
                boolean isColorless = extractPokemonTypes(support).contains("COLORLESS");
                if (isColorless && colorlessSupportCount >= 3) continue;

                int qty = 2 + random.nextInt(2);
                if (pokemonCardsPlaced + qty > targetCount) {
                    qty = targetCount - pokemonCardsPlaced;
                }
                if (qty > 0) {
                    addOrIncrement(deckCards, support.getId(), qty);
                    pokemonCardsPlaced += qty;
                    if (isColorless) colorlessSupportCount++;
                }
            }
        }
    }

    private boolean isPokemonCompatible(CardEntity pokemon, Set<String> energyTypes) {
        Set<String> pokeTypes = extractPokemonTypes(pokemon);
        if (pokeTypes.isEmpty() || pokeTypes.contains("COLORLESS")) return true;
        for (String pt : pokeTypes) {
            if (energyTypes.contains(pt)) return true;
        }
        return false;
    }


    private void pickTrainers(List<DeckCard> deckCards, List<CardEntity> trainers,
                              int targetCount, Random random) {
        List<CardEntity> supporters = filterBySubtype(trainers, "SUPPORTER");
        List<CardEntity> items = filterBySubtype(trainers, "ITEM");
        List<CardEntity> stadiums = filterBySubtype(trainers, "STADIUM");
        List<CardEntity> tools = filterBySubtype(trainers, "TOOL");
        List<CardEntity> aceSpecs = trainers.stream()
                .filter(t -> t.getIsAceSpec() != null && t.getIsAceSpec())
                .collect(Collectors.toList());

        int supporterTarget = 8 + random.nextInt(5);
        int itemTarget = 12 + random.nextInt(7);
        int stadiumTarget = random.nextInt(5);
        int toolTarget = random.nextInt(5);

        supporterTarget = Math.min(supporterTarget, supporters.size() * MAX_COPIES);
        itemTarget = Math.min(itemTarget, items.size() * MAX_COPIES);
        stadiumTarget = Math.min(stadiumTarget, stadiums.size() * MAX_COPIES);
        toolTarget = Math.min(toolTarget, tools.size() * MAX_COPIES);

        int rawTotal = supporterTarget + itemTarget + stadiumTarget + toolTarget;
        if (rawTotal < targetCount) {
            int diff = targetCount - rawTotal;
            itemTarget += diff;
        } else if (rawTotal > targetCount) {
            int diff = rawTotal - targetCount;
            supporterTarget = Math.max(0, supporterTarget - diff / 2);
            itemTarget = Math.max(0, itemTarget - (diff - diff / 2));
        }

        if (!aceSpecs.isEmpty() && random.nextBoolean()) {
            CardEntity ace = aceSpecs.get(random.nextInt(aceSpecs.size()));
            addOrIncrement(deckCards, ace.getId(), 1);
        }

        addRandomFromPool(deckCards, supporters, supporterTarget, random);
        addRandomFromPool(deckCards, items, itemTarget, random);
        addRandomFromPool(deckCards, stadiums, stadiumTarget, random);
        addRandomFromPool(deckCards, tools, toolTarget, random);
    }

    private List<CardEntity> filterBySubtype(List<CardEntity> cards, String subtype) {
        return cards.stream()
                .filter(c -> subtype.equals(c.getTrainerSubtype()))
                .collect(Collectors.toList());
    }

    private void addRandomFromPool(List<DeckCard> deckCards, List<CardEntity> pool,
                                   int targetCount, Random random) {
        if (pool.isEmpty() || targetCount <= 0) return;
        List<CardEntity> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);

        int placed = 0;
        int safety = 0;
        while (placed < targetCount && safety < 100) {
            safety++;
            for (CardEntity card : shuffled) {
                int current = getCurrentQuantity(deckCards, card.getId());
                int canAdd = Math.min(MAX_COPIES - current, targetCount - placed);
                if (canAdd > 0) {
                    addOrIncrement(deckCards, card.getId(), canAdd);
                    placed += canAdd;
                }
                if (placed >= targetCount) break;
            }
        }
    }

    private int getCurrentQuantity(List<DeckCard> deckCards, String cardId) {
        for (DeckCard dc : deckCards) {
            if (dc.getCardId().equals(cardId)) return dc.getQuantity();
        }
        return 0;
    }


    private void pickRandomCards(List<DeckCard> deckCards, List<CardEntity> pool,
                                  Random random, int min, int max, int maxCopies) {
        if (pool.isEmpty()) return;
        int count = min + (max > min ? random.nextInt(max - min + 1) : 0);
        List<CardEntity> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < Math.min(count, shuffled.size()); i++) {
            CardEntity card = shuffled.get(i);
            int qty = 1 + random.nextInt(maxCopies);
            addOrIncrement(deckCards, card.getId(), qty);
        }
    }

    private void addOrIncrement(List<DeckCard> deckCards, String cardId, int quantity) {
        for (DeckCard dc : deckCards) {
            if (dc.getCardId().equals(cardId)) {
                dc.setQuantity(dc.getQuantity() + quantity);
                return;
            }
        }
        DeckCard dc = new DeckCard();
        dc.setCardId(cardId);
        dc.setQuantity(quantity);
        deckCards.add(dc);
    }

    private List<CardEntity> filterBySupertype(List<CardEntity> cards, String supertype) {
        return cards.stream()
                .filter(c -> supertype.equalsIgnoreCase(c.getSupertype()))
                .collect(Collectors.toList());
    }


    private DeckResponse toDeckResponse(List<DeckCard> deckCards, DeckValidationResult validation) {
        List<DeckCardResponse> cardResponses = deckCards.stream()
                .map(this::toCardResponse)
                .collect(Collectors.toList());

        List<DeckValidationResponse.DeckValidationError> errors = validation.getErrors().stream()
                .map(e -> {
                    String code = switch (e) {
                        case DECK_SIZE_INVALID -> "DECK_SIZE_INVALID";
                        case MORE_THAN_4_COPIES -> "DUPLICATE_CARD";
                        case MISSING_BASIC_POKEMON -> "MISSING_BASIC_POKEMON";
                        default -> "INVALID_DECK";
                    };
                    String message = switch (e) {
                        case DECK_SIZE_INVALID -> "El mazo debe tener exactamente 60 cartas.";
                        case MORE_THAN_4_COPIES -> "No puede haber mas de 4 copias de la misma carta.";
                        case MISSING_BASIC_POKEMON -> "El mazo debe tener al menos 1 Pokemon Basico.";
                        default -> "El mazo no es valido.";
                    };
                    return new DeckValidationResponse.DeckValidationError(code, message, null);
                })
                .collect(Collectors.toList());

        return new DeckResponse(
                null,
                "Random Deck",
                null,
                "RANDOM",
                cardResponses.stream().mapToInt(DeckCardResponse::quantity).sum(),
                validation.isValid(),
                cardResponses,
                new DeckValidationResponse(validation.isValid(), errors),
                null
        );
    }

    private DeckCardResponse toCardResponse(DeckCard deckCard) {
        CardDefinition def = cardLookupPort.getCardById(deckCard.getCardId());
        String name = def != null ? def.getName() : deckCard.getCardId();
        String supertype = def != null ? def.getSupertype() : "UNKNOWN";
        boolean isBasicEnergy = def instanceof EnergyCardDefinition
                && ((EnergyCardDefinition) def).getEnergyCardType() == EnergyCardType.BASIC;
        List<String> subtypes = def != null && def.getSubtypes() != null ? def.getSubtypes() : List.of();
        String stage = def instanceof PokemonCardDefinition pokemon ? pokemon.getStage() : null;
        return new DeckCardResponse(deckCard.getCardId(), name, deckCard.getQuantity(), supertype, isBasicEnergy, subtypes, stage);
    }
}
