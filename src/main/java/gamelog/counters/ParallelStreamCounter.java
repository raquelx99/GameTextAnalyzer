package gamelog.counters;

import java.util.Arrays;

/**
 * Uses Java's parallel Stream API, which runs over the common ForkJoinPool.
 * This is useful as a high-level parallel baseline against manual chunking.
 */
public class ParallelStreamCounter implements WordCounter {
    @Override
    public long count(String[] lines, String word) {
        return Arrays.stream(lines).parallel().filter(word::equals).count();
    }

    @Override
    public String getName() {
        return "ParallelStream";
    }

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.PARALLEL_STREAM;
    }

    @Override
    public int getParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
