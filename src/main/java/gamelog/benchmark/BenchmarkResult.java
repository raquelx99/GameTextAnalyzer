package gamelog.benchmark;

import gamelog.counters.StrategyFamily;

/**
 * Armazena o resultado de uma única execução de benchmark.
 */
public class BenchmarkResult {

    public final String  file;
    public final String  logType;
    public final int     totalWords;
    public final String  wordSearched;
    public final String  strategyId;
    public final String  method;
    public final String  family;
    public final int     threads;
    public final int     run;
    public final long    occurrences;
    public final double  timeMs;
    public final double  preparationMs;
    public final double  kernelMs;
    public final boolean realGpu;

    // Métricas derivadas (definidas após todos os resultados Serial serem conhecidos)
    public double speedup         = 1.0;
    public double efficiency      = 1.0;
    public double wordsPerMs      = 0.0;

    public BenchmarkResult(String file, String logType, int totalWords,
                           String wordSearched, String strategyId, String method,
                           String family, int threads, int run, long occurrences,
                           double timeMs, double preparationMs, double kernelMs, boolean realGpu) {
        this.file          = file;
        this.logType       = logType;
        this.totalWords    = totalWords;
        this.wordSearched  = wordSearched;
        this.strategyId    = strategyId;
        this.method        = method;
        this.family        = family;
        this.threads       = threads;
        this.run           = run;
        this.occurrences   = occurrences;
        this.timeMs        = timeMs;
        this.preparationMs = preparationMs;
        this.kernelMs      = kernelMs;
        this.realGpu       = realGpu;
        this.wordsPerMs    = timeMs > 0 ? (double) totalWords / timeMs : 0;
    }

    /** Construtor retrocompatível usado por leitores de CSV mais antigos e caminhos de código legados. */
    public BenchmarkResult(String file, String logType, int totalWords,
                           String wordSearched, String method, int threads,
                           int run, long occurrences, double timeMs) {
        this(file, logType, totalWords, wordSearched,
                inferId(method), method, inferFamily(method), threads, run,
                occurrences, timeMs, 0.0, 0.0, method.equals("ParallelGPU") || method.equals("ParallelGPU-String") || method.equals("ParallelGPU-Hash"));
    }

    /** Linha de cabeçalho do CSV. */
    public static String csvHeader() {
        return "file,world,total_words,word_searched,strategy_id,method,family,parallelism,run," +
               "occurrences,time_ms,preparation_ms,kernel_ms,speedup,efficiency,words_per_ms,is_real_gpu";
    }

    /** Retorna este resultado como uma linha CSV. */
    public String toCsvRow() {
        return String.format(java.util.Locale.US,
                "%s,%s,%d,%s,%s,%s,%s,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%s",
                file, logType, totalWords, wordSearched, strategyId, method, family, threads, run,
                occurrences, timeMs, preparationMs, kernelMs, speedup, efficiency, wordsPerMs, realGpu);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US, "%-28s %,7d ocorrências em %8.4f ms  (speedup=%.2fx)",
                method + ":", occurrences, timeMs, speedup);
    }

    public static String inferId(String method) {
        return method.toLowerCase().replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String inferFamily(String method) {
        if (method.equals("SerialCPU") || method.equals("SerialStream")) return StrategyFamily.SERIAL.name();
        if (method.startsWith("ParallelCPU") || method.startsWith("ForkJoin")) return StrategyFamily.PARALLEL_CPU.name();
        if (method.startsWith("ParallelStream")) return StrategyFamily.PARALLEL_STREAM.name();
        if (method.startsWith("VirtualThreads")) return StrategyFamily.VIRTUAL_THREADS.name();
        if (method.startsWith("ParallelGPU")) return StrategyFamily.GPU_OPENCL.name();
        return "OTHER";
    }
}
