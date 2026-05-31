package gamelog.counters;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join counter - divide-and-conquer sobre o array de tokens.
 *
 * Supports both dynamic and fixed thresholds so the benchmark can evaluate how
 * task granularity affects performance.
 */
public class ForkJoinCPUCounter implements WordCounter {

    private final ForkJoinPool pool;
    private final int parallelism;
    private final int fixedThreshold;
    private final boolean dynamicThreshold;
    private int lastThreshold;

    /** Dynamic threshold: adapts to text size and available processors. */
    public ForkJoinCPUCounter() {
        this(Runtime.getRuntime().availableProcessors(), -1, true);
    }

    /** Fixed threshold: useful for comparing manual granularities. */
    public ForkJoinCPUCounter(int threshold) {
        this(Runtime.getRuntime().availableProcessors(), threshold, false);
    }

    private ForkJoinCPUCounter(int parallelism, int threshold, boolean dynamic) {
        this.parallelism = Math.max(1, parallelism);
        this.fixedThreshold = threshold;
        this.dynamicThreshold = dynamic;
        this.lastThreshold = dynamic ? 0 : Math.max(1, threshold);
        this.pool = new ForkJoinPool(this.parallelism);
    }

    @Override
    public long count(String[] lines, String word) {
        int threshold = dynamicThreshold
                ? Math.max(500, lines.length / Math.max(1, parallelism * 2))
                : Math.max(1, fixedThreshold);
        this.lastThreshold = threshold;
        return pool.invoke(new CountTask(lines, word, 0, lines.length, threshold));
    }

    public void shutdown() { pool.shutdown(); }

    @Override
    public String getName() {
        return dynamicThreshold ? "ForkJoinCPU-dyn" : "ForkJoinCPU-th" + fixedThreshold;
    }

    @Override public StrategyFamily getFamily(){ return StrategyFamily.PARALLEL_CPU; }
    @Override public int getParallelism()      { return lastThreshold; }

    private static class CountTask extends RecursiveTask<Long> {
        private final String[] lines;
        private final String word;
        private final int start, end, threshold;

        CountTask(String[] lines, String word, int start, int end, int threshold) {
            this.lines = lines;
            this.word = word;
            this.start = start;
            this.end = end;
            this.threshold = threshold;
        }

        @Override
        protected Long compute() {
            int len = end - start;
            if (len <= threshold) {
                long count = 0;
                for (int i = start; i < end; i++) {
                    if (lines[i].equals(word)) count++;
                }
                return count;
            }
            int mid = start + len / 2;
            CountTask left = new CountTask(lines, word, start, mid, threshold);
            CountTask right = new CountTask(lines, word, mid, end, threshold);
            left.fork();
            long rightResult = right.compute();
            long leftResult = left.join();
            return leftResult + rightResult;
        }
    }
}
