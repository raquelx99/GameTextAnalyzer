package gamelog.counters;

import java.util.ArrayList;
import java.util.List;

/**
 * Central registry of benchmark strategies.
 *
 * To add a new method to the application, create a new WordCounter
 * implementation and add it here. The benchmark runner, CSV and charts will
 * automatically include it.
 */
public final class StrategyRegistry {
    private StrategyRegistry() {}

    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    public static List<WordCounter> defaultStrategies() {
        List<WordCounter> strategies = new ArrayList<>();
        strategies.add(new SerialCPUCounter());
        strategies.add(new SerialStreamCounter());

        // Traditional fixed-thread strategies
        strategies.add(new ParallelCPUCounter(2));
        strategies.add(new ParallelCPUCounter(4));
        strategies.add(new ParallelCPUCounter(8));
        if (CPU_CORES != 2 && CPU_CORES != 4 && CPU_CORES != 8) {
            strategies.add(new ParallelCPUCounter(CPU_CORES));
        }

        // Chunk granularity experiments: fixed and dynamic
        strategies.add(new ChunkedParallelCPUCounter(8, 32));
        strategies.add(new ChunkedParallelCPUCounter(8, 128));
        strategies.add(ChunkedParallelCPUCounter.dynamic(8));

        // ForkJoin granularity experiments: fixed thresholds and dynamic
        strategies.add(new ForkJoinCPUCounter(2_000));
        strategies.add(new ForkJoinCPUCounter(10_000));
        strategies.add(new ForkJoinCPUCounter());

        strategies.add(new ParallelStreamCounter());
        strategies.add(new VirtualThreadCounter(50));
        strategies.add(new VirtualThreadCounter(100));

        // GPU variants: exact string comparison and hash-based numeric comparison
        strategies.add(new ParallelGPUCounter());
        strategies.add(new ParallelGPUHashCounter());
        return strategies;
    }
}
