package gamelog.counters;

/**
 * Interface comum para todas as estratégias de benchmark de contagem de palavras.
 *
 * Novas estratégias precisam apenas implementar esta interface e ser registradas em
 * {@link StrategyRegistry}. O benchmark runner, o CSV writer e o sistema de gráficos
 * irão tratá-las da mesma forma que os métodos existentes.
 */
public interface WordCounter {

    /** Conta as ocorrências de {@code word} no array de tokens normalizado. */
    long count(String[] lines, String word);

    /** Nome legível para este contador (usado nos relatórios). */
    String getName();

    /** ID estável usado nos filtros do CSV. */
    default String getId() {
        return getName().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
    }

    /** Família da estratégia, usada para gerar gráficos por categoria. */
    default StrategyFamily getFamily() {
        return StrategyFamily.SERIAL;
    }

    /** Número de workers/threads/chunks representados por esta estratégia. */
    default int getParallelism() {
        return 1;
    }

    /** Verdadeiro apenas quando uma estratégia GPU executou de fato na GPU/OpenCL, sem fallback. */
    default boolean isRealGpu() {
        return false;
    }

    /** Opcional: tempo gasto preparando os dados antes da contagem/kernel GPU. */
    default double getLastPreparationMs() {
        return 0.0;
    }

    /** Opcional: tempo gasto na fase principal de computação/kernel. */
    default double getLastKernelMs() {
        return 0.0;
    }
}
