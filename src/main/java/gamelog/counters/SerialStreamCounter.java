package gamelog.counters;

import java.util.Arrays;

/**
 * Serial stream counter - uses Java Stream API without parallelism.
 * Useful to compare manual loops against higher-level Java abstractions.
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
