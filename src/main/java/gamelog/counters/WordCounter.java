package gamelog.counters;

/**
 * Common interface for all word-counting benchmark strategies.
 *
 * New strategies only need to implement this interface and be registered in
 * {@link StrategyRegistry}. The benchmark runner, CSV writer and chart system
 * will then treat them in the same way as the existing methods.
 */
public interface WordCounter {

    /** Counts occurrences of {@code word} in the normalized token array. */
    long count(String[] lines, String word);

    /** Human-readable name for this counter (used in reports). */
    String getName();

    /** Stable ID used in CSV/filters. */
    default String getId() {
        return getName().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /** Family of the strategy, used to generate per-branch charts. */
    default StrategyFamily getFamily() {
        return StrategyFamily.SERIAL;
    }

    /** Number of workers/threads/chunks represented by this strategy. */
    default int getParallelism() {
        return 1;
    }

    /** True only when a GPU strategy actually ran on GPU/OpenCL, not fallback. */
    default boolean isRealGpu() {
        return false;
    }

    /** Optional: time spent preparing data before the actual count/GPU kernel. */
    default double getLastPreparationMs() {
        return 0.0;
    }

    /** Optional: time spent in the core computation/kernel phase. */
    default double getLastKernelMs() {
        return 0.0;
    }
}
