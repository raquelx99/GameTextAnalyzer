package gamelog.counters;

/**
 * Agrupa as estratégias de contagem pelo tipo de execução/paralelização utilizado.
 */
public enum StrategyFamily {
    SERIAL("Serial"),
    PARALLEL_CPU("CPU paralela"),
    PARALLEL_STREAM("Stream paralela"),
    VIRTUAL_THREADS("Virtual threads"),
    GPU_OPENCL("GPU/OpenCL");

    private final String label;

    StrategyFamily(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
