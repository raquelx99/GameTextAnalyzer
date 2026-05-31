package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Virtual-thread counter - divides the token array into chunks and runs each
 * chunk in a Java 21 virtual thread.
 *
 * This is intentionally included as an experiment: virtual threads are often
 * excellent for blocking I/O workloads, but this project helps verify whether
 * they are beneficial for CPU-bound word counting.
 */
public class VirtualThreadCounter implements WordCounter {

    private final int chunks;

    public VirtualThreadCounter(int chunks) {
        this.chunks = Math.max(1, chunks);
    }

    @Override
    public long count(String[] lines, String word) {
        int effectiveChunks = Math.min(chunks, Math.max(1, lines.length));
        int chunkSize = (int) Math.ceil((double) lines.length / effectiveChunks);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Long>> futures = new ArrayList<>(effectiveChunks);

            for (int c = 0; c < effectiveChunks; c++) {
                final int start = c * chunkSize;
                final int end = Math.min(start + chunkSize, lines.length);
                if (start >= end) break;

                futures.add(executor.submit(() -> {
                    long partial = 0;
                    for (int i = start; i < end; i++) {
                        if (lines[i].equals(word)) partial++;
                    }
                    return partial;
                }));
            }

            long total = 0;
            for (Future<Long> f : futures) {
                total += f.get();
            }
            return total;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while counting with virtual threads", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error in virtual-thread count", e);
        }
    }

    @Override
    public String getName() {
        return "VirtualThreads-" + chunks + "chunks";
    }

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.VIRTUAL_THREADS;
    }

    @Override
    public int getParallelism() {
        return chunks;
    }
}
