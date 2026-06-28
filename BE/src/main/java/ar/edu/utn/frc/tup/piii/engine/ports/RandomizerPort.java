package ar.edu.utn.frc.tup.piii.engine.ports;

import java.util.List;

public interface RandomizerPort {
    <T> T shuffleAndPick(List<T> items, int count);
    <T> void shuffle(List<T> items);
    int nextInt(int bound);
}
