package ar.edu.utn.frc.tup.piii.configs;

import ar.edu.utn.frc.tup.piii.engine.GameEngine;
import ar.edu.utn.frc.tup.piii.engine.ability.AbilityRegistry;
import ar.edu.utn.frc.tup.piii.engine.energy.BasicEnergyStrategy;
import ar.edu.utn.frc.tup.piii.engine.energy.DoubleColorlessEnergyStrategy;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyMatchingEngine;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyStrategyRegistry;
import ar.edu.utn.frc.tup.piii.engine.energy.RainbowEnergyStrategy;
import ar.edu.utn.frc.tup.piii.engine.energy.StrongEnergyStrategy;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.DeckLoadPort;
import ar.edu.utn.frc.tup.piii.engine.ports.EventPublisherPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.ports.StatePersisterPort;
import ar.edu.utn.frc.tup.piii.engine.rules.RuleValidator;
import ar.edu.utn.frc.tup.piii.engine.setup.SetupManager;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.trainer.resolvers.*;
import ar.edu.utn.frc.tup.piii.engine.turn.TurnManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class GameEngineConfig {

    @Bean
    public SetupManager setupManager(DeckLoadPort deckLoadPort,
                                      CardLookupPort cardLookupPort,
                                      RandomizerPort randomizerPort,
                                      EventPublisherPort eventPublisherPort) {
        return new SetupManager(deckLoadPort, cardLookupPort, randomizerPort, eventPublisherPort);
    }

    @Bean
    public TurnManager turnManager(RandomizerPort randomizerPort) {
        return new TurnManager(randomizerPort);
    }

    @Bean
    public TrainerEffectRegistry trainerEffectRegistry() {
        TrainerEffectRegistry registry = new TrainerEffectRegistry();

        Map<String, Integer> drawCounts = new HashMap<>();
        drawCounts.put("DRAW_7", 7);
        drawCounts.put("DRAW_6", 6);
        drawCounts.put("DRAW_4", 4);
        drawCounts.put("DRAW_3", 3);
        drawCounts.put("DRAW_2", 2);
        drawCounts.put("DRAW_1", 1);

        Map<String, Integer> healCounts = new HashMap<>();
        healCounts.put("HEAL_20", 2);
        healCounts.put("HEAL_30", 3);
        healCounts.put("HEAL_60", 6);
        healCounts.put("HEAL_ALL", 999);

        Map<String, int[]> discardAndDrawConfig = new HashMap<>();
        discardAndDrawConfig.put("DISCARD_1_DRAW_3", new int[]{1, 3});
        discardAndDrawConfig.put("DISCARD_ALL_DRAW_7", new int[]{-1, 7});
        discardAndDrawConfig.put("DISCARD_HAND_DRAW_7", new int[]{-1, 7});
        discardAndDrawConfig.put("DISCARD_1_DRAW_2", new int[]{1, 2});

        Map<String, Integer> shuffleDrawCounts = new HashMap<>();
        shuffleDrawCounts.put("SHUFFLE_DRAW_7", 7);
        shuffleDrawCounts.put("SHUFFLE_DRAW_6", 6);
        shuffleDrawCounts.put("SHUFFLE_DRAW_4", 4);
        shuffleDrawCounts.put("SHUFFLE_DRAW_5", 5);

        registry.registerResolver(new DrawCardsResolver(drawCounts));
        registry.registerResolver(new HealResolver(healCounts));
        registry.registerResolver(new SearchBasicPokemonResolver());
        registry.registerResolver(new SearchEnergyResolver());
        registry.registerResolver(new EvolveSearchResolver());
        registry.registerResolver(new DiscardAndDrawResolver(discardAndDrawConfig));
        registry.registerResolver(new SwitchPokemonResolver());
        registry.registerResolver(new ShuffleHandIntoDeckResolver(shuffleDrawCounts));
        registry.registerResolver(new AttachExtraEnergyResolver());
        registry.registerResolver(new DamageModifyResolver());
        registry.registerResolver(new ConditionRemoveResolver());
        registry.registerResolver(new ReviveResolver());
        registry.registerResolver(new ToolAttachResolver());
        registry.registerResolver(new StadiumPlayResolver());
        registry.registerResolver(new EvolveDirectResolver());
        registry.registerResolver(new LookTopSearchResolver());
        registry.registerResolver(new ReviveToDeckResolver());
        registry.registerResolver(new SearchEnergyToHandResolver());
        registry.registerResolver(new RedCardResolver());
        registry.registerResolver(new CoinFlipDrawResolver());
        registry.registerResolver(new HealWithDiscardResolver());
        registry.registerResolver(new ReturnPokemonToDeckResolver());
        registry.registerResolver(new DiscardOpponentEnergyResolver());

        for (String code : drawCounts.keySet()) {
            registry.registerEffectCode(code, EffectType.DRAW_CARDS);
        }
        for (String code : healCounts.keySet()) {
            registry.registerEffectCode(code, EffectType.HEAL);
        }
        for (String code : discardAndDrawConfig.keySet()) {
            registry.registerEffectCode(code, EffectType.DISCARD_AND_DRAW);
        }
        for (String code : shuffleDrawCounts.keySet()) {
            registry.registerEffectCode(code, EffectType.SHUFFLE_HAND_INTO_DECK);
        }
        registry.registerEffectCode("SEARCH_BASIC", EffectType.SEARCH_BASIC_POKEMON);
        registry.registerEffectCode("SEARCH_ENERGY", EffectType.SEARCH_ENERGY);
        registry.registerEffectCode("EVOLVE_SEARCH", EffectType.EVOLVE_SEARCH);
        registry.registerEffectCode("SWITCH", EffectType.SWITCH_POKEMON);
        registry.registerEffectCode("ATTACH_EXTRA_ENERGY", EffectType.ATTACH_EXTRA_ENERGY);
        registry.registerEffectCode("DAMAGE_MODIFY", EffectType.DAMAGE_MODIFY);
        registry.registerEffectCode("CONDITION_REMOVE", EffectType.CONDITION_REMOVE);
        registry.registerEffectCode("CONDITION_REMOVE_ALL", EffectType.CONDITION_REMOVE);
        registry.registerEffectCode("REVIVE", EffectType.REVIVE);
        registry.registerEffectCode("TOOL_ATTACH", EffectType.TOOL_ATTACH);
        registry.registerEffectCode("STADIUM_PLAY", EffectType.STADIUM_PLAY);
        registry.registerEffectCode("EVOSODA", EffectType.EVOLVE_DIRECT);
        registry.registerEffectCode("GREAT_BALL", EffectType.LOOK_TOP_SEARCH);
        registry.registerEffectCode("MAX_REVIVE", EffectType.REVIVE_TO_DECK);
        registry.registerEffectCode("PROFESSORS_LETTER", EffectType.SEARCH_ENERGY_TO_HAND);
        registry.registerEffectCode("RED_CARD", EffectType.OPPONENT_SHUFFLE_HAND_DRAW);
        registry.registerEffectCode("COIN_FLIP_DRAW_3", EffectType.COIN_FLIP_DRAW);
        registry.registerEffectCode("HEAL_60_DISCARD_1", EffectType.HEAL_WITH_DISCARD);
        registry.registerEffectCode("FAIRY_GARDEN", EffectType.STADIUM_PLAY);
        registry.registerEffectCode("SHADOW_CIRCLE", EffectType.STADIUM_PLAY);
        registry.registerEffectCode("CASSIUS", EffectType.RETURN_POKEMON_TO_DECK);
        registry.registerEffectCode("TEAM_FLARE_GRUNT", EffectType.DISCARD_OPPONENT_ENERGY);

        return registry;
    }

    @Bean
    public AbilityRegistry abilityRegistry() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register("Mystical Fire", new MysticalFireResolver());
        registry.register("Water Shuriken", new WaterShurikenResolver());
        registry.register("Fairy Transfer", new FairyTransferResolver());
        registry.register("Drive Off", new DriveOffResolver());
        registry.register("Stance Change", new StanceChangeResolver());
        registry.register("Upside-Down Evolution", new UpsideDownEvolutionResolver());
        return registry;
    }

    @Bean
    public RuleValidator ruleValidator(CardLookupPort cardLookupPort,
                                        TrainerEffectRegistry trainerEffectRegistry) {
        return new RuleValidator(cardLookupPort, trainerEffectRegistry);
    }

    @Bean
    public EnergyStrategyRegistry energyStrategyRegistry() {
        EnergyStrategyRegistry registry = new EnergyStrategyRegistry();
        registry.register(new BasicEnergyStrategy());
        registry.register(new DoubleColorlessEnergyStrategy());
        registry.register(new RainbowEnergyStrategy());
        registry.register(new StrongEnergyStrategy());
        return registry;
    }

    @Bean
    public EnergyService energyService(EnergyMatchingEngine matchingEngine,
                                       EnergyStrategyRegistry energyStrategyRegistry) {
        return new EnergyService(matchingEngine, energyStrategyRegistry);
    }

    @Bean
    public EnergyMatchingEngine energyMatchingEngine() {
        return new EnergyMatchingEngine();
    }

    @Bean
    public GameEngine gameEngine(CardLookupPort cardLookupPort,
                                  RandomizerPort randomizerPort,
                                  StatePersisterPort statePersisterPort,
                                  EventPublisherPort eventPublisherPort,
                                  TurnManager turnManager,
                                  RuleValidator ruleValidator,
                                  TrainerEffectRegistry trainerEffectRegistry,
                                  AbilityRegistry abilityRegistry,
                                   DeckLoadPort deckLoadPort,
                                   SetupManager setupManager,
                                   EnergyService energyService) {
        return new GameEngine(cardLookupPort, randomizerPort, statePersisterPort,
                eventPublisherPort, turnManager, ruleValidator, trainerEffectRegistry,
                abilityRegistry, deckLoadPort, setupManager,
                energyService);
    }
}
