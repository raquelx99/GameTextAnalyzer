package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel CPU counter — splits the lines array across N threads,
 * counts in parallel, then sums partial results.
 */
public class ParallelCPUCounter implements WordCounter {

    private final int threadCount;

    public ParallelCPUCounter(int threadCount) {
        this.threadCount = threadCount;
    }

    @Override
    public long count(String[] lines, String word) {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<Long>> futures = new ArrayList<>(threadCount);

        int chunkSize = (int) Math.ceil((double) lines.length / threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int start = t * chunkSize;
            final int end   = Math.min(start + chunkSize, lines.length);

            if (start >= lines.length) break;

            futures.add(executor.submit(() -> {
                long partial = 0;
                for (int i = start; i < end; i++) {
                    if (lines[i].equals(word)) partial++;
                }
                return partial;
            }));
        }

        executor.shutdown();

        long total = 0;
        for (Future<Long> f : futures) {
            try {
                total += f.get();
            } catch (InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Error in parallel count", e);
            }
        }
        return total;
    }

    @Override
    public String getName() {
        return "ParallelCPU-" + threadCount + "t";
    }

    public int getThreadCount() {
        return threadCount;
    }
}
