package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Parallel CPU counter - pool de threads reutilizável criado uma única vez
 * no construtor e reaproveitado em todas as chamadas a count().
 *
 * CORREÇÃO: criar o ExecutorService dentro de count() gerava overhead
 * de criação/destruição de threads a cada chamada, dominando o tempo
 * medido especialmente em textos pequenos.
 */
public class ParallelCPUCounter implements WordCounter {

    private final int threadCount;

    // Pool criado UMA vez e reutilizado em todas as execuções do benchmark.
    // Isso elimina o overhead de criação de threads por chamada.
    private final ExecutorService pool;

    public ParallelCPUCounter(int threadCount) {
        this.threadCount = Math.max(1, threadCount);
        this.pool = Executors.newFixedThreadPool(this.threadCount);
    }

    @Override
    public long count(String[] lines, String word) {
        int chunkSize = (int) Math.ceil((double) lines.length / threadCount);
        List<Future<Long>> futures = new ArrayList<>(threadCount);

        for (int t = 0; t < threadCount; t++) {
            final int start = t * chunkSize;
            final int end   = Math.min(start + chunkSize, lines.length);
            if (start >= lines.length) break;

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
                throw new RuntimeException("Error in parallel count", e);
            }
        }
        return total;
    }

    /**
     * Libera o pool ao final. Chamado automaticamente se o counter for
     * usado em try-with-resources ou explicitamente pelo BenchmarkRunner.
     */
    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public String getName()          { return "ParallelCPU-" + threadCount + "t"; }
    @Override
    public StrategyFamily getFamily(){ return StrategyFamily.PARALLEL_CPU; }
    @Override
    public int getParallelism()      { return threadCount; }
    public int getThreadCount()      { return threadCount; }
}
