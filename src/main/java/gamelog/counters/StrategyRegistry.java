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
        strategies.add(new ParallelCPUCounter(2));
        strategies.add(new ParallelCPUCounter(4));
        strategies.add(new ParallelCPUCounter(8));
        if (CPU_CORES != 2 && CPU_CORES != 4 && CPU_CORES != 8) {
            strategies.add(new ParallelCPUCounter(CPU_CORES));
        }
        strategies.add(new ForkJoinCPUCounter());
        strategies.add(new ParallelStreamCounter());
        strategies.add(new VirtualThreadCounter(50));
        strategies.add(new VirtualThreadCounter(100));
        strategies.add(new ParallelGPUCounter());
        return strategies;
    }
}
