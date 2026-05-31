package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Contador CPU paralelo com chunking explícito de tarefas.
 *
 * Diferença em relação ao ParallelCPUCounter:
 * - ParallelCPUCounter cria um chunk por thread worker.
 * - Esta classe pode criar mais chunks do que threads, permitindo experimentos com
 *   granularidade de tarefas e balanceamento de carga.
 *
 * Modos:
 * - chunks fixos: um número predefinido, por ex. 32 ou 128 chunks;
 * - chunks dinâmicos: calculados a partir do tamanho do texto e do número de workers.
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
                throw new RuntimeException("Interrompido durante a contagem", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Erro na contagem paralela com chunks", e);
            }
        }
        return total;
    }

    private int effectiveChunks(int n) {
        if (n <= 0) return 1;
        if (!dynamicChunks) return Math.min(Math.max(threadCount, configuredChunks), n);
        // Heurística dinâmica: pelo menos 4 tarefas por worker, mas sem chunks excessivamente pequenos.
        // Mantém o overhead sob controle enquanto ainda permite ao escalonador balancear a carga.
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
