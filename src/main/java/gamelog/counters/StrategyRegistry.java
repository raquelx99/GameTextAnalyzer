package gamelog.counters;

import java.util.ArrayList;
import java.util.List;

/**
 * Registro central das estratégias de benchmark.
 *
 * Para adicionar um novo método à aplicação, crie uma nova implementação de WordCounter
 * e adicione aqui. O benchmark runner, o CSV e os gráficos o incluirão automaticamente.
 */
public final class StrategyRegistry {
    private StrategyRegistry() {}

    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    public static List<WordCounter> defaultStrategies() {
        List<WordCounter> strategies = new ArrayList<>();
        strategies.add(new SerialCPUCounter());
        strategies.add(new SerialStreamCounter());

        // Estratégias tradicionais com threads fixas
        strategies.add(new ParallelCPUCounter(2));
        strategies.add(new ParallelCPUCounter(4));
        strategies.add(new ParallelCPUCounter(8));
        if (CPU_CORES != 2 && CPU_CORES != 4 && CPU_CORES != 8) {
            strategies.add(new ParallelCPUCounter(CPU_CORES));
        }

        // Experimentos de granularidade de chunk: fixos e dinâmicos
        strategies.add(new ChunkedParallelCPUCounter(8, 32));
        strategies.add(new ChunkedParallelCPUCounter(8, 128));
        strategies.add(ChunkedParallelCPUCounter.dynamic(8));

        // Experimentos de granularidade ForkJoin: thresholds fixos e dinâmico
        strategies.add(new ForkJoinCPUCounter(2_000));
        strategies.add(new ForkJoinCPUCounter(10_000));
        strategies.add(new ForkJoinCPUCounter());

        strategies.add(new ParallelStreamCounter());
        strategies.add(new VirtualThreadCounter(50));
        strategies.add(new VirtualThreadCounter(100));

        // Variantes GPU: comparação exata de string e comparação numérica baseada em hash
        strategies.add(new ParallelGPUCounter());
        strategies.add(new ParallelGPUHashCounter());
        return strategies;
    }
}
