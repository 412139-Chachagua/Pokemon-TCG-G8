package ar.edu.utn.frc.tup.piii.engine.rules;

import ar.edu.utn.frc.tup.piii.domain.cards.*;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.MatchStatus;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.action.GameAction;
import ar.edu.utn.frc.tup.piii.engine.action.GameActionType;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyPaymentResult;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.model.*;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.trainer.EffectType;
import ar.edu.utn.frc.tup.piii.engine.trainer.TrainerEffectRegistry;
import ar.edu.utn.frc.tup.piii.engine.turn.states.TurnState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleValidatorTest {

    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EnergyService energyService;
    @Mock
    private EngineContext ctx;
    @Mock
    private GameState state;
    @Mock
    private PlayerState player;
    @Mock
    private TrainerEffectRegistry effectRegistry;

    private RuleValidator validator;
    private UUID playerId;

    @BeforeEach
    void setUp() {
        validator = new RuleValidator(cardLookup);
        playerId = UUID.randomUUID();
    }

    private void givenCanPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);
        when(ctx.getState()).thenReturn(state);
        lenient().when(state.getTurnFlags()).thenReturn(new TurnFlags());
        lenient().when(player.getPlayerId()).thenReturn(playerId);
    }

    // ============================================================
    // Existing tests
    // ============================================================

    @Test
    void shouldRejectEvolveOnPlayersFirstTurn() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.EVOLVE_POKEMON);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptEvolveAfterFirstTurnCompleted() {
        UUID handCardId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(UUID.randomUUID());
        target.setCardDefinitionId("pikachu");
        target.setEnteredTurnNumber(1);

        PokemonCardDefinition targetDef = new PokemonCardDefinition();
        targetDef.setName("Pikachu");
        targetDef.setStage("BASIC");

        PokemonCardDefinition evoDef = new PokemonCardDefinition();
        evoDef.setStage("STAGE_1");
        evoDef.setEvolvesFrom("Pikachu");

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(true);
        when(state.getTurnNumber()).thenReturn(3);

        GameAction action = new GameAction();
        action.setType(GameActionType.EVOLVE_POKEMON);
        action.setPlayerId(playerId);
        action.setPayload(Map.of(
                "handIndex", 0,
                "targetPokemonInstanceId", target.getInstanceId().toString()
        ));

        when(player.getHand()).thenReturn(List.of(
                new CardInstance(handCardId, "raichu")
        ));
        when(player.getBench()).thenReturn(List.of(target));
        when(cardLookup.getCardById("raichu")).thenReturn(evoDef);
        when(cardLookup.getCardById("pikachu")).thenReturn(targetDef);

        assertTrue(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttackOnPlayersFirstTurn() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canAttack()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.getFirstPlayerId()).thenReturn(playerId);
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.DECLARE_ATTACK);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptAttackAfterFirstTurnCompleted() {
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("charizard");
        active.setSpecialConditions(new ArrayList<>());

        PokemonCardDefinition activeDef = new PokemonCardDefinition();
        var attack = new PokemonCardDefinition.AttackDefinition();
        attack.setIndex(0);
        attack.setCost(new ArrayList<>());
        attack.setDamage("50");
        activeDef.setAttacks(List.of(attack));

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canAttack()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.getFirstPlayerId()).thenReturn(UUID.randomUUID());
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(player.getActivePokemon()).thenReturn(active);
        when(cardLookup.getCardById("charizard")).thenReturn(activeDef);
        when(energyService.checkAttackRequirements(active, cardLookup, 0)).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.DECLARE_ATTACK);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("attackIndex", 0));

        assertTrue(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptKOReplacementWhenValid() {
        UUID benchId = UUID.randomUUID();
        PokemonInPlay benched = new PokemonInPlay();
        benched.setInstanceId(benchId);

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(playerId);
        when(player.getBench()).thenReturn(List.of(benched));

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("benchPokemonInstanceId", benchId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementWhenNotPending() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementForWrongPlayer() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(UUID.randomUUID());

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectKOReplacementWhenBenchPokemonNotOnBench() {
        UUID benchId = UUID.randomUUID();
        UUID wrongId = UUID.randomUUID();

        when(ctx.getState()).thenReturn(state);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.isPendingKOReplacement()).thenReturn(true);
        when(state.getKnockedOutPlayerId()).thenReturn(playerId);
        when(player.getBench()).thenReturn(List.of(
                new PokemonInPlay() {{ setInstanceId(benchId); }}
        ));

        GameAction action = new GameAction();
        action.setType(GameActionType.CHOOSE_KO_REPLACEMENT);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("benchPokemonInstanceId", wrongId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    // ============================================================
    // validateAttachEnergy
    // ============================================================

    @Test
    void shouldRejectAttachEnergy_whenTurnStateCannotPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachEnergy_whenEnergyAlreadyAttached() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        TurnFlags flags = new TurnFlags();
        flags.setHasAttachedEnergy(true);
        when(state.getTurnFlags()).thenReturn(flags);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachEnergy_whenHandIndexInvalid() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(player.getHand()).thenReturn(List.of(new CardInstance(UUID.randomUUID(), "fire-energy")));

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 5));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachEnergy_whenCardIsNotEnergy() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "professors-research")));
        when(cardLookup.getCardById("professors-research")).thenReturn(new TrainerCardDefinition());

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", UUID.randomUUID().toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachEnergy_whenTargetNotFound() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "fire-energy")));
        when(cardLookup.getCardById("fire-energy")).thenReturn(new EnergyCardDefinition());
        UUID targetId = UUID.randomUUID();
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }});
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptAttachEnergy_whenValid() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        UUID cardId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "fire-energy")));
        when(cardLookup.getCardById("fire-energy")).thenReturn(new EnergyCardDefinition());
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(targetId);
        when(player.getActivePokemon()).thenReturn(target);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_ENERGY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validatePutBasicOnBench
    // ============================================================

    @Test
    void shouldRejectPutBasicOnBench_whenCannotPlaceBasic() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlaceBasic()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.PUT_BASIC_ON_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPutBasicOnBench_whenHandIndexInvalid() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlaceBasic()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(List.of(new CardInstance(UUID.randomUUID(), "pikachu")));

        GameAction action = new GameAction();
        action.setType(GameActionType.PUT_BASIC_ON_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 5));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPutBasicOnBench_whenCardNotBasicPokemon() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlaceBasic()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "charmeleon")));
        PokemonCardDefinition nonBasic = new PokemonCardDefinition();
        nonBasic.setStage("STAGE_1");
        when(cardLookup.getCardById("charmeleon")).thenReturn(nonBasic);

        GameAction action = new GameAction();
        action.setType(GameActionType.PUT_BASIC_ON_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPutBasicOnBench_whenBenchFull() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlaceBasic()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "pikachu")));
        PokemonCardDefinition basic = new PokemonCardDefinition();
        basic.setStage("BASIC");
        when(cardLookup.getCardById("pikachu")).thenReturn(basic);
        when(player.getBench()).thenReturn(List.of(
                new PokemonInPlay(), new PokemonInPlay(), new PokemonInPlay(),
                new PokemonInPlay(), new PokemonInPlay()
        ));

        GameAction action = new GameAction();
        action.setType(GameActionType.PUT_BASIC_ON_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptPutBasicOnBench_whenValid() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlaceBasic()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "pikachu")));
        PokemonCardDefinition basic = new PokemonCardDefinition();
        basic.setStage("BASIC");
        when(cardLookup.getCardById("pikachu")).thenReturn(basic);
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.PUT_BASIC_ON_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validatePlayTrainer — basic rejection paths
    // ============================================================

    @Test
    void shouldRejectPlayTrainer_whenTurnStateCannotPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenCardNotTrainer() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "pikachu")));
        when(cardLookup.getCardById("pikachu")).thenReturn(new PokemonCardDefinition());

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenItemBlockedByForestsCurse() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "switch")));
        TrainerCardDefinition itemCard = new TrainerCardDefinition();
        itemCard.setTrainerSubtype(TrainerSubtype.ITEM);
        when(cardLookup.getCardById("switch")).thenReturn(itemCard);

        PlayerState opponent = mock(PlayerState.class);
        when(opponent.getPlayerId()).thenReturn(UUID.randomUUID());
        PokemonInPlay opponentActive = new PokemonInPlay();
        opponentActive.setCardDefinitionId("trevenant");
        opponentActive.setInstanceId(UUID.randomUUID());
        when(opponent.getActivePokemon()).thenReturn(opponentActive);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        PokemonCardDefinition trevenantDef = new PokemonCardDefinition();
        trevenantDef.setAbilities(List.of(new AbilityDefinition("Forest's Curse", "Text", AbilityType.ABILITY)));
        when(cardLookup.getCardById("trevenant")).thenReturn(trevenantDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenSupporterAlreadyPlayed() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "professors-research")));
        TrainerCardDefinition supporterCard = new TrainerCardDefinition();
        supporterCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        when(cardLookup.getCardById("professors-research")).thenReturn(supporterCard);
        TurnFlags flags = new TurnFlags();
        flags.setHasPlayedSupporter(true);
        when(state.getTurnFlags()).thenReturn(flags);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenStadiumAlreadyPlayed() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "tropical-beach")));
        TrainerCardDefinition stadiumCard = new TrainerCardDefinition();
        stadiumCard.setTrainerSubtype(TrainerSubtype.STADIUM);
        when(cardLookup.getCardById("tropical-beach")).thenReturn(stadiumCard);
        TurnFlags flags = new TurnFlags();
        flags.setHasPlayedStadium(true);
        when(state.getTurnFlags()).thenReturn(flags);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenEffectCodeUnknown() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "mystery-card")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("UNKNOWN_EFFECT");
        when(cardLookup.getCardById("mystery-card")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("UNKNOWN_EFFECT")).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainer_whenRequiredTargetMissing() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "hyper-potion")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("HEAL");
        when(cardLookup.getCardById("hyper-potion")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("HEAL")).thenReturn(true);
        when(effectRegistry.getEffectType("HEAL")).thenReturn(EffectType.HEAL);
        when(effectRegistry.getRequiredTargetKeys(EffectType.HEAL)).thenReturn(List.of("targetPokemonInstanceId"));

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    // ============================================================
    // validatePlayTrainer — EvolveDirect
    // ============================================================

    @Test
    void shouldRejectPlayTrainerEvolveDirect_whenPlayersFirstTurn() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "rare-candy")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("EVOLVE_DIRECT");
        when(cardLookup.getCardById("rare-candy")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("EVOLVE_DIRECT")).thenReturn(true);
        when(effectRegistry.getEffectType("EVOLVE_DIRECT")).thenReturn(EffectType.EVOLVE_DIRECT);
        when(effectRegistry.getRequiredTargetKeys(EffectType.EVOLVE_DIRECT)).thenReturn(List.of("targetPokemonInstanceId"));
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", UUID.randomUUID().toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainerEvolveDirect_whenTargetNotFound() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "rare-candy")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("EVOLVE_DIRECT");
        when(cardLookup.getCardById("rare-candy")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("EVOLVE_DIRECT")).thenReturn(true);
        when(effectRegistry.getEffectType("EVOLVE_DIRECT")).thenReturn(EffectType.EVOLVE_DIRECT);
        when(effectRegistry.getRequiredTargetKeys(EffectType.EVOLVE_DIRECT)).thenReturn(List.of("targetPokemonInstanceId"));
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(true);
        UUID targetId = UUID.randomUUID();
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }});
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainerEvolveDirect_whenTargetEnteredThisTurn() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "rare-candy")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("EVOLVE_DIRECT");
        when(cardLookup.getCardById("rare-candy")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("EVOLVE_DIRECT")).thenReturn(true);
        when(effectRegistry.getEffectType("EVOLVE_DIRECT")).thenReturn(EffectType.EVOLVE_DIRECT);
        when(effectRegistry.getRequiredTargetKeys(EffectType.EVOLVE_DIRECT)).thenReturn(List.of("targetPokemonInstanceId"));
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(true);
        when(state.getTurnNumber()).thenReturn(5);
        UUID targetId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setEnteredTurnNumber(5);
        when(player.getActivePokemon()).thenReturn(target);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainerEvolveDirect_whenTargetAlreadyEvolved() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "rare-candy")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("EVOLVE_DIRECT");
        when(cardLookup.getCardById("rare-candy")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("EVOLVE_DIRECT")).thenReturn(true);
        when(effectRegistry.getEffectType("EVOLVE_DIRECT")).thenReturn(EffectType.EVOLVE_DIRECT);
        when(effectRegistry.getRequiredTargetKeys(EffectType.EVOLVE_DIRECT)).thenReturn(List.of("targetPokemonInstanceId"));
        when(state.hasPlayerCompletedFirstTurn(playerId)).thenReturn(true);
        when(state.getTurnNumber()).thenReturn(5);
        UUID targetId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setEnteredTurnNumber(3);
        target.setEvolvedThisTurn(true);
        when(player.getActivePokemon()).thenReturn(target);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    // ============================================================
    // validatePlayTrainer — ReturnToDeck
    // ============================================================

    @Test
    void shouldRejectPlayTrainerReturnToDeck_whenNoBench() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "cassius")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("RETURN_TO_DECK");
        when(cardLookup.getCardById("cassius")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("RETURN_TO_DECK")).thenReturn(true);
        when(effectRegistry.getEffectType("RETURN_TO_DECK")).thenReturn(EffectType.RETURN_POKEMON_TO_DECK);
        when(effectRegistry.getRequiredTargetKeys(EffectType.RETURN_POKEMON_TO_DECK)).thenReturn(List.of("targetPokemonInstanceId"));
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", UUID.randomUUID().toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    @Test
    void shouldRejectPlayTrainerReturnToDeck_whenActiveTargetWithoutReplacement() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "cassius")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("RETURN_TO_DECK");
        when(cardLookup.getCardById("cassius")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("RETURN_TO_DECK")).thenReturn(true);
        when(effectRegistry.getEffectType("RETURN_TO_DECK")).thenReturn(EffectType.RETURN_POKEMON_TO_DECK);
        when(effectRegistry.getRequiredTargetKeys(EffectType.RETURN_POKEMON_TO_DECK)).thenReturn(List.of("targetPokemonInstanceId"));
        UUID targetId = UUID.randomUUID();
        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(targetId);
        when(player.getActivePokemon()).thenReturn(active);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }}));

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    // ============================================================
    // validatePlayTrainer — DiscardOpponentEnergy
    // ============================================================

    @Test
    void shouldRejectPlayTrainerDiscardOpponentEnergy_whenTargetingOwnPokemon() {
        RuleValidator validatorWithRegistry = new RuleValidator(cardLookup, effectRegistry);
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "team-flare-grunt")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        trainerCard.setEffectCode("DISCARD_OPPONENT_ENERGY");
        when(cardLookup.getCardById("team-flare-grunt")).thenReturn(trainerCard);
        when(effectRegistry.isEffectCodeKnown("DISCARD_OPPONENT_ENERGY")).thenReturn(true);
        when(effectRegistry.getEffectType("DISCARD_OPPONENT_ENERGY")).thenReturn(EffectType.DISCARD_OPPONENT_ENERGY);
        when(effectRegistry.getRequiredTargetKeys(EffectType.DISCARD_OPPONENT_ENERGY)).thenReturn(List.of("targetPokemonInstanceId"));
        UUID targetId = UUID.randomUUID();
        PokemonInPlay ownActive = new PokemonInPlay();
        ownActive.setInstanceId(targetId);
        when(player.getActivePokemon()).thenReturn(ownActive);

        GameAction action = new GameAction();
        action.setType(GameActionType.PLAY_TRAINER);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validatorWithRegistry.validate(ctx, action));
    }

    // ============================================================
    // validateRetreat
    // ============================================================

    @Test
    void shouldRejectRetreat_whenTurnStateCannotPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenAlreadyRetreated() {
        givenCanPlay();
        TurnFlags flags = new TurnFlags();
        flags.setHasRetreated(true);
        when(state.getTurnFlags()).thenReturn(flags);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenNoBench() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenNoActive() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenCannotPayEnergy() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        PokemonInPlay active = new PokemonInPlay();
        active.setSpecialConditions(new ArrayList<>());
        active.setAttachedEnergies(new ArrayList<>());
        when(player.getActivePokemon()).thenReturn(active);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(energyService.validateAndPayRetreat(eq(active), isNull(), eq(cardLookup)))
                .thenReturn(new EnergyPaymentResult(false, List.of(), "Not enough energy"));

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenAsleep() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        PokemonInPlay active = new PokemonInPlay();
        active.setSpecialConditions(List.of(SpecialCondition.ASLEEP));
        active.setAttachedEnergies(new ArrayList<>());
        when(player.getActivePokemon()).thenReturn(active);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(energyService.validateAndPayRetreat(eq(active), isNull(), eq(cardLookup)))
                .thenReturn(new EnergyPaymentResult(true, List.of(), null));

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectRetreat_whenParalyzed() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        PokemonInPlay active = new PokemonInPlay();
        active.setSpecialConditions(List.of(SpecialCondition.PARALYZED));
        active.setAttachedEnergies(new ArrayList<>());
        when(player.getActivePokemon()).thenReturn(active);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(energyService.validateAndPayRetreat(eq(active), isNull(), eq(cardLookup)))
                .thenReturn(new EnergyPaymentResult(true, List.of(), null));

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptRetreat_whenFairyGarden() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        PokemonInPlay active = new PokemonInPlay();
        active.setSpecialConditions(new ArrayList<>());
        UUID energyId = UUID.randomUUID();
        CardInstance fairyEnergy = new CardInstance(energyId, "fairy-energy");
        active.setAttachedEnergies(List.of(fairyEnergy));
        when(player.getActivePokemon()).thenReturn(active);
        String stadiumId = "fairy-garden";
        when(state.getStadiumCardDefinitionId()).thenReturn(stadiumId);
        TrainerCardDefinition stadiumDef = new TrainerCardDefinition();
        stadiumDef.setEffectCode("FAIRY_GARDEN");
        when(cardLookup.getCardById(stadiumId)).thenReturn(stadiumDef);
        EnergyCardDefinition fairyEnergyDef = new EnergyCardDefinition();
        fairyEnergyDef.setProvides(List.of(EnergyType.FAIRY));
        when(cardLookup.getCardById("fairy-energy")).thenReturn(fairyEnergyDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertTrue(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptRetreat_whenValidEnergyPayment() {
        givenCanPlay();
        when(state.getTurnFlags()).thenReturn(new TurnFlags());
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay()));
        PokemonInPlay active = new PokemonInPlay();
        active.setSpecialConditions(new ArrayList<>());
        active.setAttachedEnergies(new ArrayList<>());
        when(player.getActivePokemon()).thenReturn(active);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(energyService.validateAndPayRetreat(eq(active), isNull(), eq(cardLookup)))
                .thenReturn(new EnergyPaymentResult(true, List.of(), null));

        GameAction action = new GameAction();
        action.setType(GameActionType.RETREAT_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(new HashMap<>());

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateEndTurn
    // ============================================================

    @Test
    void shouldRejectEndTurn_whenPendingKOReplacement() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.END_TURN);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectEndTurn_whenCannotEndTurn() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(false);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canEndTurn()).thenReturn(false);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.END_TURN);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptEndTurn_whenValid() {
        when(ctx.getState()).thenReturn(state);
        when(state.isPendingKOReplacement()).thenReturn(false);
        TurnState turnState = mock(TurnState.class);
        when(turnState.canEndTurn()).thenReturn(true);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.END_TURN);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateDrawCard
    // ============================================================

    @Test
    void shouldRejectDrawCard_whenCannotDraw() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canDraw()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.DRAW_CARD);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectDrawCard_whenAlreadyDrawn() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canDraw()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        TurnFlags flags = new TurnFlags();
        flags.setHasDrawnForTurn(true);
        when(state.getTurnFlags()).thenReturn(flags);

        GameAction action = new GameAction();
        action.setType(GameActionType.DRAW_CARD);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptDrawCard_whenValid() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canDraw()).thenReturn(true);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);
        when(state.getTurnFlags()).thenReturn(new TurnFlags());

        GameAction action = new GameAction();
        action.setType(GameActionType.DRAW_CARD);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateTakePrizeCard
    // ============================================================

    @Test
    void shouldRejectTakePrizeCard_whenNoPendingPrizeOwner() {
        when(ctx.getState()).thenReturn(state);
        when(state.getPendingPrizeOwnerPlayerId()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.TAKE_PRIZE_CARD);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectTakePrizeCard_whenPlayerIdMismatch() {
        when(ctx.getState()).thenReturn(state);
        when(state.getPendingPrizeOwnerPlayerId()).thenReturn(UUID.randomUUID());

        GameAction action = new GameAction();
        action.setType(GameActionType.TAKE_PRIZE_CARD);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptTakePrizeCard_whenValid() {
        when(ctx.getState()).thenReturn(state);
        when(state.getPendingPrizeOwnerPlayerId()).thenReturn(playerId);

        GameAction action = new GameAction();
        action.setType(GameActionType.TAKE_PRIZE_CARD);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateAttachTool
    // ============================================================

    @Test
    void shouldRejectAttachTool_whenTurnStateCannotPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenHandIndexInvalid() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getHand()).thenReturn(List.of(new CardInstance(UUID.randomUUID(), "choice-band")));

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 5));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenCardNotTrainer() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "pikachu")));
        when(cardLookup.getCardById("pikachu")).thenReturn(new PokemonCardDefinition());

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenCardNotToolSubtype() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "professors-research")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        when(cardLookup.getCardById("professors-research")).thenReturn(trainerCard);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenTargetIdNull() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "choice-band")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);
        when(cardLookup.getCardById("choice-band")).thenReturn(trainerCard);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenTargetNotFound() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "choice-band")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);
        when(cardLookup.getCardById("choice-band")).thenReturn(trainerCard);
        UUID targetId = UUID.randomUUID();
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }});
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectAttachTool_whenTargetAlreadyHasTool() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "choice-band")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);
        when(cardLookup.getCardById("choice-band")).thenReturn(trainerCard);
        UUID targetId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setAttachedTool(new CardInstance(UUID.randomUUID(), "exp-share"));
        when(player.getActivePokemon()).thenReturn(target);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptAttachTool_whenValid() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID cardId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(cardId, "choice-band")));
        TrainerCardDefinition trainerCard = new TrainerCardDefinition();
        trainerCard.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);
        when(cardLookup.getCardById("choice-band")).thenReturn(trainerCard);
        UUID targetId = UUID.randomUUID();
        PokemonInPlay target = new PokemonInPlay();
        target.setInstanceId(targetId);
        target.setAttachedTool(null);
        when(player.getActivePokemon()).thenReturn(target);

        GameAction action = new GameAction();
        action.setType(GameActionType.ATTACH_TOOL);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("handIndex", 0, "targetPokemonInstanceId", targetId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateUseAbility
    // ============================================================

    @Test
    void shouldRejectUseAbility_whenTurnStateCannotPlay() {
        TurnState turnState = mock(TurnState.class);
        when(turnState.canPlay()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.getTurnState()).thenReturn(turnState);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenPokemonInstanceIdNull() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("pokemonInstanceId", null);
        payload.put("abilityName", "Mysterious Phone Call");
        action.setPayload(payload);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenAbilityNameNull() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("pokemonInstanceId", UUID.randomUUID().toString());
        payload.put("abilityName", null);
        action.setPayload(payload);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenPokemonNotFound() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }});
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenPokemonDoesNotHaveAbility() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokeId);
        pokemon.setCardDefinitionId("pikachu");
        when(player.getActivePokemon()).thenReturn(pokemon);
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of());
        when(cardLookup.getCardById("pikachu")).thenReturn(pkmDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenAbilitiesSuppressed() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokeId);
        pokemon.setCardDefinitionId("pikachu");
        pokemon.setAbilitiesSuppressedNextTurn(true);
        when(player.getActivePokemon()).thenReturn(pokemon);
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Mysterious Phone Call", "Text", AbilityType.ABILITY)));
        when(cardLookup.getCardById("pikachu")).thenReturn(pkmDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenAbilityAlreadyUsed() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokeId);
        pokemon.setCardDefinitionId("pikachu");
        pokemon.getAbilitiesUsedThisTurn().add("Mysterious Phone Call");
        when(player.getActivePokemon()).thenReturn(pokemon);
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Mysterious Phone Call", "Text", AbilityType.ABILITY)));
        when(cardLookup.getCardById("pikachu")).thenReturn(pkmDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectUseAbility_whenAsleepOrParalyzed() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokeId);
        pokemon.setCardDefinitionId("pikachu");
        pokemon.setSpecialConditions(List.of(SpecialCondition.ASLEEP));
        when(player.getActivePokemon()).thenReturn(pokemon);
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Mysterious Phone Call", "Text", AbilityType.ABILITY)));
        when(cardLookup.getCardById("pikachu")).thenReturn(pkmDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptUseAbility_whenValid() {
        givenCanPlay();
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID pokeId = UUID.randomUUID();
        PokemonInPlay pokemon = new PokemonInPlay();
        pokemon.setInstanceId(pokeId);
        pokemon.setCardDefinitionId("pikachu");
        pokemon.setSpecialConditions(new ArrayList<>());
        pokemon.setAbilitiesSuppressedNextTurn(false);
        when(player.getActivePokemon()).thenReturn(pokemon);
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Mysterious Phone Call", "Text", AbilityType.ABILITY)));
        when(cardLookup.getCardById("pikachu")).thenReturn(pkmDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.USE_ABILITY);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("pokemonInstanceId", pokeId.toString(), "abilityName", "Mysterious Phone Call"));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateSetupPlaceActive
    // ============================================================

    @Test
    void shouldRejectSetupPlaceActive_whenPlayerNull() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceActive_whenAlreadyHasActive() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay());

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceActive_whenCardInstanceIdNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceActive_whenCardNotInHand() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        when(player.getHand()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", UUID.randomUUID().toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceActive_whenCardNotBasicPokemon() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        UUID instanceId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(instanceId, "charmeleon")));
        when(cardLookup.getCardById("charmeleon")).thenReturn(new PokemonCardDefinition() {{
            setStage("STAGE_1");
        }});

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", instanceId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptSetupPlaceActive_whenValid() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);
        UUID instanceId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(instanceId, "pikachu")));
        PokemonCardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setStage("BASIC");
        when(cardLookup.getCardById("pikachu")).thenReturn(basicDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_ACTIVE);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", instanceId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateSetupPlaceBench
    // ============================================================

    @Test
    void shouldRejectSetupPlaceBench_whenPlayerNull() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceBench_whenBenchFull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(
                new PokemonInPlay(), new PokemonInPlay(), new PokemonInPlay(),
                new PokemonInPlay(), new PokemonInPlay()
        ));

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceBench_whenCardInstanceIdNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceBench_whenCardNotInHand() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of());
        when(player.getHand()).thenReturn(List.of());

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", UUID.randomUUID().toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupPlaceBench_whenCardNotBasic() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of());
        UUID instanceId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(instanceId, "charmeleon")));
        when(cardLookup.getCardById("charmeleon")).thenReturn(new PokemonCardDefinition() {{
            setStage("STAGE_1");
        }});

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", instanceId.toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptSetupPlaceBench_whenValid() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of());
        UUID instanceId = UUID.randomUUID();
        when(player.getHand()).thenReturn(List.of(new CardInstance(instanceId, "pikachu")));
        PokemonCardDefinition basicDef = new PokemonCardDefinition();
        basicDef.setStage("BASIC");
        when(cardLookup.getCardById("pikachu")).thenReturn(basicDef);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_PLACE_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", instanceId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateSetupRemoveActive
    // ============================================================

    @Test
    void shouldRejectSetupRemoveActive_whenPlayerNull() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_ACTIVE);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupRemoveActive_whenNoActive() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_ACTIVE);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptSetupRemoveActive_whenHasActive() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay());

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_ACTIVE);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateSetupRemoveBench
    // ============================================================

    @Test
    void shouldRejectSetupRemoveBench_whenPlayerNull() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupRemoveBench_whenCardInstanceIdNull() {
        when(ctx.getPlayer(playerId)).thenReturn(player);

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_BENCH);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectSetupRemoveBench_whenNotOnBench() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay() {{
            setInstanceId(UUID.randomUUID());
        }}));

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", UUID.randomUUID().toString()));

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptSetupRemoveBench_whenOnBench() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        UUID benchId = UUID.randomUUID();
        when(player.getBench()).thenReturn(List.of(new PokemonInPlay() {{
            setInstanceId(benchId);
        }}));

        GameAction action = new GameAction();
        action.setType(GameActionType.SETUP_REMOVE_BENCH);
        action.setPlayerId(playerId);
        action.setPayload(Map.of("cardInstanceId", benchId.toString()));

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateConfirmSetup
    // ============================================================

    @Test
    void shouldRejectConfirmSetup_whenPlayerNull() {
        when(ctx.getPlayer(playerId)).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.CONFIRM_SETUP);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectConfirmSetup_whenAlreadyConfirmed() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.isSetupConfirmed()).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.CONFIRM_SETUP);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectConfirmSetup_whenMulliganDrawPending() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.isSetupConfirmed()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.isMulliganDrawPending()).thenReturn(true);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.CONFIRM_SETUP);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectConfirmSetup_whenNoActive() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.isSetupConfirmed()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.isMulliganDrawPending()).thenReturn(false);
        when(player.getActivePokemon()).thenReturn(null);

        GameAction action = new GameAction();
        action.setType(GameActionType.CONFIRM_SETUP);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptConfirmSetup_whenValid() {
        when(ctx.getPlayer(playerId)).thenReturn(player);
        when(player.isSetupConfirmed()).thenReturn(false);
        when(ctx.getState()).thenReturn(state);
        when(state.isMulliganDrawPending()).thenReturn(false);
        when(player.getActivePokemon()).thenReturn(new PokemonInPlay());

        GameAction action = new GameAction();
        action.setType(GameActionType.CONFIRM_SETUP);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // validateResolveMulliganDraw
    // ============================================================

    @Test
    void shouldRejectResolveMulliganDraw_whenStatusNotSetup() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.ACTIVE);

        GameAction action = new GameAction();
        action.setType(GameActionType.RESOLVE_MULLIGAN_DRAW);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectResolveMulliganDraw_whenNotPending() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.isMulliganDrawPending()).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.RESOLVE_MULLIGAN_DRAW);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldRejectResolveMulliganDraw_whenPlayerNotPending() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.isMulliganDrawPending()).thenReturn(true);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(false);

        GameAction action = new GameAction();
        action.setType(GameActionType.RESOLVE_MULLIGAN_DRAW);
        action.setPlayerId(playerId);

        assertFalse(validator.validate(ctx, action));
    }

    @Test
    void shouldAcceptResolveMulliganDraw_whenValid() {
        when(ctx.getState()).thenReturn(state);
        when(state.getStatus()).thenReturn(MatchStatus.SETUP);
        when(state.isMulliganDrawPending()).thenReturn(true);
        when(state.hasPendingMulliganDraw(playerId)).thenReturn(true);

        GameAction action = new GameAction();
        action.setType(GameActionType.RESOLVE_MULLIGAN_DRAW);
        action.setPlayerId(playerId);

        assertTrue(validator.validate(ctx, action));
    }

    // ============================================================
    // isToolSubtype (static, tested via reflection)
    // ============================================================

    @Test
    void isToolSubtype_shouldReturnTrue_whenPokemonTool() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.POKEMON_TOOL);
        assertTrue((Boolean) method.invoke(null, def));
    }

    @Test
    void isToolSubtype_shouldReturnTrue_whenItem() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.ITEM);
        assertTrue((Boolean) method.invoke(null, def));
    }

    @Test
    void isToolSubtype_shouldReturnTrue_whenSubtypesContainsTool() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        def.setSubtypes(List.of("TOOL"));
        assertTrue((Boolean) method.invoke(null, def));
    }

    @Test
    void isToolSubtype_shouldReturnTrue_whenSubtypesContainsPokemonTool() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        def.setSubtypes(List.of("POKEMON_TOOL"));
        assertTrue((Boolean) method.invoke(null, def));
    }

    @Test
    void isToolSubtype_shouldReturnTrue_whenSubtypesContainsAccentedVariant() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        def.setSubtypes(List.of("Pokémon Tool"));
        assertTrue((Boolean) method.invoke(null, def));
    }

    @Test
    void isToolSubtype_shouldReturnFalse_whenNoTool() throws Exception {
        Method method = RuleValidator.class.getDeclaredMethod("isToolSubtype", TrainerCardDefinition.class);
        method.setAccessible(true);
        TrainerCardDefinition def = new TrainerCardDefinition();
        def.setTrainerSubtype(TrainerSubtype.SUPPORTER);
        assertFalse((Boolean) method.invoke(null, def));
    }
}
