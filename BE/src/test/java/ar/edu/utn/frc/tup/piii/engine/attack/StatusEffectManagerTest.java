package ar.edu.utn.frc.tup.piii.engine.attack;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StatusEffectManagerTest {

    @Mock
    private RandomizerPort randomizer;
    @Mock
    private CardLookupPort cardLookup;
    @Mock
    private EngineContext ctx;
    @Mock
    private EnergyService energyService;

    private PokemonInPlay createPokemon(String cardDefId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setCardDefinitionId(cardDefId);
        p.setOwnerPlayerId(UUID.randomUUID());
        p.setDamageCounters(0);
        p.setSpecialConditions(new ArrayList<>());
        p.setAttachedEnergies(new ArrayList<>());
        return p;
    }

    private GameState createGameState(UUID currentPlayerId) {
        GameState state = new GameState();
        state.setMatchId(UUID.randomUUID());
        state.setCurrentPlayerId(currentPlayerId);
        state.setTurnNumber(1);
        PlayerState p1 = new PlayerState();
        p1.setPlayerId(currentPlayerId);
        p1.setDiscard(new ArrayList<>());
        PlayerState p2 = new PlayerState();
        p2.setPlayerId(UUID.randomUUID());
        p2.setDiscard(new ArrayList<>());
        state.setPlayers(new PlayerState[]{p1, p2});
        return state;
    }

    @Test
    void applyCondition_asleep_appliedSuccessfully() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.ASLEEP);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertEquals(1, pkm.getSpecialConditions().size());
    }

    @Test
    void applyCondition_confused_appliedSuccessfully() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.CONFUSED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }

    @Test
    void applyCondition_paralyzed_appliedSuccessfully() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.PARALYZED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void applyCondition_burned_appliedSuccessfully() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.BURNED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.BURNED));
    }

    @Test
    void applyCondition_poisoned_appliedSuccessfully() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.POISONED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.POISONED));
    }

    @Test
    void applyCondition_volatileReplacesExistingVolatile() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.ASLEEP);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.CONFUSED);
        assertFalse(pkm.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        assertEquals(1, pkm.getSpecialConditions().size());
    }

    @Test
    void applyCondition_persistentKeepsExistingVolatile() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.ASLEEP);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.POISONED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertEquals(2, pkm.getSpecialConditions().size());
    }

    @Test
    void applyCondition_burnedAndPoisonedCanCoexist() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.BURNED);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.POISONED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertEquals(2, pkm.getSpecialConditions().size());
    }

    @Test
    void applyCondition_nullConditionsList_initializesNewList() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.setSpecialConditions(null);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.ASLEEP);
        assertNotNull(pkm.getSpecialConditions());
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    @Test
    void applyCondition_volatileReplacesVolatile_keepsPersistent() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        StatusEffectManager.applyCondition(pkm, SpecialCondition.BURNED);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.POISONED);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.ASLEEP);
        StatusEffectManager.applyCondition(pkm, SpecialCondition.CONFUSED);
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.POISONED));
        assertTrue(pkm.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        assertFalse(pkm.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertEquals(3, pkm.getSpecialConditions().size());
    }

    @Test
    void processBetweenTurnStatuses_poisoned_addsDamageCounter() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.POISONED);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getEnergyService()).thenReturn(energyService);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertEquals(1, active.getDamageCounters());
        assertTrue(events.stream().anyMatch(e -> e.getType().equals(GameEventType.DAMAGE_APPLIED.name())));
    }

    @Test
    void processBetweenTurnStatuses_burned_coinFlipHeads_recovers() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.BURNED);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);
        when(randomizer.nextInt(2)).thenReturn(0);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertFalse(active.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertEquals(0, active.getDamageCounters());
    }

    @Test
    void processBetweenTurnStatuses_burned_coinFlipTails_takesDamage() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.BURNED);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(randomizer.nextInt(2)).thenReturn(1);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertTrue(active.getSpecialConditions().contains(SpecialCondition.BURNED));
        assertEquals(2, active.getDamageCounters());
    }

    @Test
    void processBetweenTurnStatuses_asleep_coinFlipHeads_wakesUp() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.ASLEEP);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);
        when(randomizer.nextInt(2)).thenReturn(0);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertFalse(active.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    @Test
    void processBetweenTurnStatuses_asleep_coinFlipTails_staysAsleep() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.ASLEEP);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);
        when(randomizer.nextInt(2)).thenReturn(1);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertTrue(active.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    @Test
    void processBetweenTurnStatuses_paralyzed_recovers() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.PARALYZED);
        state.getPlayers()[0].setActivePokemon(active);
        when(ctx.getState()).thenReturn(state);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertFalse(active.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void processBetweenTurnStatuses_nonCurrentPlayer_skipped() {
        UUID currentPlayerId = UUID.randomUUID();
        UUID otherPlayerId = UUID.randomUUID();
        GameState state = createGameState(currentPlayerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.POISONED);
        state.getPlayers()[1].setActivePokemon(active);
        state.getPlayers()[1].setPlayerId(otherPlayerId);
        state.setCurrentPlayerId(currentPlayerId);
        when(ctx.getState()).thenReturn(state);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertEquals(0, active.getDamageCounters());
        assertTrue(events.isEmpty());
    }

    @Test
    void processBetweenTurnStatuses_nullActive_skipped() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        state.getPlayers()[0].setActivePokemon(null);
        when(ctx.getState()).thenReturn(state);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertTrue(events.isEmpty());
    }

    @Test
    void processBetweenTurnStatuses_sweetVeil_clearsConditions() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.POISONED);
        active.getSpecialConditions().add(SpecialCondition.BURNED);
        active.setAttachedEnergies(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "fairy-1"))));
        EnergyCardDefinition fairyDef = new EnergyCardDefinition();
        fairyDef.setEnergyCardType(EnergyCardType.BASIC);
        fairyDef.setProvides(List.of(EnergyType.FAIRY));
        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Sweet Veil", "Prevents conditions", null)));
        state.getPlayers()[0].setActivePokemon(active);
        state.getPlayers()[0].setBench(new ArrayList<>());
        state.getPlayers()[1].setBench(new ArrayList<>());
        when(ctx.getState()).thenReturn(state);
        when(cardLookup.getCardById("pkm-1")).thenReturn(pkmDef);
        when(cardLookup.getCardById("fairy-1")).thenReturn(fairyDef);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertTrue(active.getSpecialConditions().isEmpty());
    }

    @Test
    void checkKoBetweenTurns_damageBelowHp_noKO() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.setDamageCounters(5);
        GameState state = createGameState(pkm.getOwnerPlayerId());
        PlayerState owner = state.getPlayers()[0];
        owner.setActivePokemon(pkm);
        List<GameEvent> events = new ArrayList<>();
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setHp(100);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(cardLookup.getCardById("pkm-1")).thenReturn(def);

        StatusEffectManager.checkKoBetweenTurns(pkm, cardLookup, state, owner, events, ctx);

        assertTrue(events.isEmpty());
    }

    @Test
    void checkKoBetweenTurns_damageExceedsHp_knocksOutActive() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.setDamageCounters(15);
        pkm.setOwnerPlayerId(UUID.randomUUID());
        GameState state = createGameState(pkm.getOwnerPlayerId());
        PlayerState owner = state.getPlayers()[0];
        owner.setActivePokemon(pkm);
        owner.setBench(new ArrayList<>());
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setHp(100);
        List<GameEvent> events = new ArrayList<>();
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(cardLookup.getCardById("pkm-1")).thenReturn(def);

        StatusEffectManager.checkKoBetweenTurns(pkm, cardLookup, state, owner, events, ctx);

        assertNull(owner.getActivePokemon());
        assertTrue(events.stream().anyMatch(e -> e.getType().equals(GameEventType.KNOCKOUT_OCCURRED.name())));
        verify(energyService).detachAllEnergies(pkm, owner, ctx);
    }

    @Test
    void checkKoBetweenTurns_nullDefinition_noOp() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        GameState state = createGameState(pkm.getOwnerPlayerId());
        PlayerState owner = state.getPlayers()[0];
        List<GameEvent> events = new ArrayList<>();
        when(cardLookup.getCardById("pkm-1")).thenReturn(null);

        StatusEffectManager.checkKoBetweenTurns(pkm, cardLookup, state, owner, events, ctx);

        assertTrue(events.isEmpty());
    }

    @Test
    void clearConditionsOnEvolveOrRetreat_clearsAllConditions() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.getSpecialConditions().add(SpecialCondition.POISONED);
        pkm.getSpecialConditions().add(SpecialCondition.ASLEEP);
        StatusEffectManager.clearConditionsOnEvolveOrRetreat(pkm);
        assertTrue(pkm.getSpecialConditions().isEmpty());
    }

    @Test
    void clearConditionsOnEvolveOrRetreat_nullConditions_noError() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.setSpecialConditions(null);
        assertDoesNotThrow(() -> StatusEffectManager.clearConditionsOnEvolveOrRetreat(pkm));
    }

    @Test
    void clearConditionsOnBench_clearsAllBenchConditions() {
        PlayerState player = new PlayerState();
        PokemonInPlay bench1 = createPokemon("pkm-1");
        bench1.getSpecialConditions().add(SpecialCondition.BURNED);
        PokemonInPlay bench2 = createPokemon("pkm-2");
        bench2.getSpecialConditions().add(SpecialCondition.PARALYZED);
        player.setBench(new ArrayList<>(List.of(bench1, bench2)));
        StatusEffectManager.clearConditionsOnBench(player);
        assertTrue(bench1.getSpecialConditions().isEmpty());
        assertTrue(bench2.getSpecialConditions().isEmpty());
    }

    @Test
    void clearConditionsOnBench_nullBench_noError() {
        PlayerState player = new PlayerState();
        assertDoesNotThrow(() -> StatusEffectManager.clearConditionsOnBench(player));
    }

    @Test
    void isConfused_withConfused_returnsTrue() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.getSpecialConditions().add(SpecialCondition.CONFUSED);
        assertTrue(StatusEffectManager.isConfused(pkm));
    }

    @Test
    void isConfused_withoutConfused_returnsFalse() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.getSpecialConditions().add(SpecialCondition.ASLEEP);
        assertFalse(StatusEffectManager.isConfused(pkm));
    }

    @Test
    void isConfused_nullConditions_returnsFalse() {
        PokemonInPlay pkm = createPokemon("pkm-1");
        pkm.setSpecialConditions(null);
        assertFalse(StatusEffectManager.isConfused(pkm));
    }

    @Test
    void processBetweenTurnStatuses_koFromPoison_checkKoTriggered() {
        UUID playerId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        PokemonInPlay active = createPokemon("pkm-1");
        active.getSpecialConditions().add(SpecialCondition.POISONED);
        active.setDamageCounters(9);
        state.getPlayers()[0].setActivePokemon(active);
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setHp(100);
        state.getPlayers()[0].setBench(new ArrayList<>());
        when(ctx.getState()).thenReturn(state);
        when(ctx.getEnergyService()).thenReturn(energyService);
        when(cardLookup.getCardById("pkm-1")).thenReturn(def);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertTrue(events.stream().anyMatch(e -> e.getType().equals(GameEventType.KNOCKOUT_OCCURRED.name())));
    }

    @Test
    void processBetweenTurnStatuses_onlyProcessesCurrentPlayer() {
        UUID playerId = UUID.randomUUID();
        UUID otherId = UUID.randomUUID();
        GameState state = createGameState(playerId);
        state.getPlayers()[0].setPlayerId(playerId);
        state.getPlayers()[1].setPlayerId(otherId);
        PokemonInPlay active1 = createPokemon("pkm-1");
        active1.setOwnerPlayerId(playerId);
        active1.getSpecialConditions().add(SpecialCondition.POISONED);
        PokemonInPlay active2 = createPokemon("pkm-2");
        active2.setOwnerPlayerId(otherId);
        active2.getSpecialConditions().add(SpecialCondition.POISONED);
        state.getPlayers()[0].setActivePokemon(active1);
        state.getPlayers()[1].setActivePokemon(active2);
        when(ctx.getState()).thenReturn(state);
        when(ctx.getEnergyService()).thenReturn(energyService);

        List<GameEvent> events = StatusEffectManager.processBetweenTurnStatuses(state, randomizer, cardLookup, ctx);

        assertEquals(1, active1.getDamageCounters());
        assertEquals(0, active2.getDamageCounters());
    }
}
