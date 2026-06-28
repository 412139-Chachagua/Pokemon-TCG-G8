package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.domain.cards.*;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ErrorCode;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.ForestsCurseHook;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.handlers.HandlerHelper;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyPaymentResult;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class RuleValidator {
    private static final Logger log = LoggerFactory.getLogger(RuleValidator.class);
    private final CardLookupPort cardLookup;
    private final TrainerEffectRegistry effectRegistry;

    public RuleValidator(CardLookupPort cardLookup) {
        this.cardLookup = cardLookup;
        this.effectRegistry = null;
    }

    public RuleValidator(CardLookupPort cardLookup, TrainerEffectRegistry effectRegistry) {
        this.cardLookup = cardLookup;
        this.effectRegistry = effectRegistry;
    }

    public boolean validate(EngineContext ctx, GameAction action) {
        GameActionType type = action.getType();
        return switch (type) {
            case ATTACH_ENERGY -> validateAttachEnergy(ctx, action);
            case PUT_BASIC_ON_BENCH -> validatePutBasicOnBench(ctx, action);
            case EVOLVE_POKEMON -> validateEvolve(ctx, action);
            case PLAY_TRAINER -> validatePlayTrainer(ctx, action);
            case RETREAT_ACTIVE -> validateRetreat(ctx, action);
            case DECLARE_ATTACK -> validateAttack(ctx, action);
            case END_TURN -> validateEndTurn(ctx, action);
            case DRAW_CARD -> validateDrawCard(ctx, action);
            case TAKE_PRIZE_CARD -> validateTakePrizeCard(ctx, action);
            case ATTACH_TOOL -> validateAttachTool(ctx, action);
            case USE_ABILITY -> validateUseAbility(ctx, action);
            case CHOOSE_KO_REPLACEMENT -> validateKOReplacement(ctx, action);
            case SETUP_PLACE_ACTIVE -> validateSetupPlaceActive(ctx, action);
            case SETUP_PLACE_BENCH -> validateSetupPlaceBench(ctx, action);
            case SETUP_REMOVE_ACTIVE -> validateSetupRemoveActive(ctx, action);
            case SETUP_REMOVE_BENCH -> validateSetupRemoveBench(ctx, action);
            case CONFIRM_SETUP -> validateConfirmSetup(ctx, action);
            case RESOLVE_MULLIGAN_DRAW -> validateResolveMulliganDraw(ctx, action);
            case RESOLVE_INITIAL_MULLIGAN -> true;
        };
    }

    private boolean validateAttachEnergy(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (state.getTurnFlags().hasAttachedEnergy()) return false;

        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return false;

        CardInstance card = player.getHand().get(handIndex);
        CardDefinition def = cardLookup.getCardById(card.getCardDefinitionId());
        if (!(def instanceof EnergyCardDefinition)) return false;

        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) return false;
        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        return target != null;
    }

    private boolean validatePutBasicOnBench(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlaceBasic()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return false;

        CardInstance card = player.getHand().get(handIndex);
        CardDefinition def = cardLookup.getCardById(card.getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pokemonDef)) return false;
        if (!"BASIC".equals(pokemonDef.getStage())) return false;

        return player.getBench().size() < 5;
    }

    private boolean validateEvolve(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) return false;

        // No evolution on the player's first turn
        if (!state.hasPlayerCompletedFirstTurn(action.getPlayerId())) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return false;

        CardInstance evolutionCard = player.getHand().get(handIndex);
        CardDefinition evolutionDef = cardLookup.getCardById(evolutionCard.getCardDefinitionId());
        if (!(evolutionDef instanceof PokemonCardDefinition evoDef)) return false;
        if ("BASIC".equals(evoDef.getStage())) return false;

        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) return false;
        UUID targetId = UUID.fromString(targetIdStr);
        PokemonInPlay target = HandlerHelper.findPokemon(player, targetId);
        if (target == null) return false;
        if (target.getEnteredTurnNumber() == state.getTurnNumber()) return false;
        if (target.isEvolvedThisTurn()) return false;

        CardDefinition targetDef = cardLookup.getCardById(target.getCardDefinitionId());
        if (!(targetDef instanceof PokemonCardDefinition targetPkmDef)) return false;

        if (!evoDef.getEvolvesFrom().equalsIgnoreCase(targetPkmDef.getName())) return false;

        String targetStage = targetPkmDef.getStage();
        String evolutionStage = evoDef.getStage();
        boolean validProgression =
                ("BASIC".equalsIgnoreCase(targetStage) && "STAGE_1".equalsIgnoreCase(evolutionStage)) ||
                        ("STAGE_1".equalsIgnoreCase(targetStage) && "STAGE_2".equalsIgnoreCase(evolutionStage));
        if (!validProgression) return false;

        boolean onBench = player.getBench().stream()
                .anyMatch(p -> p.getInstanceId().equals(targetId));
        boolean isActive = player.getActivePokemon() != null
                && player.getActivePokemon().getInstanceId().equals(targetId);
        return onBench || isActive;
    }

    private boolean validatePlayTrainer(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) return false;

        CardInstance card = player.getHand().get(handIndex);
        CardDefinition def = cardLookup.getCardById(card.getCardDefinitionId());
        if (!(def instanceof TrainerCardDefinition trainerDef)) return false;

        if (trainerDef.getTrainerSubtype() == TrainerSubtype.ITEM
                && ForestsCurseHook.isItemBlocked(player, state, cardLookup)) return false;

        if (trainerDef.getTrainerSubtype() == TrainerSubtype.SUPPORTER
                && state.getTurnFlags().hasPlayedSupporter()) {
            ctx.setError(new GameError(ErrorCode.SUPPORTER_ALREADY_PLAYED.name(),
                    "You already played a Supporter this turn"));
            return false;
        }

        if (trainerDef.getTrainerSubtype() == TrainerSubtype.STADIUM
                && state.getTurnFlags().hasPlayedStadium()) {
            ctx.setError(new GameError(ErrorCode.STADIUM_ALREADY_PLAYED.name(),
                    "You already played a Stadium this turn"));
            return false;
        }

        if (trainerDef.getEffectCode() != null && effectRegistry != null
                && !effectRegistry.isEffectCodeKnown(trainerDef.getEffectCode())) {
            ctx.setError(new GameError(ErrorCode.UNKNOWN_EFFECT_CODE.name(),
                    "Unknown trainer effect code: " + trainerDef.getEffectCode()));
            return false;
        }

        if (trainerDef.getEffectCode() != null && effectRegistry != null
                && effectRegistry.isEffectCodeKnown(trainerDef.getEffectCode())) {
            var effectType = effectRegistry.getEffectType(trainerDef.getEffectCode());
            if (effectType != null) {
                List<String> requiredKeys = effectRegistry.getRequiredTargetKeys(effectType);
                for (String key : requiredKeys) {
                    if (action.getPayload() == null || !action.getPayload().containsKey(key)) {
                        ctx.setError(new GameError(ErrorCode.MISSING_TARGET.name(),
                                "Missing required target: " + key));
                        return false;
                    }
                }

                if (effectType == EffectType.EVOLVE_DIRECT) {
                    if (!state.hasPlayerCompletedFirstTurn(action.getPlayerId())) {
                        ctx.setError(new GameError(ErrorCode.EVOLVE_NOT_ALLOWED.name(), "No podés evolucionar en tu primer turno"));
                        return false;
                    }
                    String targetIdStr = (String) action.getPayload().get("targetPokemonInstanceId");
                    if (targetIdStr != null) {
                        UUID targetId = UUID.fromString(targetIdStr);
                        PokemonInPlay target = HandlerHelper.findPokemon(player, targetId);
                        if (target == null) {
                            ctx.setError(new GameError(ErrorCode.INVALID_TARGET.name(), "Target Pokémon no encontrado"));
                            return false;
                        }
                        if (target.getEnteredTurnNumber() == state.getTurnNumber()) {
                            ctx.setError(new GameError(ErrorCode.EVOLVE_NOT_ALLOWED.name(), "Este Pokémon no puede evolucionar este turno"));
                            return false;
                        }
                        if (target.isEvolvedThisTurn()) {
                            ctx.setError(new GameError(ErrorCode.EVOLVE_NOT_ALLOWED.name(), "Este Pokémon ya evolucionó este turno"));
                            return false;
                        }
                    }
                }

                if (effectType == EffectType.RETURN_POKEMON_TO_DECK) {
                    boolean hasBench = player.getBench() != null && !player.getBench().isEmpty();
                    if (!hasBench) {
                        ctx.setError(new GameError(ErrorCode.INVALID_TARGET.name(),
                                "No podés jugar Cassius sin Pokémon en Banca"));
                        return false;
                    }
                    String targetStr = (String) action.getPayload().get("targetPokemonInstanceId");
                    if (targetStr != null) {
                        UUID targetId = UUID.fromString(targetStr);
                        boolean isTargetActive = player.getActivePokemon() != null
                                && player.getActivePokemon().getInstanceId().equals(targetId);
                        if (isTargetActive) {
                            if (action.getPayload().get("newActiveInstanceId") == null) {
                                ctx.setError(new GameError(ErrorCode.MISSING_TARGET.name(),
                                        "Debés elegir un Pokémon de Banca para reemplazar al Activo"));
                                return false;
                            }
                        }
                    }
                }

                if (effectType == EffectType.DISCARD_OPPONENT_ENERGY) {
                    String targetStr = (String) action.getPayload().get("targetPokemonInstanceId");
                    if (targetStr != null) {
                        UUID targetId = UUID.fromString(targetStr);
                        boolean isOwnActive = player.getActivePokemon() != null
                                && player.getActivePokemon().getInstanceId().equals(targetId);
                        boolean isOwnBench = player.getBench() != null
                                && player.getBench().stream().anyMatch(p -> p != null && p.getInstanceId().equals(targetId));
                        if (isOwnActive || isOwnBench) {
                            ctx.setError(new GameError(ErrorCode.INVALID_TARGET.name(),
                                    "Team Flare Grunt solo puede aplicarse sobre Pokémon del oponente"));
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    private boolean validateRetreat(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) return false;

        if (state.getTurnFlags().hasRetreated()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player.getBench().isEmpty()) return false;

        PokemonInPlay active = player.getActivePokemon();
        if (active == null) return false;

        List<UUID> toDiscard;
        @SuppressWarnings("unchecked")
        List<String> rawDiscard = (List<String>) action.getPayload().get("energyCardInstanceIdsToDiscard");
        if (rawDiscard != null && !rawDiscard.isEmpty()) {
            toDiscard = rawDiscard.stream().map(UUID::fromString).toList();
        } else {
            toDiscard = null;
        }

        boolean fairyGardenFreeRetreat = false;
        if (state.getStadiumCardDefinitionId() != null) {
            CardDefinition stadiumDef = cardLookup.getCardById(state.getStadiumCardDefinitionId());
            if (stadiumDef instanceof TrainerCardDefinition trainerDef
                    && "FAIRY_GARDEN".equals(trainerDef.getEffectCode())) {
                if (active.getAttachedEnergies() != null) {
                    for (CardInstance ci : active.getAttachedEnergies()) {
                        CardDefinition eDef = cardLookup.getCardById(ci.getCardDefinitionId());
                        if (eDef instanceof EnergyCardDefinition energyDef
                                && energyDef.getProvides() != null
                                && energyDef.getProvides().contains(EnergyType.FAIRY)) {
                            fairyGardenFreeRetreat = true;
                            break;
                        }
                    }
                }
            }
        }

        if (!fairyGardenFreeRetreat) {
            EnergyPaymentResult result = ctx.getEnergyService().validateAndPayRetreat(active, toDiscard, cardLookup);
            if (!result.canPay()) return false;
        }

        List<SpecialCondition> conditions = active.getSpecialConditions();
        if (conditions != null) {
            if (conditions.contains(SpecialCondition.ASLEEP)) return false;
            if (conditions.contains(SpecialCondition.PARALYZED)) return false;
        }

        return true;
    }

    private boolean validateAttack(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canAttack()) return false;

        // No attack on the first player's first turn (TCG rule: Player 2 CAN attack on turn 2)
        if (state.getFirstPlayerId() != null
                && state.getFirstPlayerId().equals(action.getPlayerId())
                && !state.hasPlayerCompletedFirstTurn(action.getPlayerId())) return false;

        if (state.getTurnFlags() != null && state.getTurnFlags().hasAttacked()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        PokemonInPlay active = player.getActivePokemon();
        if (active == null) return false;

        Integer attackIndex = action.getPayloadInt("attackIndex");
        if (attackIndex == null) return false;

        CardDefinition activeDef = cardLookup.getCardById(active.getCardDefinitionId());
        if (activeDef instanceof PokemonCardDefinition pkmDef) {
            boolean validIndex = pkmDef.getAttacks() != null
                    && pkmDef.getAttacks().stream().anyMatch(a -> a.getIndex() == attackIndex);
            if (!validIndex) return false;
        }

        // Check energy requirements
        if (!ctx.getEnergyService().checkAttackRequirements(active, ctx.getCardLookup(), attackIndex)) {
            ctx.setError(new GameError(ErrorCode.INSUFFICIENT_ENERGY.name(),
                    "El Pokémon activo no tiene suficiente energía para este ataque."));
            return false;
        }

        // ASLEEP and PARALYZED are handled by ConditionCheckStep in the attack chain,
        // which emits ATTACK_CANCELED events with proper messages for the frontend.
        return true;
    }

    private boolean validateEndTurn(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (state.isPendingKOReplacement()) return false;
        return state.getTurnState().canEndTurn();
    }

    private boolean validateDrawCard(EngineContext ctx, GameAction action) {
        if (!ctx.getState().getTurnState().canDraw()) return false;
        if (ctx.getState().getTurnFlags() != null && ctx.getState().getTurnFlags().hasDrawnForTurn()) return false;
        return true;
    }

    private boolean validateTakePrizeCard(EngineContext ctx, GameAction action) {
        UUID pendingOwner = ctx.getState().getPendingPrizeOwnerPlayerId();
        return pendingOwner != null && pendingOwner.equals(action.getPlayerId());
    }

    private boolean validateAttachTool(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) {
            log.warn("[validateAttachTool] REJECTED: turnState.canPlay()=false, phase={}", state.getPhase());
            return false;
        }

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        Integer handIndex = action.getPayloadInt("handIndex");
        if (handIndex == null || handIndex < 0 || handIndex >= player.getHand().size()) {
            log.warn("[validateAttachTool] REJECTED: invalid handIndex={}, handSize={}", handIndex, player.getHand().size());
            return false;
        }

        CardInstance card = player.getHand().get(handIndex);
        CardDefinition def = cardLookup.getCardById(card.getCardDefinitionId());
        if (!(def instanceof TrainerCardDefinition trainerDef)) {
            log.warn("[validateAttachTool] REJECTED: card def is not TrainerCardDefinition, got={}", def != null ? def.getClass().getSimpleName() : "null");
            return false;
        }
        if (!isToolSubtype(trainerDef)) {
            log.warn("[validateAttachTool] REJECTED: invalid trainerSubtype={}, subtypes={}, cardName={}", trainerDef.getTrainerSubtype(), trainerDef.getSubtypes(), trainerDef.getName());
            return false;
        }

        String targetIdStr = action.getPayloadString("targetPokemonInstanceId");
        if (targetIdStr == null) {
            log.warn("[validateAttachTool] REJECTED: targetPokemonInstanceId is null in payload keys={}", action.getPayload() != null ? action.getPayload().keySet() : "null");
            return false;
        }

        PokemonInPlay target = HandlerHelper.findPokemon(player, UUID.fromString(targetIdStr));
        if (target == null) {
            log.warn("[validateAttachTool] REJECTED: target Pokemon not found for instanceId={}", targetIdStr);
            ctx.setError(new GameError(ErrorCode.INVALID_TARGET.name(), "Invalid target Pokemon."));
            return false;
        }

        if (target.getAttachedTool() != null) {
            log.warn("[validateAttachTool] REJECTED: target {} already has a tool attached (toolId={})", targetIdStr, target.getAttachedTool().getInstanceId());
            ctx.setError(new GameError(ErrorCode.INVALID_TARGET.name(), "This Pokemon already has a Tool card attached."));
            return false;
        }

        log.warn("[validateAttachTool] ACCEPTED: handIndex={}, target={}, cardName={}", handIndex, targetIdStr, trainerDef.getName());
        return true;
    }

    private boolean validateKOReplacement(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.isPendingKOReplacement()) return false;
        if (!state.getKnockedOutPlayerId().equals(action.getPlayerId())) return false;

        String benchPkmIdStr = action.getPayloadString("benchPokemonInstanceId");
        if (benchPkmIdStr == null) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());
        UUID benchPkmId = UUID.fromString(benchPkmIdStr);
        return player.getBench().stream()
                .anyMatch(p -> p.getInstanceId().equals(benchPkmId));
    }

    private boolean validateUseAbility(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (!state.getTurnState().canPlay()) return false;

        PlayerState player = ctx.getPlayer(action.getPlayerId());

        String pokemonInstanceIdStr = action.getPayloadString("pokemonInstanceId");
        String abilityName = action.getPayloadString("abilityName");
        if (pokemonInstanceIdStr == null || abilityName == null) return false;

        UUID pokemonInstanceId = UUID.fromString(pokemonInstanceIdStr);
        PokemonInPlay pokemon = HandlerHelper.findPokemon(player, pokemonInstanceId);
        if (pokemon == null) return false;

        CardDefinition cardDef = cardLookup.getCardById(pokemon.getCardDefinitionId());
        if (!(cardDef instanceof PokemonCardDefinition pokemonDef)) return false;

        boolean hasAbility = pokemonDef.getAbilities() != null
                && pokemonDef.getAbilities().stream().anyMatch(a -> a.getName().equals(abilityName));
        if (!hasAbility) return false;

        if (pokemon.isAbilitiesSuppressedNextTurn()) return false;

        if (pokemon.getAbilitiesUsedThisTurn().contains(abilityName)) return false;

        if (pokemon.getSpecialConditions() != null
                && (pokemon.getSpecialConditions().contains(SpecialCondition.ASLEEP)
                || pokemon.getSpecialConditions().contains(SpecialCondition.PARALYZED))) return false;

        return true;
    }

    private boolean validateSetupPlaceActive(EngineContext ctx, GameAction action) {
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player == null) return false;
        if (player.getActivePokemon() != null) return false;
        String cardInstanceIdStr = action.getPayloadString("cardInstanceId");
        if (cardInstanceIdStr == null) return false;
        UUID cardInstanceId = UUID.fromString(cardInstanceIdStr);
        Optional<CardInstance> cardOpt = player.getHand().stream()
                .filter(c -> c.getInstanceId().equals(cardInstanceId))
                .findFirst();
        if (cardOpt.isEmpty()) return false;
        CardDefinition def = cardLookup.getCardById(cardOpt.get().getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmnDef)) return false;
        return "BASIC".equalsIgnoreCase(pkmnDef.getStage()) || pkmnDef.getStage() == null;
    }

    private boolean validateSetupPlaceBench(EngineContext ctx, GameAction action) {
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player == null) return false;
        if (player.getBench().size() >= 5) return false;
        String cardInstanceIdStr = action.getPayloadString("cardInstanceId");
        if (cardInstanceIdStr == null) return false;
        UUID cardInstanceId = UUID.fromString(cardInstanceIdStr);
        Optional<CardInstance> cardOpt = player.getHand().stream()
                .filter(c -> c.getInstanceId().equals(cardInstanceId))
                .findFirst();
        if (cardOpt.isEmpty()) return false;
        CardDefinition def = cardLookup.getCardById(cardOpt.get().getCardDefinitionId());
        if (!(def instanceof PokemonCardDefinition pkmnDef)) return false;
        return "BASIC".equalsIgnoreCase(pkmnDef.getStage()) || pkmnDef.getStage() == null;
    }

    private boolean validateSetupRemoveActive(EngineContext ctx, GameAction action) {
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player == null) return false;
        return player.getActivePokemon() != null;
    }

    private boolean validateSetupRemoveBench(EngineContext ctx, GameAction action) {
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player == null) return false;
        String cardInstanceIdStr = action.getPayloadString("cardInstanceId");
        if (cardInstanceIdStr == null) return false;
        UUID cardInstanceId = UUID.fromString(cardInstanceIdStr);
        return player.getBench().stream().anyMatch(p -> p.getInstanceId().equals(cardInstanceId));
    }

    private boolean validateResolveMulliganDraw(EngineContext ctx, GameAction action) {
        GameState state = ctx.getState();
        if (state.getStatus() != MatchStatus.SETUP) {
            ctx.setError(new GameError("WRONG_STATUS", "Solo se puede resolver mulligan durante SETUP"));
            return false;
        }
        if (!state.isMulliganDrawPending()) {
            ctx.setError(new GameError("NOT_PENDING", "No hay decisión de mulligan pendiente"));
            return false;
        }
        if (!state.hasPendingMulliganDraw(action.getPlayerId())) {
            ctx.setError(new GameError("ALREADY_RESOLVED", "Ya resolviste tu decisión de mulligan"));
            return false;
        }
        return true;
    }

    private static boolean isToolSubtype(TrainerCardDefinition def) {
        if (def.getTrainerSubtype() == TrainerSubtype.POKEMON_TOOL
                || def.getTrainerSubtype() == TrainerSubtype.ITEM) {
            return true;
        }
        List<String> subtypes = def.getSubtypes();
        if (subtypes != null) {
            for (String s : subtypes) {
                String upper = s.toUpperCase()
                        .replace("É", "E")
                        .replace(" ", "_");
                if ("TOOL".equals(upper) || "POKEMON_TOOL".equals(upper)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean validateConfirmSetup(EngineContext ctx, GameAction action) {
        PlayerState player = ctx.getPlayer(action.getPlayerId());
        if (player == null) return false;
        if (player.isSetupConfirmed()) return false;

        GameState state = ctx.getState();
        if (state.isMulliganDrawPending() && state.hasPendingMulliganDraw(action.getPlayerId())) {
            ctx.setError(new GameError("MULLIGAN_DRAW_PENDING",
                "Debes decidir sobre el mulligan del oponente antes de confirmar tu setup"));
            return false;
        }

        return player.getActivePokemon() != null;
    }
}
