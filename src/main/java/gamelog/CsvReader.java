package gamelog;

import gamelog.benchmark.BenchmarkResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads benchmark results from an existing CSV file.
 */
public class CsvReader {

    public static List<BenchmarkResult> read(String path) throws IOException {
        List<BenchmarkResult> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine(); // skip header
            if (header == null) return list;
            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                if (cols.length < 9) continue;
                try {
                    String file     = cols[0].trim();
                    String logType  = cols[1].trim();
                    int    total    = Integer.parseInt(cols[2].trim());
                    String event    = cols[3].trim();
                    String method   = cols[4].trim();
                    int    threads  = parseThreads(cols[5].trim());
                    int    run      = Integer.parseInt(cols[6].trim());
                    long   occ      = Long.parseLong(cols[7].trim());
                    double timeMs   = Double.parseDouble(cols[8].trim());

                    BenchmarkResult r = new BenchmarkResult(file, logType, total, event,
                                                            method, threads, run, occ, timeMs);
                    if (cols.length > 9) r.speedup    = Double.parseDouble(cols[9].trim());
                    if (cols.length > 10) r.efficiency = Double.parseDouble(cols[10].trim());
                    if (cols.length > 11) r.wordsPerMs = Double.parseDouble(cols[11].trim());
                    list.add(r);
                } catch (NumberFormatException ignored) {
                    // skip malformed rows
                }
            }
        }
        return list;
    }

    private static int parseThreads(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 1; }
    }
}
