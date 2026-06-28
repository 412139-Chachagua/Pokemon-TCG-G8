package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StatusEffectManager {

    private static final Logger log = LoggerFactory.getLogger(StatusEffectManager.class);

    public static void applyCondition(PokemonInPlay pkm, SpecialCondition newCondition) {
        log.warn("[status] applyCondition called: pokemon={}, condition={}",
                pkm.getInstanceId(), newCondition);
        if (pkm.getSpecialConditions() == null) {
            pkm.setSpecialConditions(new ArrayList<>());
        }
        // Per TCG rules:
        // - ASLEEP, CONFUSED, PARALYZED are VOLATILE conditions (mutually exclusive).
        //   Applying a new volatile condition removes any existing volatile condition.
        // - BURNED and POISONED are PERSISTENT conditions (independent markers).
        //   They can coexist with each other and with any volatile condition.
        if (isVolatile(newCondition)) {
            // Remove only other volatile conditions (keep Burned/Poisoned)
            pkm.getSpecialConditions().remove(SpecialCondition.ASLEEP);
            pkm.getSpecialConditions().remove(SpecialCondition.CONFUSED);
            pkm.getSpecialConditions().remove(SpecialCondition.PARALYZED);
        }
        // Remove the same condition if already present (to re-apply)
        pkm.getSpecialConditions().remove(newCondition);
        pkm.getSpecialConditions().add(newCondition);
    }

    private static boolean isVolatile(SpecialCondition sc) {
        return sc == SpecialCondition.ASLEEP || sc == SpecialCondition.CONFUSED || sc == SpecialCondition.PARALYZED;
    }

    public static List<GameEvent> processBetweenTurnStatuses(GameState state, RandomizerPort randomizer, CardLookupPort cardLookup, EngineContext ctx) {
        List<GameEvent> events = new ArrayList<>();
        UUID currentPlayerId = state.getCurrentPlayerId();

        for (PlayerState player : state.getPlayers()) {
            // Only process statuses for the player whose turn just ended.
            // Per TCG rules: poison/burn damage, sleep check, and paralysis recovery
            // happen at the end of the affected player's own turn.
            if (!player.getPlayerId().equals(currentPlayerId)) continue;

            PokemonInPlay active = player.getActivePokemon();
            if (active == null || active.getSpecialConditions() == null) continue;

            if (SweetVeilHook.isImmune(active, player, cardLookup)) {
                active.getSpecialConditions().clear();
                continue;
            }

            List<SpecialCondition> conditions = new ArrayList<>(active.getSpecialConditions());

            for (SpecialCondition sc : conditions) {
                switch (sc) {
                    case POISONED:
                        active.setDamageCounters(active.getDamageCounters() + 1);
                        events.add(new GameEvent(
                                GameEventType.DAMAGE_APPLIED.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                Instant.now(),
                                "Poisened Pokemon took 1 damage counter.",
                                Map.of("defenderPokemonInstanceId", active.getInstanceId().toString(),
                                       "finalDamage", 10,
                                       "damageCountersAdded", 1)
                        ));
                        checkKoBetweenTurns(active, cardLookup, state, player, events, ctx);
                        break;

                    case BURNED:
                        if (randomizer.nextInt(2) == 0) {
                            active.getSpecialConditions().remove(SpecialCondition.BURNED);
                            events.add(new GameEvent(
                                    GameEventType.STATE_UPDATED.name(),
                                    state.getMatchId(),
                                    state.getTurnNumber(),
                                    Instant.now(),
                                    "Burned Pokemon recovered.",
                                    null
                            ));
                        } else {
                            active.setDamageCounters(active.getDamageCounters() + 2);
                            events.add(new GameEvent(
                                    GameEventType.DAMAGE_APPLIED.name(),
                                    state.getMatchId(),
                                    state.getTurnNumber(),
                                    Instant.now(),
                                    "Burned Pokemon took 2 damage counters.",
                                    Map.of("defenderPokemonInstanceId", active.getInstanceId().toString(),
                                           "finalDamage", 20,
                                           "damageCountersAdded", 2)
                            ));
                            checkKoBetweenTurns(active, cardLookup, state, player, events, ctx);
                        }
                        break;

                    case ASLEEP:
                        boolean wokeUp = randomizer.nextInt(2) == 0;
                        events.add(new GameEvent(
                                GameEventType.COIN_FLIP_RESULT.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                Instant.now(),
                                wokeUp ? "Cara" : "Cruz",
                                Map.of("result", wokeUp ? "HEADS" : "TAILS", "source", "sleep_check")
                        ));
                        if (wokeUp) {
                            active.getSpecialConditions().remove(SpecialCondition.ASLEEP);
                            events.add(new GameEvent(
                                    GameEventType.STATE_UPDATED.name(),
                                    state.getMatchId(),
                                    state.getTurnNumber(),
                                    Instant.now(),
                                    "Sleeping Pokemon woke up.",
                                    null
                            ));
                        }
                        break;

                    case PARALYZED:
                        active.getSpecialConditions().remove(SpecialCondition.PARALYZED);
                        events.add(new GameEvent(
                                GameEventType.STATE_UPDATED.name(),
                                state.getMatchId(),
                                state.getTurnNumber(),
                                Instant.now(),
                                "Paralyzed Pokemon recovered between turns.",
                                null
                        ));
                        break;

                    default:
                        break;
                }
            }
        }

        return events;
    }

    public static void checkKoBetweenTurns(
            PokemonInPlay pkm, CardLookupPort cardLookup,
            GameState state, PlayerState owner, List<GameEvent> events,
            EngineContext ctx) {
        PokemonCardDefinition pkmDef = (PokemonCardDefinition) cardLookup.getCardById(pkm.getCardDefinitionId());
        if (pkmDef == null) return;
        if (pkm.getDamageCounters() * 10 < pkmDef.getHp()) return;

        ctx.getEnergyService().detachAllEnergies(pkm, owner, ctx);

        if (pkm.getToolCardInstanceId() != null) {
            CardInstance tool = pkm.getAttachedTool();
            if (tool != null) {
                owner.pushToDiscard(tool);
            }
            pkm.setToolCardInstanceId(null);
            pkm.setAttachedTool(null);
        }

        owner.pushToDiscard(new CardInstance(pkm.getInstanceId(), pkm.getCardDefinitionId()));

        PlayerState opponent = null;
        for (PlayerState p : state.getPlayers()) {
            if (!p.getPlayerId().equals(owner.getPlayerId())) {
                opponent = p;
                break;
            }
        }

        boolean isActive = owner.getActivePokemon() != null
                && owner.getActivePokemon().getInstanceId().equals(pkm.getInstanceId());
        if (isActive) {
            owner.setActivePokemon(null);
            if (owner.getBench() != null && !owner.getBench().isEmpty()) {
                owner.setActivePokemon(owner.getBench().remove(0));
            }
        } else if (owner.getBench() != null) {
            owner.getBench().removeIf(p -> p.getInstanceId().equals(pkm.getInstanceId()));
        }

        if (opponent != null) {
            state.setPendingPrizeOwnerPlayerId(opponent.getPlayerId());
            state.setPendingPrizeCount(state.getPendingPrizeCount() + (pkmDef.isEx() ? 2 : 1));
        }

        events.add(new GameEvent(
                GameEventType.KNOCKOUT_OCCURRED.name(),
                state.getMatchId(), state.getTurnNumber(), Instant.now(),
                "Knockout by special condition.",
                Map.of(
                        "knockedOutPokemonInstanceId", pkm.getInstanceId().toString(),
                        "ownerPlayerId", owner.getPlayerId().toString()
                )
        ));
    }

    public static void clearConditionsOnEvolveOrRetreat(PokemonInPlay pokemon) {
        if (pokemon.getSpecialConditions() != null) {
            pokemon.getSpecialConditions().clear();
        }
    }

    public static void clearConditionsOnBench(PlayerState player) {
        if (player.getBench() == null) return;
        for (PokemonInPlay pkm : player.getBench()) {
            clearConditionsOnEvolveOrRetreat(pkm);
        }
    }

    public static boolean isConfused(PokemonInPlay pokemon) {
        return pokemon.getSpecialConditions() != null
                && pokemon.getSpecialConditions().contains(SpecialCondition.CONFUSED);
    }
}
