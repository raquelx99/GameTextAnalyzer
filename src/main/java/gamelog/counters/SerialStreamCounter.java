package gamelog.counters;

import java.util.Arrays;

/**
 * Contador stream serial - usa a Java Stream API sem paralelismo.
 * Útil para comparar loops manuais com abstrações Java de nível mais alto.
 */
public class SerialStreamCounter implements WordCounter {
    @Override
    public long count(String[] lines, String word) {
        return Arrays.stream(lines).filter(word::equals).count();
    }

    @Override
    public String getName() {
        return "SerialStream";
    }

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.SERIAL;
    }
}
