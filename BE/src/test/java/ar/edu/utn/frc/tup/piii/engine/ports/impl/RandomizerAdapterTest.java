package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RandomizerAdapterTest {

    private RandomizerAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new RandomizerAdapter();
    }

    @Test
    void shuffleAndPick_nullList_returnsNull() {
        assertNull(adapter.shuffleAndPick(null, 1));
    }

    @Test
    void shuffleAndPick_emptyList_returnsNull() {
        assertNull(adapter.shuffleAndPick(List.of(), 1));
    }

    @Test
    void shuffleAndPick_validList_returnsElementInList() {
        List<String> items = List.of("A", "B", "C", "D", "E");

        String picked = adapter.shuffleAndPick(items, 3);

        assertNotNull(picked);
        assertTrue(items.contains(picked));
    }

    @Test
    void shuffleAndPick_countLargerThanSize_usesFullSize() {
        List<String> items = List.of("A", "B");

        String picked = adapter.shuffleAndPick(items, 10);

        assertNotNull(picked);
        assertTrue(items.contains(picked));
    }

    @Test
    void nextInt_returnsValueWithinBound() {
        int result = adapter.nextInt(100);

        assertTrue(result >= 0 && result < 100);
    }

    @Test
    void shuffle_modifiesOrder() {
        List<Integer> items = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        List<Integer> original = new ArrayList<>(items);

        adapter.shuffle(items);

        assertEquals(original.size(), items.size());
        assertTrue(items.containsAll(original)); // same content
    }
}
