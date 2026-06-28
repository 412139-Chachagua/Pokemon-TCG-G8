package ar.edu.utn.frc.tup.piii.engine.handlers;

import ar.edu.utn.frc.tup.piii.engine.model.PlayerState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonInPlay;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class HandlerHelperTest {

    @Test
    void shouldFindPokemonInActive() {
        UUID activeId = UUID.randomUUID();
        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(activeId);

        PlayerState player = new PlayerState();
        player.setActivePokemon(active);
        player.setBench(new ArrayList<>());

        PokemonInPlay result = HandlerHelper.findPokemon(player, activeId);
        assertSame(active, result);
    }

    @Test
    void shouldFindPokemonOnBench() {
        UUID benchId = UUID.randomUUID();
        PokemonInPlay benched = new PokemonInPlay();
        benched.setInstanceId(benchId);

        PlayerState player = new PlayerState();
        player.setActivePokemon(null);
        player.setBench(new ArrayList<>(List.of(benched)));

        PokemonInPlay result = HandlerHelper.findPokemon(player, benchId);
        assertSame(benched, result);
    }

    @Test
    void shouldReturnNullWhenNotFound() {
        PlayerState player = new PlayerState();
        player.setActivePokemon(null);
        player.setBench(new ArrayList<>());

        PokemonInPlay result = HandlerHelper.findPokemon(player, UUID.randomUUID());
        assertNull(result);
    }

    @Test
    void shouldReturnNullWhenActiveHasDifferentId() {
        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(UUID.randomUUID());

        PlayerState player = new PlayerState();
        player.setActivePokemon(active);
        player.setBench(new ArrayList<>());

        PokemonInPlay result = HandlerHelper.findPokemon(player, UUID.randomUUID());
        assertNull(result);
    }

    @Test
    void shouldSearchBenchWhenActiveDoesNotMatch() {
        UUID activeId = UUID.randomUUID();
        UUID benchId = UUID.randomUUID();

        PokemonInPlay active = new PokemonInPlay();
        active.setInstanceId(activeId);

        PokemonInPlay benched = new PokemonInPlay();
        benched.setInstanceId(benchId);

        PlayerState player = new PlayerState();
        player.setActivePokemon(active);
        player.setBench(new ArrayList<>(List.of(benched)));

        // Search for bench pokemon
        PokemonInPlay result = HandlerHelper.findPokemon(player, benchId);
        assertSame(benched, result);
    }

    @Test
    void shouldHandleNullActive() {
        UUID benchId = UUID.randomUUID();
        PokemonInPlay benched = new PokemonInPlay();
        benched.setInstanceId(benchId);

        PlayerState player = new PlayerState();
        player.setActivePokemon(null);
        player.setBench(new ArrayList<>(List.of(benched)));

        PokemonInPlay result = HandlerHelper.findPokemon(player, benchId);
        assertSame(benched, result);
    }
}
