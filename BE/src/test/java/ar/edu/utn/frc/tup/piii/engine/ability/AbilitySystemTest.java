package ar.edu.utn.frc.tup.piii.engine.ability;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.AbilityType;
import ar.edu.utn.frc.tup.piii.domain.cards.CardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.DestinyBurstHook;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.ForestsCurseHook;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.FurCoatHook;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SpikyShieldHook;
import ar.edu.utn.frc.tup.piii.engine.ability.hooks.SweetVeilHook;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.DriveOffResolver;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.FairyTransferResolver;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.MysticalFireResolver;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.StanceChangeResolver;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.UpsideDownEvolutionResolver;
import ar.edu.utn.frc.tup.piii.engine.ability.resolvers.WaterShurikenResolver;
import ar.edu.utn.frc.tup.piii.engine.action.GameError;
import ar.edu.utn.frc.tup.piii.engine.energy.EnergyService;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbilitySystemTest {

    // ─────────────────────────────────────────────
    // AbilityRegistry
    // ─────────────────────────────────────────────

    @Test
    void shouldRegisterAndGetResolver() {
        AbilityRegistry registry = new AbilityRegistry();
        AbilityResolver resolver = mock(AbilityResolver.class);

        registry.register("Test Ability", resolver);
        assertSame(resolver, registry.get("Test Ability"));
    }

    @Test
    void shouldReturnNullWhenAbilityNotRegistered() {
        AbilityRegistry registry = new AbilityRegistry();
        assertNull(registry.get("NonExistent"));
    }

    @Test
    void shouldReturnTrueWhenAbilityIsRegistered() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.register("Foo", mock(AbilityResolver.class));

        assertTrue(registry.has("Foo"));
        assertFalse(registry.has("Bar"));
    }

    // ─────────────────────────────────────────────
    // FurCoatHook
    // ─────────────────────────────────────────────

    @Test
    void shouldReduceDamageBy20WhenFurCoatPresent() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setAbilities(List.of(new AbilityDefinition("Fur Coat", "Reduces damage", AbilityType.POKEMON_POWER)));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(def);

        PokemonInPlay defender = new PokemonInPlay();
        defender.setCardDefinitionId("pkm-test");

        int result = FurCoatHook.reduceDamage(50, defender, cardLookup);
        assertEquals(30, result);
    }

    @Test
    void shouldNotReduceDamageWhenFurCoatAbsent() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setAbilities(List.of(new AbilityDefinition("Other", "...", AbilityType.POKEMON_POWER)));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(def);

        PokemonInPlay defender = new PokemonInPlay();
        defender.setCardDefinitionId("pkm-test");

        assertEquals(50, FurCoatHook.reduceDamage(50, defender, cardLookup));
    }

    @Test
    void shouldFloorAtZeroDamage() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setAbilities(List.of(new AbilityDefinition("Fur Coat", "Reduces damage", AbilityType.POKEMON_POWER)));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(def);

        PokemonInPlay defender = new PokemonInPlay();
        defender.setCardDefinitionId("pkm-test");

        assertEquals(0, FurCoatHook.reduceDamage(10, defender, cardLookup));
    }

    @Test
    void shouldReturnDamageWhenDefenderIsNull() {
        assertEquals(50, FurCoatHook.reduceDamage(50, null, mock(CardLookupPort.class)));
    }

    @Test
    void shouldReturnDamageWhenCardLookupIsNull() {
        assertEquals(50, FurCoatHook.reduceDamage(50, new PokemonInPlay(), null));
    }

    @Test
    void shouldReturnDamageWhenDefIsNotPokemon() {
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(mock(CardDefinition.class));

        assertEquals(50, FurCoatHook.reduceDamage(50, new PokemonInPlay(), cardLookup));
    }

    @Test
    void shouldReturnDamageWhenAbilitiesAreNull() {
        PokemonCardDefinition def = new PokemonCardDefinition();

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(def);

        assertEquals(50, FurCoatHook.reduceDamage(50, new PokemonInPlay(), cardLookup));
    }

    // ─────────────────────────────────────────────
    // SweetVeilHook — isImmune
    // ─────────────────────────────────────────────

    @Test
    void shouldReturnTrueWhenFairyEnergyAndSweetVeilPresent() {
        PokemonInPlay target = new PokemonInPlay();
        target.setCardDefinitionId("pkm-target");
        target.setAttachedEnergies(List.of(createFairyEnergyInstance()));

        PlayerState owner = mock(PlayerState.class);
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("pkm-sweetveil");
        when(owner.getActivePokemon()).thenReturn(active);
        when(owner.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-target")).thenReturn(createFairyEnergyCardDef());
        when(cardLookup.getCardById("pkm-sweetveil")).thenReturn(createSweetVeilPokemonDef());

        assertTrue(SweetVeilHook.isImmune(target, owner, cardLookup));
    }

    @Test
    void shouldReturnFalseWhenNoFairyEnergy() {
        PokemonInPlay target = new PokemonInPlay();
        target.setCardDefinitionId("pkm-target");
        target.setAttachedEnergies(new ArrayList<>());

        PlayerState owner = mock(PlayerState.class);
        when(owner.getActivePokemon()).thenReturn(new PokemonInPlay());
        when(owner.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);

        assertFalse(SweetVeilHook.isImmune(target, owner, cardLookup));
    }

    @Test
    void shouldReturnFalseWhenOwnerHasNoSweetVeil() {
        PokemonInPlay target = new PokemonInPlay();
        target.setCardDefinitionId("pkm-target");
        target.setAttachedEnergies(List.of(createFairyEnergyInstance()));

        PlayerState owner = mock(PlayerState.class);
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("pkm-other");
        when(owner.getActivePokemon()).thenReturn(active);
        when(owner.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-target")).thenReturn(createFairyEnergyCardDef());
        when(cardLookup.getCardById("pkm-other")).thenReturn(createNonSweetVeilPokemonDef());

        assertFalse(SweetVeilHook.isImmune(target, owner, cardLookup));
    }

    @Test
    void shouldReturnFalseWhenTargetIsNull() {
        assertFalse(SweetVeilHook.isImmune(null, mock(PlayerState.class), mock(CardLookupPort.class)));
    }

    @Test
    void shouldReturnFalseWhenOwnerIsNull() {
        assertFalse(SweetVeilHook.isImmune(new PokemonInPlay(), null, mock(CardLookupPort.class)));
    }

    @Test
    void shouldReturnFalseWhenCardLookupIsNull() {
        assertFalse(SweetVeilHook.isImmune(new PokemonInPlay(), mock(PlayerState.class), null));
    }

    // ─────────────────────────────────────────────
    // SweetVeilHook — syncImmunity
    // ─────────────────────────────────────────────

    @Test
    void syncImmunityShouldClearConditionsWhenSweetVeilPresent() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setCardDefinitionId("pkm-target");
        pkm.setAttachedEnergies(List.of(createFairyEnergyInstance()));
        pkm.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));

        PokemonInPlay sweetVeilPkm = new PokemonInPlay();
        sweetVeilPkm.setCardDefinitionId("pkm-sweetveil");

        PlayerState player = mock(PlayerState.class);
        when(player.getActivePokemon()).thenReturn(sweetVeilPkm);
        when(player.getBench()).thenReturn(List.of(pkm));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("energy-fairy")).thenReturn(createFairyEnergyCardDef());
        when(cardLookup.getCardById("pkm-sweetveil")).thenReturn(createSweetVeilPokemonDef());

        SweetVeilHook.syncImmunity(player, cardLookup);

        assertTrue(pkm.getSpecialConditions().isEmpty());
    }

    @Test
    void syncImmunityShouldDoNothingWhenNoSweetVeil() {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setCardDefinitionId("pkm-target");
        pkm.setAttachedEnergies(List.of(createFairyEnergyInstance()));
        pkm.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));

        PlayerState player = mock(PlayerState.class);
        when(player.getActivePokemon()).thenReturn(pkm);
        when(player.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-target")).thenReturn(createFairyEnergyCardDef());

        SweetVeilHook.syncImmunity(player, cardLookup);

        assertFalse(pkm.getSpecialConditions().isEmpty());
    }

    @Test
    void syncImmunityShouldDoNothingWhenPlayerIsNull() {
        SweetVeilHook.syncImmunity(null, mock(CardLookupPort.class));
    }

    // ─────────────────────────────────────────────
    // SweetVeilHook — hasFairyEnergy
    // ─────────────────────────────────────────────

    @Test
    void hasFairyEnergyShouldReturnTrueWhenFairyAttached() {
        PokemonInPlay target = new PokemonInPlay();
        target.setCardDefinitionId("pkm-target");
        target.setAttachedEnergies(List.of(createFairyEnergyInstance()));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById(any())).thenReturn(createFairyEnergyCardDef());

        assertTrue(SweetVeilHook.hasFairyEnergy(target, cardLookup));
    }

    @Test
    void hasFairyEnergyShouldReturnFalseWhenEnergiesNull() {
        PokemonInPlay target = new PokemonInPlay();
        target.setAttachedEnergies(null);

        assertFalse(SweetVeilHook.hasFairyEnergy(target, mock(CardLookupPort.class)));
    }

    @Test
    void hasFairyEnergyShouldReturnFalseWhenEnergyIsNotFairy() {
        PokemonInPlay target = new PokemonInPlay();
        target.setCardDefinitionId("pkm-target");
        target.setAttachedEnergies(List.of(new CardInstance(UUID.randomUUID(), "energy-fire")));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        EnergyCardDefinition fireEnergy = new EnergyCardDefinition();
        fireEnergy.setProvides(List.of(EnergyType.FIRE));
        when(cardLookup.getCardById("energy-fire")).thenReturn(fireEnergy);

        assertFalse(SweetVeilHook.hasFairyEnergy(target, cardLookup));
    }

    // ─────────────────────────────────────────────
    // SweetVeilHook — ownerHasSweetVeil
    // ─────────────────────────────────────────────

    @Test
    void ownerHasSweetVeilShouldReturnTrueWhenPresent() {
        PlayerState owner = mock(PlayerState.class);
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("pkm-sweetveil");
        when(owner.getActivePokemon()).thenReturn(active);
        when(owner.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-sweetveil")).thenReturn(createSweetVeilPokemonDef());

        assertTrue(SweetVeilHook.ownerHasSweetVeil(owner, cardLookup));
    }

    @Test
    void ownerHasSweetVeilShouldReturnFalseWhenAbsent() {
        PlayerState owner = mock(PlayerState.class);
        PokemonInPlay active = new PokemonInPlay();
        active.setCardDefinitionId("pkm-other");
        when(owner.getActivePokemon()).thenReturn(active);
        when(owner.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-other")).thenReturn(createNonSweetVeilPokemonDef());

        assertFalse(SweetVeilHook.ownerHasSweetVeil(owner, cardLookup));
    }

    // ─────────────────────────────────────────────
    // ForestsCurseHook
    // ─────────────────────────────────────────────

    @Test
    void shouldBlockItemsWhenForestsCurseActive() {
        PlayerState player = mock(PlayerState.class);
        UUID playerId = UUID.randomUUID();
        when(player.getPlayerId()).thenReturn(playerId);

        PlayerState opponent = mock(PlayerState.class);
        when(opponent.getPlayerId()).thenReturn(UUID.randomUUID());

        PokemonInPlay opponentActive = new PokemonInPlay();
        opponentActive.setCardDefinitionId("pkm-forestcurse");
        when(opponent.getActivePokemon()).thenReturn(opponentActive);

        GameState state = mock(GameState.class);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setAbilities(List.of(new AbilityDefinition("Forest's Curse", "...", AbilityType.POKEMON_POWER)));
        when(cardLookup.getCardById("pkm-forestcurse")).thenReturn(def);

        assertTrue(ForestsCurseHook.isItemBlocked(player, state, cardLookup));
    }

    @Test
    void shouldNotBlockItemsWhenNoForestsCurse() {
        PlayerState player = mock(PlayerState.class);
        UUID playerId = UUID.randomUUID();
        when(player.getPlayerId()).thenReturn(playerId);

        PlayerState opponent = mock(PlayerState.class);
        when(opponent.getPlayerId()).thenReturn(UUID.randomUUID());

        PokemonInPlay opponentActive = new PokemonInPlay();
        opponentActive.setCardDefinitionId("pkm-other");
        when(opponent.getActivePokemon()).thenReturn(opponentActive);

        GameState state = mock(GameState.class);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-other")).thenReturn(createNonSweetVeilPokemonDef());

        assertFalse(ForestsCurseHook.isItemBlocked(player, state, cardLookup));
    }

    @Test
    void shouldReturnFalseWhenOpponentActiveNull() {
        PlayerState player = mock(PlayerState.class);
        UUID playerId = UUID.randomUUID();
        when(player.getPlayerId()).thenReturn(playerId);

        PlayerState opponent = mock(PlayerState.class);
        when(opponent.getPlayerId()).thenReturn(UUID.randomUUID());
        when(opponent.getActivePokemon()).thenReturn(null);

        GameState state = mock(GameState.class);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});

        assertFalse(ForestsCurseHook.isItemBlocked(player, state, mock(CardLookupPort.class)));
    }

    @Test
    void shouldReturnFalseWhenAnyParamIsNull() {
        assertFalse(ForestsCurseHook.isItemBlocked(null, mock(GameState.class), mock(CardLookupPort.class)));
        assertFalse(ForestsCurseHook.isItemBlocked(mock(PlayerState.class), null, mock(CardLookupPort.class)));
        assertFalse(ForestsCurseHook.isItemBlocked(mock(PlayerState.class), mock(GameState.class), null));
    }

    // ─────────────────────────────────────────────
    // SpikyShieldHook
    // ─────────────────────────────────────────────

    private PokemonCardDefinition createDefWithAbility(String name) {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setAbilities(List.of(new AbilityDefinition(name, "...", AbilityType.POKEMON_POWER)));
        return def;
    }

    private PokemonInPlay createPokemon(String cardDefId) {
        PokemonInPlay pkm = new PokemonInPlay();
        pkm.setInstanceId(UUID.randomUUID());
        pkm.setCardDefinitionId(cardDefId);
        pkm.setDamageCounters(0);
        return pkm;
    }

    @Test
    void shouldAdd3DamageCountersToAttackerWhenSpikyShieldPresent() {
        PokemonInPlay defender = createPokemon("pkm-spiky");
        PokemonInPlay attacker = createPokemon("pkm-attacker");

        EngineContext ctx = mock(EngineContext.class);
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-spiky")).thenReturn(createDefWithAbility("Spiky Shield"));
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        GameState state = mock(GameState.class);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getState()).thenReturn(state);

        SpikyShieldHook.afterDamageTaken(defender, attacker, ctx);

        assertEquals(3, attacker.getDamageCounters());
        verify(ctx).addEvent(argThat(e -> GameEventType.DAMAGE_APPLIED.name().equals(e.getType())));
    }

    @Test
    void shouldNotAddDamageWhenNoSpikyShield() {
        PokemonInPlay defender = createPokemon("pkm-other");
        PokemonInPlay attacker = createPokemon("pkm-attacker");

        EngineContext ctx = mock(EngineContext.class);
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-other")).thenReturn(createNonSweetVeilPokemonDef());
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        SpikyShieldHook.afterDamageTaken(defender, attacker, ctx);

        assertEquals(0, attacker.getDamageCounters());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void shouldDoNothingWhenDefenderIsNull() {
        SpikyShieldHook.afterDamageTaken(null, createPokemon("pkm-attacker"), mock(EngineContext.class));
    }

    @Test
    void shouldDoNothingWhenAttackerIsNullInSpikyShield() {
        SpikyShieldHook.afterDamageTaken(createPokemon("pkm-spiky"), null, mock(EngineContext.class));
    }

    @Test
    void shouldDoNothingWhenCtxIsNullInSpikyShield() {
        SpikyShieldHook.afterDamageTaken(createPokemon("pkm-spiky"), createPokemon("pkm-attacker"), null);
    }

    // ─────────────────────────────────────────────
    // DestinyBurstHook
    // ─────────────────────────────────────────────

    @Test
    void shouldAdd5DamageOnHeads() {
        PokemonInPlay knockedOut = createPokemon("pkm-destiny");
        PokemonInPlay attacker = createPokemon("pkm-attacker");

        EngineContext ctx = mock(EngineContext.class);
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-destiny")).thenReturn(createDefWithAbility("Destiny Burst"));
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        RandomizerPort randomizer = mock(RandomizerPort.class);
        when(randomizer.nextInt(2)).thenReturn(0);
        when(ctx.getRandomizer()).thenReturn(randomizer);

        GameState state = mock(GameState.class);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getState()).thenReturn(state);

        DestinyBurstHook.onKnockout(knockedOut, attacker, ctx);

        assertEquals(5, attacker.getDamageCounters());
        verify(ctx).addEvent(argThat(e -> GameEventType.DAMAGE_APPLIED.name().equals(e.getType())));
    }

    @Test
    void shouldNotAddDamageOnTails() {
        PokemonInPlay knockedOut = createPokemon("pkm-destiny");
        PokemonInPlay attacker = createPokemon("pkm-attacker");

        EngineContext ctx = mock(EngineContext.class);
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-destiny")).thenReturn(createDefWithAbility("Destiny Burst"));
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        RandomizerPort randomizer = mock(RandomizerPort.class);
        when(randomizer.nextInt(2)).thenReturn(1);
        when(ctx.getRandomizer()).thenReturn(randomizer);

        DestinyBurstHook.onKnockout(knockedOut, attacker, ctx);

        assertEquals(0, attacker.getDamageCounters());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void shouldDoNothingWhenNoDestinyBurst() {
        PokemonInPlay knockedOut = createPokemon("pkm-other");
        PokemonInPlay attacker = createPokemon("pkm-attacker");

        EngineContext ctx = mock(EngineContext.class);
        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-other")).thenReturn(createNonSweetVeilPokemonDef());
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        DestinyBurstHook.onKnockout(knockedOut, attacker, ctx);

        assertEquals(0, attacker.getDamageCounters());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void shouldDoNothingWhenKnockedOutIsNull() {
        DestinyBurstHook.onKnockout(null, createPokemon("pkm-attacker"), mock(EngineContext.class));
    }

    @Test
    void shouldDoNothingWhenAttackerIsNull() {
        DestinyBurstHook.onKnockout(createPokemon("pkm-destiny"), null, mock(EngineContext.class));
    }

    @Test
    void shouldDoNothingWhenCtxIsNull() {
        DestinyBurstHook.onKnockout(createPokemon("pkm-destiny"), createPokemon("pkm-attacker"), null);
    }

    // ─────────────────────────────────────────────
    // Resolvers — Helpers
    // ─────────────────────────────────────────────

    private EngineContext mockContextWithState() {
        EngineContext ctx = mock(EngineContext.class);
        GameState state = mock(GameState.class);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getState()).thenReturn(state);
        return ctx;
    }

    private PlayerState mockPlayer(UUID playerId) {
        PlayerState player = mock(PlayerState.class);
        when(player.getPlayerId()).thenReturn(playerId);
        when(player.getHand()).thenReturn(new ArrayList<>());
        when(player.getDeck()).thenReturn(new ArrayList<>());
        return player;
    }

    private CardInstance createEnergyInstance(String cardDefId) {
        return new CardInstance(UUID.randomUUID(), cardDefId);
    }

    private PokemonCardDefinition createNonSweetVeilPokemonDef() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setName("Other Pokemon");
        def.setAbilities(new ArrayList<>());
        return def;
    }

    private PokemonCardDefinition createSweetVeilPokemonDef() {
        PokemonCardDefinition def = new PokemonCardDefinition();
        def.setName("Sweet Veil Pokemon");
        def.setAbilities(List.of(new AbilityDefinition("Sweet Veil", "...", AbilityType.POKEMON_POWER)));
        return def;
    }

    private EnergyCardDefinition createFairyEnergyCardDef() {
        EnergyCardDefinition def = new EnergyCardDefinition();
        def.setProvides(List.of(EnergyType.FAIRY));
        return def;
    }

    private CardInstance createFairyEnergyInstance() {
        return new CardInstance(UUID.randomUUID(), "energy-fairy");
    }

    // ─────────────────────────────────────────────
    // MysticalFireResolver
    // ─────────────────────────────────────────────

    @Test
    void mysticalFireShouldDrawCardsUpToHandSize6() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-mysticalfire");

        List<CardInstance> hand = new ArrayList<>();
        hand.add(new CardInstance(UUID.randomUUID(), "card1"));
        hand.add(new CardInstance(UUID.randomUUID(), "card2"));
        hand.add(new CardInstance(UUID.randomUUID(), "card3"));
        when(player.getHand()).thenReturn(hand);

        List<CardInstance> deck = new ArrayList<>();
        deck.add(new CardInstance(UUID.randomUUID(), "deck1"));
        deck.add(new CardInstance(UUID.randomUUID(), "deck2"));
        deck.add(new CardInstance(UUID.randomUUID(), "deck3"));
        deck.add(new CardInstance(UUID.randomUUID(), "deck4"));
        when(player.getDeck()).thenReturn(deck);

        MysticalFireResolver resolver = new MysticalFireResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), new HashMap<>());

        assertEquals(6, hand.size());
        assertTrue(deck.isEmpty());
        verify(ctx).addEvent(argThat(e -> GameEventType.CARDS_DRAWN.name().equals(e.getType())));
    }

    @Test
    void mysticalFireShouldDrawNothingWhenHandAlreadyAt6() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-mysticalfire");

        List<CardInstance> hand = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            hand.add(new CardInstance(UUID.randomUUID(), "card" + i));
        }
        when(player.getHand()).thenReturn(hand);

        MysticalFireResolver resolver = new MysticalFireResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), new HashMap<>());

        assertEquals(6, hand.size());
        verify(ctx, never()).addEvent(any());
    }

    @Test
    void mysticalFireShouldDrawOnlyAvailableCardsWhenDeckIsSmaller() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-mysticalfire");

        List<CardInstance> hand = new ArrayList<>();
        hand.add(new CardInstance(UUID.randomUUID(), "card1"));
        hand.add(new CardInstance(UUID.randomUUID(), "card2"));
        when(player.getHand()).thenReturn(hand);

        List<CardInstance> deck = new ArrayList<>();
        deck.add(new CardInstance(UUID.randomUUID(), "deck1"));
        when(player.getDeck()).thenReturn(deck);

        MysticalFireResolver resolver = new MysticalFireResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), new HashMap<>());

        assertEquals(3, hand.size());
        assertTrue(deck.isEmpty());
    }

    @Test
    void mysticalFireShouldDrawNothingWhenDeckIsEmpty() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-mysticalfire");

        List<CardInstance> hand = new ArrayList<>();
        hand.add(new CardInstance(UUID.randomUUID(), "card1"));
        when(player.getHand()).thenReturn(hand);

        MysticalFireResolver resolver = new MysticalFireResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), new HashMap<>());

        assertEquals(1, hand.size());
        verify(ctx, never()).addEvent(any());
    }

    // ─────────────────────────────────────────────
    // WaterShurikenResolver
    // ─────────────────────────────────────────────

    @Test
    void waterShurikenShouldApply3DamageAndDiscardEnergy() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-greninja");

        CardInstance waterEnergy = createEnergyInstance("energy-water");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(waterEnergy);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        EnergyCardDefinition waterDef = new EnergyCardDefinition();
        waterDef.setProvides(List.of(EnergyType.WATER));
        when(cardLookup.getCardById("energy-water")).thenReturn(waterDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        UUID opponentId = UUID.randomUUID();
        PlayerState opponent = mockPlayer(opponentId);
        PokemonInPlay target = createPokemon("pkm-target");
        when(opponent.getActivePokemon()).thenReturn(target);
        when(opponent.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getOpponent(playerId)).thenReturn(opponent);

        Map<String, Object> payload = new HashMap<>();
        payload.put("energyCardInstanceId", waterEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", target.getInstanceId().toString());

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), payload);

        assertTrue(hand.isEmpty());
        verify(player).pushToDiscard(waterEnergy);
        assertEquals(3, target.getDamageCounters());
        verify(ctx).addEvent(argThat(e -> GameEventType.DAMAGE_APPLIED.name().equals(e.getType())));
    }

    @Test
    void waterShurikenShouldSetErrorWhenPayloadKeysMissing() {
        EngineContext ctx = mockContextWithState();

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, mock(PlayerState.class), createPokemon("pkm"), mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "MISSING_TARGET".equals(e.getCode())));
    }

    @Test
    void waterShurikenShouldSetErrorWhenEnergyNotInHand() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("energyCardInstanceId", UUID.randomUUID().toString());
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "CARD_NOT_IN_HAND".equals(e.getCode())));
    }

    @Test
    void waterShurikenShouldSetErrorWhenCardIsNotEnergy() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        CardInstance notEnergy = createEnergyInstance("pkm-something");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(notEnergy);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("pkm-something")).thenReturn(createNonSweetVeilPokemonDef());
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        Map<String, Object> payload = new HashMap<>();
        payload.put("energyCardInstanceId", notEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    @Test
    void waterShurikenShouldSetErrorWhenNotWaterEnergy() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        CardInstance fireEnergy = createEnergyInstance("energy-fire");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(fireEnergy);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        EnergyCardDefinition fireDef = new EnergyCardDefinition();
        fireDef.setProvides(List.of(EnergyType.FIRE));
        when(cardLookup.getCardById("energy-fire")).thenReturn(fireDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        Map<String, Object> payload = new HashMap<>();
        payload.put("energyCardInstanceId", fireEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    @Test
    void waterShurikenShouldSetErrorWhenTargetNotFound() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        CardInstance waterEnergy = createEnergyInstance("energy-water");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(waterEnergy);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        EnergyCardDefinition waterDef = new EnergyCardDefinition();
        waterDef.setProvides(List.of(EnergyType.WATER));
        when(cardLookup.getCardById("energy-water")).thenReturn(waterDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        UUID opponentId = UUID.randomUUID();
        PlayerState opponent = mockPlayer(opponentId);
        when(opponent.getActivePokemon()).thenReturn(null);
        when(opponent.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getOpponent(playerId)).thenReturn(opponent);

        Map<String, Object> payload = new HashMap<>();
        payload.put("energyCardInstanceId", waterEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        WaterShurikenResolver resolver = new WaterShurikenResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    // ─────────────────────────────────────────────
    // StanceChangeResolver
    // ─────────────────────────────────────────────

    @Test
    void stanceChangeShouldSwapToAegislash() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay currentPokemon = createPokemon("pkm-doublade");
        currentPokemon.setOwnerPlayerId(playerId);

        CardInstance aegislashCard = new CardInstance(UUID.randomUUID(), "pkm-aegislash");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(aegislashCard);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition aegislashDef = new PokemonCardDefinition();
        aegislashDef.setName("Aegislash");
        when(cardLookup.getCardById("pkm-aegislash")).thenReturn(aegislashDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        StanceChangeResolver resolver = new StanceChangeResolver();
        resolver.resolve(ctx, player, currentPokemon, mock(AbilityDefinition.class), new HashMap<>());

        assertTrue(hand.isEmpty());
        verify(player).pushToDiscard(any(CardInstance.class));
        verify(player).setActivePokemon(argThat(p ->
                p.getInstanceId().equals(aegislashCard.getInstanceId()) &&
                        p.getCardDefinitionId().equals("pkm-aegislash") &&
                        p.getOwnerPlayerId().equals(playerId) &&
                        p.getDamageCounters() == 0
        ));
        verify(ctx).addEvent(argThat(e -> GameEventType.POKEMON_EVOLVED.name().equals(e.getType())));
    }

    @Test
    void stanceChangeShouldSetErrorWhenNoAegislashInHand() {
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(UUID.randomUUID());

        StanceChangeResolver resolver = new StanceChangeResolver();
        resolver.resolve(ctx, player, createPokemon("pkm-doublade"), mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "MISSING_TARGET".equals(e.getCode())));
    }

    @Test
    void stanceChangeShouldDiscardOriginalPokemonWithCorrectId() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay currentPokemon = createPokemon("pkm-doublade");
        currentPokemon.setInstanceId(UUID.randomUUID());
        currentPokemon.setOwnerPlayerId(playerId);

        CardInstance aegislashCard = new CardInstance(UUID.randomUUID(), "pkm-aegislash");
        List<CardInstance> hand = new ArrayList<>();
        hand.add(aegislashCard);
        when(player.getHand()).thenReturn(hand);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition aegislashDef = new PokemonCardDefinition();
        aegislashDef.setName("Aegislash");
        when(cardLookup.getCardById("pkm-aegislash")).thenReturn(aegislashDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        StanceChangeResolver resolver = new StanceChangeResolver();
        resolver.resolve(ctx, player, currentPokemon, mock(AbilityDefinition.class), new HashMap<>());

        ArgumentCaptor<CardInstance> captor = ArgumentCaptor.forClass(CardInstance.class);
        verify(player).pushToDiscard(captor.capture());
        CardInstance discarded = captor.getValue();
        assertEquals(currentPokemon.getInstanceId(), discarded.getInstanceId());
        assertEquals("pkm-doublade", discarded.getCardDefinitionId());
    }

    // ─────────────────────────────────────────────
    // FairyTransferResolver
    // ─────────────────────────────────────────────

    @Test
    void fairyTransferShouldMoveFairyEnergyToTargetPokemon() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        PokemonInPlay sourcePkm = createPokemon("pkm-source");
        CardInstance fairyEnergy = createFairyEnergyInstance();
        sourcePkm.setAttachedEnergies(new ArrayList<>(List.of(fairyEnergy)));

        PokemonInPlay targetPkm = createPokemon("pkm-target");
        targetPkm.setAttachedEnergies(new ArrayList<>());

        when(player.getActivePokemon()).thenReturn(sourcePkm);
        when(player.getBench()).thenReturn(List.of(targetPkm));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        when(cardLookup.getCardById("energy-fairy")).thenReturn(createFairyEnergyCardDef());
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        EnergyService energyService = mock(EnergyService.class);
        when(ctx.getEnergyService()).thenReturn(energyService);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceEnergyInstanceId", fairyEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", targetPkm.getInstanceId().toString());

        FairyTransferResolver resolver = new FairyTransferResolver();
        resolver.resolve(ctx, player, createPokemon("pkm-owner"), mock(AbilityDefinition.class), payload);

        verify(energyService).transferEnergy(fairyEnergy, sourcePkm, targetPkm, player, ctx);
        verify(ctx).addEvent(argThat(e -> GameEventType.ENERGY_ATTACHED.name().equals(e.getType())));
    }

    @Test
    void fairyTransferShouldSetErrorWhenPayloadKeysMissing() {
        EngineContext ctx = mockContextWithState();

        FairyTransferResolver resolver = new FairyTransferResolver();
        resolver.resolve(ctx, mock(PlayerState.class), createPokemon("pkm"), mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "MISSING_TARGET".equals(e.getCode())));
    }

    @Test
    void fairyTransferShouldSetErrorWhenTargetPokemonNotFound() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        when(player.getActivePokemon()).thenReturn(null);
        when(player.getBench()).thenReturn(new ArrayList<>());

        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceEnergyInstanceId", UUID.randomUUID().toString());
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        FairyTransferResolver resolver = new FairyTransferResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    @Test
    void fairyTransferShouldSetErrorWhenSourceEnergyNotFound() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        PokemonInPlay active = createPokemon("pkm-active");
        active.setAttachedEnergies(new ArrayList<>());
        when(player.getActivePokemon()).thenReturn(active);

        PokemonInPlay targetPkm = createPokemon("pkm-target");
        when(player.getBench()).thenReturn(List.of(targetPkm));

        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceEnergyInstanceId", UUID.randomUUID().toString());
        payload.put("targetPokemonInstanceId", targetPkm.getInstanceId().toString());

        FairyTransferResolver resolver = new FairyTransferResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "CARD_NOT_IN_HAND".equals(e.getCode())));
    }

    @Test
    void fairyTransferShouldSetErrorWhenEnergyIsNotFairy() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        PokemonInPlay sourcePkm = createPokemon("pkm-source");
        CardInstance fireEnergy = createEnergyInstance("energy-fire");
        sourcePkm.setAttachedEnergies(new ArrayList<>(List.of(fireEnergy)));

        PokemonInPlay targetPkm = createPokemon("pkm-target");
        targetPkm.setAttachedEnergies(new ArrayList<>());

        when(player.getActivePokemon()).thenReturn(sourcePkm);
        when(player.getBench()).thenReturn(List.of(targetPkm));

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        EnergyCardDefinition fireDef = new EnergyCardDefinition();
        fireDef.setProvides(List.of(EnergyType.FIRE));
        when(cardLookup.getCardById("energy-fire")).thenReturn(fireDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        Map<String, Object> payload = new HashMap<>();
        payload.put("sourceEnergyInstanceId", fireEnergy.getInstanceId().toString());
        payload.put("targetPokemonInstanceId", targetPkm.getInstanceId().toString());

        FairyTransferResolver resolver = new FairyTransferResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    // ─────────────────────────────────────────────
    // DriveOffResolver
    // ─────────────────────────────────────────────

    @Test
    void driveOffShouldSwapOpponentActiveWithBenchTarget() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay pokemon = createPokemon("pkm-driveoff");

        UUID opponentId = UUID.randomUUID();
        PlayerState opponent = mockPlayer(opponentId);

        PokemonInPlay currentActive = createPokemon("pkm-active");
        PokemonInPlay benchTarget = createPokemon("pkm-bench");

        when(opponent.getActivePokemon()).thenReturn(currentActive);
        List<PokemonInPlay> bench = new ArrayList<>();
        bench.add(createPokemon("pkm-other1"));
        bench.add(benchTarget);
        bench.add(createPokemon("pkm-other2"));
        when(opponent.getBench()).thenReturn(bench);

        when(ctx.getOpponent(playerId)).thenReturn(opponent);

        GameState state = mock(GameState.class);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);
        when(ctx.getState()).thenReturn(state);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        CardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setName("Drive Off Pokemon");
        when(cardLookup.getCardById("pkm-driveoff")).thenReturn(pkmDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", benchTarget.getInstanceId().toString());

        DriveOffResolver resolver = new DriveOffResolver();
        resolver.resolve(ctx, player, pokemon, mock(AbilityDefinition.class), payload);

        assertSame(benchTarget, opponent.getActivePokemon());
        assertSame(currentActive, bench.get(1));
        verify(ctx).addEvent(argThat(e -> GameEventType.RETREAT_EXECUTED.name().equals(e.getType())));
    }

    @Test
    void driveOffShouldSetErrorWhenPayloadMissing() {
        EngineContext ctx = mockContextWithState();

        DriveOffResolver resolver = new DriveOffResolver();
        resolver.resolve(ctx, mock(PlayerState.class), createPokemon("pkm"), mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "MISSING_TARGET".equals(e.getCode())));
    }

    @Test
    void driveOffShouldSetErrorWhenTargetNotOnBench() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);

        UUID opponentId = UUID.randomUUID();
        PlayerState opponent = mockPlayer(opponentId);
        when(opponent.getActivePokemon()).thenReturn(createPokemon("pkm-active"));
        when(opponent.getBench()).thenReturn(new ArrayList<>());
        when(ctx.getOpponent(playerId)).thenReturn(opponent);

        Map<String, Object> payload = new HashMap<>();
        payload.put("targetPokemonInstanceId", UUID.randomUUID().toString());

        DriveOffResolver resolver = new DriveOffResolver();
        resolver.resolve(ctx, player, createPokemon("pkm"), mock(AbilityDefinition.class), payload);

        verify(ctx).setError(argThat(e -> "INVALID_TARGET".equals(e.getCode())));
    }

    // ─────────────────────────────────────────────
    // UpsideDownEvolutionResolver
    // ─────────────────────────────────────────────

    @Test
    void upsideDownEvolutionShouldEvolveInkayFromDeckWhenConfused() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay inkay = createPokemon("pkm-inkay");
        inkay.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));
        inkay.setDamageCounters(2);

        CardInstance evolutionCard = new CardInstance(UUID.randomUUID(), "pkm-malamar");
        List<CardInstance> deck = new ArrayList<>();
        deck.add(evolutionCard);
        when(player.getDeck()).thenReturn(deck);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition inkayDef = new PokemonCardDefinition();
        inkayDef.setName("Inkay");
        when(cardLookup.getCardById("pkm-inkay")).thenReturn(inkayDef);

        PokemonCardDefinition malamarDef = new PokemonCardDefinition();
        malamarDef.setName("Malamar");
        malamarDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-malamar")).thenReturn(malamarDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        when(player.getActivePokemon()).thenReturn(inkay);
        when(player.getBench()).thenReturn(new ArrayList<>());

        UpsideDownEvolutionResolver resolver = new UpsideDownEvolutionResolver();
        resolver.resolve(ctx, player, inkay, mock(AbilityDefinition.class), new HashMap<>());

        assertTrue(deck.isEmpty());
        verify(player).setActivePokemon(argThat(p ->
                p.getInstanceId().equals(evolutionCard.getInstanceId()) &&
                        p.getCardDefinitionId().equals("pkm-malamar") &&
                        p.getDamageCounters() == 2 &&
                        p.isEvolvedThisTurn() &&
                        p.getSpecialConditions().isEmpty()
        ));
        verify(ctx).addEvent(argThat(e -> GameEventType.POKEMON_EVOLVED.name().equals(e.getType())));
    }

    @Test
    void upsideDownEvolutionShouldSetErrorWhenNotConfused() {
        EngineContext ctx = mockContextWithState();
        PokemonInPlay inkay = createPokemon("pkm-inkay");
        inkay.setSpecialConditions(new ArrayList<>());

        UpsideDownEvolutionResolver resolver = new UpsideDownEvolutionResolver();
        resolver.resolve(ctx, mock(PlayerState.class), inkay, mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "POKEMON_CANNOT_USE_ABILITY".equals(e.getCode())));
    }

    @Test
    void upsideDownEvolutionShouldSetErrorWhenNoEvolutionInDeck() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay inkay = createPokemon("pkm-inkay");
        inkay.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));

        List<CardInstance> hand = new ArrayList<>();
        when(player.getHand()).thenReturn(hand);
        when(player.getDeck()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition inkayDef = new PokemonCardDefinition();
        inkayDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-inkay")).thenReturn(inkayDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        UpsideDownEvolutionResolver resolver = new UpsideDownEvolutionResolver();
        resolver.resolve(ctx, player, inkay, mock(AbilityDefinition.class), new HashMap<>());

        verify(ctx).setError(argThat(e -> "MISSING_TARGET".equals(e.getCode())));
    }

    @Test
    void upsideDownEvolutionShouldEvolveInBenchPositionWhenNotActive() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay inkay = createPokemon("pkm-inkay");
        inkay.setInstanceId(UUID.randomUUID());
        inkay.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));

        PokemonInPlay active = createPokemon("pkm-active");
        when(player.getActivePokemon()).thenReturn(active);

        CardInstance evolutionCard = new CardInstance(UUID.randomUUID(), "pkm-malamar");
        List<CardInstance> deck = new ArrayList<>();
        deck.add(evolutionCard);
        when(player.getDeck()).thenReturn(deck);

        List<PokemonInPlay> bench = new ArrayList<>();
        bench.add(createPokemon("pkm-bench1"));
        bench.add(inkay);
        bench.add(createPokemon("pkm-bench2"));
        when(player.getBench()).thenReturn(bench);

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition inkayDef = new PokemonCardDefinition();
        inkayDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-inkay")).thenReturn(inkayDef);

        PokemonCardDefinition malamarDef = new PokemonCardDefinition();
        malamarDef.setName("Malamar");
        malamarDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-malamar")).thenReturn(malamarDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        UpsideDownEvolutionResolver resolver = new UpsideDownEvolutionResolver();
        resolver.resolve(ctx, player, inkay, mock(AbilityDefinition.class), new HashMap<>());

        PokemonInPlay evolved = bench.get(1);
        assertEquals("pkm-malamar", evolved.getCardDefinitionId());
        assertEquals(evolutionCard.getInstanceId(), evolved.getInstanceId());
    }

    @Test
    void upsideDownEvolutionShouldCarryOverDamageAndEnergies() {
        UUID playerId = UUID.randomUUID();
        EngineContext ctx = mockContextWithState();
        PlayerState player = mockPlayer(playerId);
        PokemonInPlay inkay = createPokemon("pkm-inkay");
        inkay.setInstanceId(UUID.randomUUID());
        inkay.setSpecialConditions(new ArrayList<>(List.of(SpecialCondition.CONFUSED)));
        inkay.setDamageCounters(4);

        CardInstance darkEnergy = createEnergyInstance("energy-dark");
        inkay.setAttachedEnergies(new ArrayList<>(List.of(darkEnergy)));

        CardInstance evolutionCard = new CardInstance(UUID.randomUUID(), "pkm-malamar");
        List<CardInstance> deck = new ArrayList<>();
        deck.add(evolutionCard);
        when(player.getDeck()).thenReturn(deck);

        when(player.getActivePokemon()).thenReturn(inkay);
        when(player.getBench()).thenReturn(new ArrayList<>());

        CardLookupPort cardLookup = mock(CardLookupPort.class);
        PokemonCardDefinition inkayDef = new PokemonCardDefinition();
        inkayDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-inkay")).thenReturn(inkayDef);

        PokemonCardDefinition malamarDef = new PokemonCardDefinition();
        malamarDef.setName("Malamar");
        malamarDef.setEvolvesFrom("Inkay");
        when(cardLookup.getCardById("pkm-malamar")).thenReturn(malamarDef);
        when(ctx.getCardLookup()).thenReturn(cardLookup);

        UpsideDownEvolutionResolver resolver = new UpsideDownEvolutionResolver();
        resolver.resolve(ctx, player, inkay, mock(AbilityDefinition.class), new HashMap<>());

        verify(player).setActivePokemon(argThat(p ->
                p.getDamageCounters() == 4 &&
                        p.getAttachedEnergies().size() == 1 &&
                        p.getAttachedEnergies().get(0).getCardDefinitionId().equals("energy-dark") &&
                        p.getSpecialConditions().isEmpty()
        ));
    }
}
