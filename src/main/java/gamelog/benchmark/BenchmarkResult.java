package gamelog.benchmark;

/**
 * Holds the result of a single benchmark run.
 */
public class BenchmarkResult {

    public final String  file;
    public final String  logType;
    public final int     totalWords;
    public final String  wordSearched;
    public final String  method;
    public final int     threads;
    public final int     run;
    public final long    occurrences;
    public final double  timeMs;

    // Derived metrics (set after all Serial results are known)
    public double speedup         = 1.0;
    public double efficiency      = 1.0;
    public double wordsPerMs     = 0.0;

    public BenchmarkResult(String file, String logType, int totalWords,
                           String wordSearched, String method, int threads,
                           int run, long occurrences, double timeMs) {
        this.file          = file;
        this.logType       = logType;
        this.totalWords   = totalWords;
        this.wordSearched = wordSearched;
        this.method        = method;
        this.threads       = threads;
        this.run           = run;
        this.occurrences   = occurrences;
        this.timeMs        = timeMs;
        this.wordsPerMs   = timeMs > 0 ? (double) totalWords / timeMs : 0;
    }

    /** CSV header row. */
    public static String csvHeader() {
        return "file,world,total_words,word_searched,method,threads,run," +
               "occurrences,time_ms,speedup,efficiency,words_per_ms";
    }

    /** Returns this result as a CSV row. */
    public String toCsvRow() {
        return String.format(java.util.Locale.US, "%s,%s,%d,%s,%s,%d,%d,%d,%.4f,%.4f,%.4f,%.2f",
                file, logType, totalWords, wordSearched, method, threads, run,
                occurrences, timeMs, speedup, efficiency, wordsPerMs);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.US, "%-24s %,7d ocorrências em %8.4f ms  (speedup=%.2fx)",
                method + ":", occurrences, timeMs, speedup);
    }
}
