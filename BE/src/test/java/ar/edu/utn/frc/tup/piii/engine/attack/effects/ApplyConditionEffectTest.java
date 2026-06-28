package ar.edu.utn.frc.tup.piii.engine.attack.effects;

import ar.edu.utn.frc.tup.piii.domain.cards.AbilityDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardDefinition;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyCardType;
import ar.edu.utn.frc.tup.piii.domain.cards.EnergyType;
import ar.edu.utn.frc.tup.piii.domain.cards.PokemonCardDefinition;
import ar.edu.utn.frc.tup.piii.engine.EngineContext;
import ar.edu.utn.frc.tup.piii.engine.SpecialCondition;
import ar.edu.utn.frc.tup.piii.engine.attack.AttackContext;
import ar.edu.utn.frc.tup.piii.engine.event.GameEvent;
import ar.edu.utn.frc.tup.piii.engine.event.GameEventType;
import ar.edu.utn.frc.tup.piii.engine.model.CardInstance;
import ar.edu.utn.frc.tup.piii.engine.model.GameState;
import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import ar.edu.utn.frc.tup.piii.engine.ports.CardLookupPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ApplyConditionEffectTest {

    @Mock
    private EngineContext ctx;
    @Mock
    private AttackContext attackCtx;
    @Mock
    private GameState state;
    @Mock
    private CardLookupPort cardLookup;

    private PokemonInPlay createPokemon(UUID ownerId) {
        PokemonInPlay p = new PokemonInPlay();
        p.setInstanceId(UUID.randomUUID());
        p.setOwnerPlayerId(ownerId);
        p.setSpecialConditions(new ArrayList<>());
        p.setAttachedEnergies(new ArrayList<>());
        return p;
    }

    @Test
    void apply_conditionToDefender_appliesToDefender() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setActivePokemon(defender);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.PARALYZED);
        effect.apply(ctx, attackCtx);

        assertTrue(defender.getSpecialConditions().contains(SpecialCondition.PARALYZED));
    }

    @Test
    void apply_conditionToSelf_appliesToAttacker() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setActivePokemon(defender);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.CONFUSED, "self");
        effect.apply(ctx, attackCtx);

        assertTrue(attacker.getSpecialConditions().contains(SpecialCondition.CONFUSED));
        assertFalse(defender.getSpecialConditions().contains(SpecialCondition.CONFUSED));
    }

    @Test
    void apply_conditionToBoth_appliesToBoth() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(UUID.randomUUID());
        opponent.setActivePokemon(defender);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.ASLEEP, "both");
        effect.apply(ctx, attackCtx);

        assertTrue(attacker.getSpecialConditions().contains(SpecialCondition.ASLEEP));
        assertTrue(defender.getSpecialConditions().contains(SpecialCondition.ASLEEP));
    }

    @Test
    void apply_sweetVeilImmunity_blocksCondition() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        defender.setAttachedEnergies(new ArrayList<>(List.of(new CardInstance(UUID.randomUUID(), "fairy-1"))));
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        player.setBench(new ArrayList<>());
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(defender.getOwnerPlayerId());
        opponent.setActivePokemon(defender);
        opponent.setBench(new ArrayList<>());

        PokemonCardDefinition pkmDef = new PokemonCardDefinition();
        pkmDef.setAbilities(List.of(new AbilityDefinition("Sweet Veil", "Immune to conditions", null)));
        EnergyCardDefinition fairyDef = new EnergyCardDefinition();
        fairyDef.setEnergyCardType(EnergyCardType.BASIC);
        fairyDef.setProvides(List.of(EnergyType.FAIRY));

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(cardLookup.getCardById(defender.getCardDefinitionId())).thenReturn(pkmDef);
        when(cardLookup.getCardById("fairy-1")).thenReturn(fairyDef);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.POISONED);
        effect.apply(ctx, attackCtx);

        assertFalse(defender.getSpecialConditions().contains(SpecialCondition.POISONED));
    }

    @Test
    void apply_nullDefender_noAction() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(null);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.BURNED);
        effect.apply(ctx, attackCtx);

        verify(ctx, never()).addEvent(any());
    }

    @Test
    void apply_conditionToDefender_firesEvent() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(defender.getOwnerPlayerId());
        opponent.setActivePokemon(defender);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.BURNED);
        effect.apply(ctx, attackCtx);

        ArgumentCaptor<GameEvent> captor = ArgumentCaptor.forClass(GameEvent.class);
        verify(ctx).addEvent(captor.capture());
        assertEquals(GameEventType.STATUS_APPLIED.name(), captor.getValue().getType());
        assertEquals(SpecialCondition.BURNED.name(), captor.getValue().getPayload().get("condition"));
    }

    @Test
    void apply_nullCondition_doesNotThrow() {
        UUID playerId = UUID.randomUUID();
        PokemonInPlay attacker = createPokemon(playerId);
        PokemonInPlay defender = createPokemon(UUID.randomUUID());
        when(attackCtx.getAttacker()).thenReturn(attacker);
        when(attackCtx.getDefender()).thenReturn(defender);

        PlayerState player = new PlayerState();
        player.setPlayerId(playerId);
        player.setActivePokemon(attacker);
        PlayerState opponent = new PlayerState();
        opponent.setPlayerId(defender.getOwnerPlayerId());
        opponent.setActivePokemon(defender);

        when(ctx.getState()).thenReturn(state);
        when(state.getPlayers()).thenReturn(new PlayerState[]{player, opponent});
        when(ctx.getCardLookup()).thenReturn(cardLookup);
        when(state.getMatchId()).thenReturn(UUID.randomUUID());
        when(state.getTurnNumber()).thenReturn(1);

        ApplyConditionEffect effect = new ApplyConditionEffect(null);
        assertThrows(NullPointerException.class, () -> effect.apply(ctx, attackCtx));
    }

    @Test
    void getTiming_returnsAfterDamage() {
        ApplyConditionEffect effect = new ApplyConditionEffect(SpecialCondition.ASLEEP);
        assertEquals(PostDamageEffect.EffectTiming.AFTER_DAMAGE, effect.getTiming());
    }
}
