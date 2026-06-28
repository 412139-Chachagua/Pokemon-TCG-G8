package ar.edu.utn.frc.tup.piii.engine.ports.impl;

import ar.edu.utn.frc.tup.piii.engine.ports.RandomizerPort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
public class RandomizerAdapter implements RandomizerPort {

    private final Random random;

    public RandomizerAdapter() {
        this.random = new Random();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T shuffleAndPick(List<T> items, int count) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<T> copy = new ArrayList<>(items);
        int pickIndex = random.nextInt(copy.size());
        if (count > 0 && count <= copy.size()) {
            pickIndex = random.nextInt(count);
        }
        return copy.get(pickIndex);
    }

    @Override
    public <T> void shuffle(List<T> items) {
        for (int i = items.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T temp = items.get(i);
            items.set(i, items.get(j));
            items.set(j, temp);
        }
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
