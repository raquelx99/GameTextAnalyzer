package gamelog.benchmark;

import gamelog.SampleConfig;
import gamelog.counters.*;
import gamelog.utils.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Orquestra as execuções de benchmark para todas as estratégias, arquivos e palavras buscadas.
 *
 * Estratégia:
 *  1. Carrega/tokeniza o texto uma única vez.
 *  2. Aquece a JVM com uma execução descartada por estratégia (warmup).
 *  3. Executa cada estratégia {@code RUNS} vezes, coletando resultados cronometrados.
 *  4. Calcula speedup/eficiência em relação à mediana do SerialCPU.
 */
public class BenchmarkRunner {

    /** Número de execuções cronometradas por método (a execução de warmup é separada e descartada). */
    public static final int RUNS = 3;

    /** Workers disponíveis na máquina atual, usados para estratégias dinâmicas. */
    public static final int CPU_CORES = StrategyRegistry.CPU_CORES;

    /** Estratégias utilizadas no benchmark (na ordem de exibição). */
    private static final List<WordCounter> COUNTERS = Collections.unmodifiableList(StrategyRegistry.defaultStrategies());

    public static List<WordCounter> defaultCounters() {
        return COUNTERS;
    }

    /** Executa o benchmark completo para uma combinação de arquivo de texto e palavra. */
    public List<BenchmarkResult> run(String filePath, String eventToCount) {
        return run(filePath, eventToCount, COUNTERS);
    }

    /** Executa um benchmark usando uma lista personalizada de estratégias. */
    public List<BenchmarkResult> run(String filePath, String eventToCount, List<WordCounter> counters) {
        eventToCount = FileUtils.normalizeQuery(eventToCount);
        if (eventToCount.isEmpty()) {
            System.err.println("Search word is empty after normalization.");
            return Collections.emptyList();
        }

        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("File not found: " + filePath);
            return Collections.emptyList();
        }

        if (counters == null || counters.isEmpty()) {
            System.err.println("No benchmark strategies selected.");
            return Collections.emptyList();
        }

        System.out.println("\n==============================================");
        SampleConfig sample = SampleConfig.findByFileName(file.getName());
        if (sample != null) {
            System.out.printf(" World : %s%n", sample.gameWorld);
            System.out.printf(" Text  : %s%n", sample.title);
        }
        System.out.printf(" File  : %s%n", file.getName());
        System.out.printf(" Word  : %s%n", eventToCount);
        System.out.printf(" Strategies: %d%n", counters.size());
        System.out.println("==============================================");

        String[] lines;
        try {
            lines = FileUtils.readLines(filePath);
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            return Collections.emptyList();
        }

        String logType = deriveLogType(file.getName());
        int total = lines.length;

        System.out.println("  [warmup] executando uma passagem descartada por estratégia...");
        for (WordCounter counter : counters) {
            counter.count(lines, eventToCount);
        }

        List<BenchmarkResult> results = new ArrayList<>();
        double serialMedianMs = 1.0;

        for (WordCounter counter : counters) {
            List<Double> times = new ArrayList<>();
            String methodName = counter.getName();

            for (int run = 1; run <= RUNS; run++) {
                long t0 = System.nanoTime();
                long occ = counter.count(lines, eventToCount);
                double elapsedMs = (System.nanoTime() - t0) / 1_000_000.0;
                times.add(elapsedMs);

                methodName = counter.getName(); // o fallback do GPU pode atualizar o nome visível após count()

                BenchmarkResult r = new BenchmarkResult(
                        file.getName(),
                        logType,
                        total,
                        eventToCount,
                        counter.getId(),
                        methodName,
                        counter.getFamily().name(),
                        counter.getParallelism(),
                        run,
                        occ,
                        elapsedMs,
                        counter.getLastPreparationMs(),
                        counter.getLastKernelMs(),
                        counter.isRealGpu()
                );
                results.add(r);
            }

            if (methodName.equals("SerialCPU")) {
                serialMedianMs = Math.max(0.0001, median(times));
            }

            double med = median(times);
            System.out.printf(java.util.Locale.US, "  %-28s  %-16s median=%9.4f ms   runs=%s%n",
                    methodName, counter.getFamily().name(), med, times);
        }

        for (BenchmarkResult r : results) {
            double t = Math.max(0.0001, r.timeMs);
            double spd = serialMedianMs / t;
            int p = r.threads > 0 ? r.threads : 1;
            r.speedup = spd;
            if (r.family.equals(StrategyFamily.PARALLEL_CPU.name()) || r.family.equals(StrategyFamily.VIRTUAL_THREADS.name())) {
                r.efficiency = spd / p;
            } else {
                r.efficiency = spd;
            }
            r.wordsPerMs = (double) total / t;
        }

        printRanking(results, file.getName(), eventToCount);

        return results;
    }

    /**
     * Libera recursos dos counters com estado (pool de threads, contexto OpenCL).
     * Deve ser chamado UMA vez ao final de TODOS os benchmarks, não entre arquivos.
     * Chamar durante o benchmark causaria RejectedExecutionException nas próximas runs.
     */
    public void releaseCounters(List<WordCounter> counters) {
        for (WordCounter counter : counters) {
            if (counter instanceof ParallelCPUCounter pcc) pcc.shutdown();
            if (counter instanceof ChunkedParallelCPUCounter cpc) cpc.shutdown();
            if (counter instanceof ForkJoinCPUCounter fjc) fjc.shutdown();
            if (counter instanceof ParallelGPUCounter gpu) gpu.release();
            if (counter instanceof ParallelGPUHashCounter gpuHash) gpuHash.release();
        }
    }

    private void printRanking(List<BenchmarkResult> results, String fileName, String event) {
        Map<String, Double> medians = new LinkedHashMap<>();
        Map<String, List<Double>> grouped = new LinkedHashMap<>();
        for (BenchmarkResult r : results) {
            grouped.computeIfAbsent(r.method, k -> new ArrayList<>()).add(r.timeMs);
        }
        grouped.forEach((m, ts) -> medians.put(m, median(ts)));

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(medians.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        System.out.printf("%n  -- Ranking: %s / palavra '%s' --%n", fileName, event);
        for (int i = 0; i < sorted.size(); i++) {
            System.out.printf(java.util.Locale.US, "  %2d) %-28s  %9.4f ms%n",
                    i + 1,
                    sorted.get(i).getKey(),
                    sorted.get(i).getValue());
        }
    }

    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n == 0) return 0.0;
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static String deriveLogType(String name) {
        SampleConfig sample = SampleConfig.findByFileName(name);
        if (sample != null) return sample.gameWorld;
        return "generic";
    }
}
