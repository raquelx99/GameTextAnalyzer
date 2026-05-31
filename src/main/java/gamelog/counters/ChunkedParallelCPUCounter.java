package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel CPU counter with explicit task chunking.
 *
 * Difference from ParallelCPUCounter:
 * - ParallelCPUCounter creates one chunk per worker thread.
 * - This class can create more chunks than threads, allowing experiments with
 *   task granularity and load balancing.
 *
 * Modes:
 * - fixed chunks: a predefined number, e.g. 32 or 128 chunks;
 * - dynamic chunks: calculated from text size and worker count.
 */
public class ChunkedParallelCPUCounter implements WordCounter {
    private final int threadCount;
    private final int configuredChunks;
    private final boolean dynamicChunks;
    private final ExecutorService pool;
    private int lastEffectiveChunks;

    public ChunkedParallelCPUCounter(int threadCount, int fixedChunks) {
        this.threadCount = Math.max(1, threadCount);
        this.configuredChunks = Math.max(this.threadCount, fixedChunks);
        this.dynamicChunks = false;
        this.lastEffectiveChunks = this.configuredChunks;
        this.pool = Executors.newFixedThreadPool(this.threadCount);
    }

    public static ChunkedParallelCPUCounter dynamic(int threadCount) {
        return new ChunkedParallelCPUCounter(threadCount, -1, true);
    }

    private ChunkedParallelCPUCounter(int threadCount, int chunks, boolean dynamic) {
        this.threadCount = Math.max(1, threadCount);
        this.configuredChunks = chunks;
        this.dynamicChunks = dynamic;
        this.lastEffectiveChunks = this.threadCount;
        this.pool = Executors.newFixedThreadPool(this.threadCount);
    }

    @Override
    public long count(String[] lines, String word) {
        int chunks = effectiveChunks(lines.length);
        this.lastEffectiveChunks = chunks;
        int chunkSize = (int) Math.ceil((double) lines.length / chunks);
        List<Future<Long>> futures = new ArrayList<>(chunks);

        for (int c = 0; c < chunks; c++) {
            final int start = c * chunkSize;
            final int end = Math.min(start + chunkSize, lines.length);
            if (start >= end) break;

            futures.add(pool.submit(() -> {
                long partial = 0;
                for (int i = start; i < end; i++) {
                    if (lines[i].equals(word)) partial++;
                }
                return partial;
            }));
        }

        long total = 0;
        for (Future<Long> f : futures) {
            try {
                total += f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while counting", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Error in chunked parallel count", e);
            }
        }
        return total;
    }

    private int effectiveChunks(int n) {
        if (n <= 0) return 1;
        if (!dynamicChunks) return Math.min(Math.max(threadCount, configuredChunks), n);
        // Dynamic heuristic: at least 4 tasks per worker, but not excessively small chunks.
        // Keeps overhead controlled while still allowing the scheduler to balance work.
        int targetChunkSize = 4096;
        int bySize = (int) Math.ceil((double) n / targetChunkSize);
        int byThreads = threadCount * 4;
        return Math.min(n, Math.max(byThreads, bySize));
    }

    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public String getName() {
        if (dynamicChunks) return "ParallelCPU-" + threadCount + "t-dynChunks";
        return "ParallelCPU-" + threadCount + "t-" + configuredChunks + "chunks";
    }

    @Override public StrategyFamily getFamily() { return StrategyFamily.PARALLEL_CPU; }
    @Override public int getParallelism() { return dynamicChunks ? lastEffectiveChunks : configuredChunks; }
    public int getThreadCount() { return threadCount; }
}
