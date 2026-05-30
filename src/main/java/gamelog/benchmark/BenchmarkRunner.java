package gamelog.benchmark;

import gamelog.SampleConfig;
import gamelog.counters.*;
import gamelog.utils.FileUtils;

import java.io.File;
import java.util.*;

/**
 * Orchestrates benchmark runs across all counters, files, and events.
 *
 * Strategy:
 *  1. Warm up the JVM with one discarded run per counter.
 *  2. Run each counter {@code RUNS} times, collecting timed results.
 *  3. Compute speedup/efficiency relative to the SerialCPU median.
 */
public class BenchmarkRunner {

    /** Number of timed runs per method (warmup run is separate and discarded). */
    public static final int RUNS = 3;

    /** Threads available on the current machine, used for the dynamic counter. */
    public static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /** Counters used in the benchmark (in display order). */
    private static final List<WordCounter> COUNTERS = List.of(
            new SerialCPUCounter(),
            new ParallelCPUCounter(2),
            new ParallelCPUCounter(4),
            new ParallelCPUCounter(8),
            new ParallelCPUCounter(CPU_CORES),
            new ParallelGPUCounter()
    );

    /**
     * Runs the full benchmark for one log file / event combination.
     *
     * @param filePath     path to the .txt log file
     * @param eventToCount event name to search for
     * @return list of all {@link BenchmarkResult} from this run
     */
    public List<BenchmarkResult> run(String filePath, String eventToCount) {
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

        System.out.println("\n==============================================");
        SampleConfig sample = SampleConfig.findByFileName(file.getName());
        if (sample != null) {
            System.out.printf(" World : %s%n", sample.gameWorld);
            System.out.printf(" Text  : %s%n", sample.title);
        }
        System.out.printf(" File  : %s%n", file.getName());
        System.out.printf(" Word  : %s%n", eventToCount);
        System.out.println("==============================================");

        // Load lines once — shared across all counters
        String[] lines;
        try {
            lines = FileUtils.readLines(filePath);
        } catch (Exception e) {
            System.err.println("Error reading file: " + e.getMessage());
            return Collections.emptyList();
        }

        String logType = deriveLogType(file.getName());
        int    total   = lines.length;

        // ── JVM Warmup ───────────────────────────────────────────────────────
        System.out.println("  [warmup] running one discarded pass per counter...");
        for (WordCounter counter : COUNTERS) {
            counter.count(lines, eventToCount); // result discarded
        }

        // ── Timed runs ───────────────────────────────────────────────────────
        List<BenchmarkResult> results = new ArrayList<>();
        double serialMedianMs = 1.0; // placeholder, updated below

        for (WordCounter counter : COUNTERS) {
            List<Double> times = new ArrayList<>();

            for (int run = 1; run <= RUNS; run++) {
                long t0   = System.nanoTime();
                long occ  = counter.count(lines, eventToCount);
                double elap = (System.nanoTime() - t0) / 1_000_000.0;
                times.add(elap);

                int threadCount = (counter instanceof ParallelCPUCounter pcc)
                                  ? pcc.getThreadCount() : 1;

                BenchmarkResult r = new BenchmarkResult(
                        file.getName(), logType, total, eventToCount,
                        counter.getName(), threadCount, run, occ, elap);
                results.add(r);
            }

            // Update serial baseline after SerialCPU runs
            if (counter instanceof SerialCPUCounter) {
                serialMedianMs = Math.max(0.0001, median(times));
            }

            // Print summary line for this counter
            double med = median(times);
            System.out.printf(java.util.Locale.US, "  %-24s  median=%9.4f ms   runs=%s%n",
                    counter.getName(), med, times);
        }

        // ── Compute derived metrics ──────────────────────────────────────────
        for (BenchmarkResult r : results) {
            double t   = Math.max(0.0001, r.timeMs);
            double spd = serialMedianMs / t;
            int    thr = r.threads > 0 ? r.threads : 1;
            r.speedup     = spd;
            r.efficiency  = (r.method.startsWith("ParallelCPU")) ? spd / thr : spd;
            r.wordsPerMs = (double) total / t;
        }

        // ── Print ranking ────────────────────────────────────────────────────
        printRanking(results, file.getName(), eventToCount);

        return results;
    }

    /** Prints a medal-style ranking table for one file run. */
    private void printRanking(List<BenchmarkResult> results, String fileName, String event) {
        // Median time per method
        Map<String, Double> medians = new LinkedHashMap<>();
        Map<String, List<Double>> grouped = new LinkedHashMap<>();
        for (BenchmarkResult r : results) {
            grouped.computeIfAbsent(r.method, k -> new ArrayList<>()).add(r.timeMs);
        }
        grouped.forEach((m, ts) -> medians.put(m, median(ts)));

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(medians.entrySet());
        sorted.sort(Map.Entry.comparingByValue());

        String[] medals = {"1)", "2)", "3)", "4)", "5)", "6)"};
        System.out.printf("%n  -- Ranking: %s / palavra '%s' --%n", fileName, event);
        for (int i = 0; i < sorted.size(); i++) {
            System.out.printf(java.util.Locale.US, "  %s %-24s  %9.4f ms%n",
                    medals[Math.min(i, medals.length - 1)],
                    sorted.get(i).getKey(),
                    sorted.get(i).getValue());
        }
    }

    /** Returns the median value from a list of longs. */
    private static double median(List<Double> values) {
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        if (n == 0) return 0.0;
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    /** Derives a human-readable dataset/game-world type from the file name. */
    private static String deriveLogType(String name) {
        SampleConfig sample = SampleConfig.findByFileName(name);
        if (sample != null) return sample.gameWorld;

        String lower = name.toLowerCase();
        if (lower.contains("tutorial"))   return "tutorial-extra";
        if (lower.contains("explor"))     return "exploration-extra";
        if (lower.contains("combat"))     return "combat-extra";
        if (lower.contains("boss"))       return "bossfight-extra";
        return "generic";
    }
}
