package gamelog.counters;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/**
 * Fork/Join counter — divide-and-conquer sobre o array de tokens.
 *
 * CORREÇÃO 1: pool dedicado em vez do commonPool().
 *   O ForkJoinPool.commonPool() é compartilhado com ParallelStream.
 *   Usar o pool comum causava contenção entre os dois counters durante
 *   o benchmark, corrompendo as medições de ambos.
 *
 * CORREÇÃO 2: threshold calibrado ao tamanho real do array.
 *   Um threshold fixo de 20.000 é grande demais para textos pequenos
 *   (Dracula tem ~15k linhas — o ForkJoin nunca chegava a dividir nada,
 *   rodando praticamente serial mas com overhead de RecursiveTask).
 *   O novo threshold adapta-se: divide o array em ~2× o nº de cores,
 *   garantindo sempre algum paralelismo independente do tamanho do texto.
 */
public class ForkJoinCPUCounter implements WordCounter {

    // Pool dedicado com todos os cores disponíveis.
    // Criado uma vez e reutilizado em todas as chamadas.
    private final ForkJoinPool pool;
    private final int parallelism;

    public ForkJoinCPUCounter() {
        this.parallelism = Runtime.getRuntime().availableProcessors();
        this.pool = new ForkJoinPool(parallelism);
    }

    @Override
    public long count(String[] lines, String word) {
        // Threshold adaptativo: cada "folha" processa ~tamanho/2*cores linhas.
        // Isso garante que o ForkJoin sempre cria múltiplas tarefas paralelas,
        // independente de o texto ter 15k ou 400k linhas.
        int threshold = Math.max(500, lines.length / (parallelism * 2));
        return pool.invoke(new CountTask(lines, word, 0, lines.length, threshold));
    }

    public void shutdown() {
        pool.shutdown();
    }

    @Override
    public String getName()          { return "ForkJoinCPU"; }
    @Override
    public StrategyFamily getFamily(){ return StrategyFamily.PARALLEL_CPU; }
    @Override
    public int getParallelism()      { return parallelism; }

    // ── RecursiveTask ──────────────────────────────────────────────────────

    private static class CountTask extends RecursiveTask<Long> {
        private final String[] lines;
        private final String   word;
        private final int      start, end, threshold;

        CountTask(String[] lines, String word, int start, int end, int threshold) {
            this.lines     = lines;
            this.word      = word;
            this.start     = start;
            this.end       = end;
            this.threshold = threshold;
        }

        @Override
        protected Long compute() {
            int len = end - start;
            // Abaixo do threshold: conta sequencialmente (sem overhead de fork)
            if (len <= threshold) {
                long count = 0;
                for (int i = start; i < end; i++) {
                    if (lines[i].equals(word)) count++;
                }
                return count;
            }
            // Acima: divide ao meio e executa em paralelo
            int mid = start + len / 2;
            CountTask left  = new CountTask(lines, word, start, mid, threshold);
            CountTask right = new CountTask(lines, word, mid,   end, threshold);
            left.fork();                     // esquerda roda em paralelo
            long rightResult = right.compute(); // direita roda nesta thread
            long leftResult  = left.join();     // espera a esquerda
            return leftResult + rightResult;
        }
    }
}
