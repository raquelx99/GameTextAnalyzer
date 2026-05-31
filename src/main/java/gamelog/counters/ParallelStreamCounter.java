package gamelog.counters;

import java.util.Arrays;

/**
 * Usa a API de parallel Stream do Java, que executa sobre o ForkJoinPool comum.
 * Útil como baseline paralelo de alto nível em comparação com chunking manual.
 */
public class ParallelStreamCounter implements WordCounter {
    @Override
    public long count(String[] lines, String word) {
        return Arrays.stream(lines).parallel().filter(word::equals).count();
    }

    @Override
    public String getName() {
        return "ParallelStream";
    }

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.PARALLEL_STREAM;
    }

    @Override
    public int getParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
