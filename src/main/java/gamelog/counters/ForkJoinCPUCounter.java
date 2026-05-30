package gamelog.counters;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join counter — recursively splits the array into smaller pieces.
 * Useful for testing a divide-and-conquer strategy against fixed chunks.
 */
public class ForkJoinCPUCounter implements WordCounter {

    private final int threshold;

    public ForkJoinCPUCounter() {
        this(20_000);
    }

    public ForkJoinCPUCounter(int threshold) {
        this.threshold = Math.max(1_000, threshold);
    }

    @Override
    public long count(String[] lines, String word) {
        return ForkJoinPool.commonPool().invoke(new CountTask(lines, word, 0, lines.length, threshold));
    }

    @Override
    public String getName() {
        return "ForkJoinCPU";
    }

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.PARALLEL_CPU;
    }

    @Override
    public int getParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }

    private static class CountTask extends RecursiveTask<Long> {
        private final String[] lines;
        private final String word;
        private final int start;
        private final int end;
        private final int threshold;

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
