package gamelog.counters;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Contador de virtual threads - divide o array de tokens em chunks e executa cada
 * chunk em uma virtual thread do Java 21.
 *
 * Incluído intencionalmente como experimento: virtual threads costumam ser
 * excelentes para workloads de I/O bloqueante, mas este projeto ajuda a verificar
 * se elas são benéficas para a contagem de palavras CPU-bound.
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
            throw new RuntimeException("Interrompido durante a contagem com virtual threads", e);
        } catch (ExecutionException e) {
            throw new RuntimeException("Erro na contagem com virtual threads", e);
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
