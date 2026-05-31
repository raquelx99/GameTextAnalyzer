package gamelog;

import gamelog.benchmark.BenchmarkResult;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads benchmark results from an existing CSV file.
 * Supports both the current modular format and the previous CSV layout.
 */
public class CsvReader {

    public static List<BenchmarkResult> read(String path) throws IOException {
        List<BenchmarkResult> list = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String header = br.readLine();
            if (header == null) return list;
            boolean modular = header.contains("strategy_id") && header.contains("family");

            String line;
            while ((line = br.readLine()) != null) {
                String[] cols = line.split(",");
                try {
                    if (modular) {
                        if (cols.length < 11) continue;
                        String file       = cols[0].trim();
                        String world      = cols[1].trim();
                        int total         = Integer.parseInt(cols[2].trim());
                        String word       = cols[3].trim();
                        String strategyId = cols[4].trim();
                        String method     = cols[5].trim();
                        String family     = cols[6].trim();
                        int parallelism   = parseInt(cols[7].trim(), 1);
                        int run           = Integer.parseInt(cols[8].trim());
                        long occ          = Long.parseLong(cols[9].trim());
                        double timeMs     = Double.parseDouble(cols[10].trim());

                        boolean hasPrepKernel = header.contains("preparation_ms") && header.contains("kernel_ms");
                        double prepMs = hasPrepKernel && cols.length > 11 ? parseDouble(cols[11].trim(), 0.0) : 0.0;
                        double kernelMs = hasPrepKernel && cols.length > 12 ? parseDouble(cols[12].trim(), 0.0) : 0.0;
                        int speedupIdx = hasPrepKernel ? 13 : 11;
                        int efficiencyIdx = hasPrepKernel ? 14 : 12;
                        int wordsIdx = hasPrepKernel ? 15 : 13;
                        int gpuIdx = hasPrepKernel ? 16 : 14;
                        boolean realGpu = cols.length > gpuIdx && Boolean.parseBoolean(cols[gpuIdx].trim());

                        BenchmarkResult r = new BenchmarkResult(file, world, total, word, strategyId, method,
                                family, parallelism, run, occ, timeMs, prepMs, kernelMs, realGpu);
                        if (cols.length > speedupIdx) r.speedup    = parseDouble(cols[speedupIdx].trim(), r.speedup);
                        if (cols.length > efficiencyIdx) r.efficiency = parseDouble(cols[efficiencyIdx].trim(), r.efficiency);
                        if (cols.length > wordsIdx) r.wordsPerMs = parseDouble(cols[wordsIdx].trim(), r.wordsPerMs);
                        list.add(r);
                    } else {
                        if (cols.length < 9) continue;
                        String file     = cols[0].trim();
                        String world    = cols[1].trim();
                        int total       = Integer.parseInt(cols[2].trim());
                        String word     = cols[3].trim();
                        String method   = cols[4].trim();
                        int threads     = parseInt(cols[5].trim(), 1);
                        int run         = Integer.parseInt(cols[6].trim());
                        long occ        = Long.parseLong(cols[7].trim());
                        double timeMs   = Double.parseDouble(cols[8].trim());

                        BenchmarkResult r = new BenchmarkResult(file, world, total, word, method, threads, run, occ, timeMs);
                        if (cols.length > 9) r.speedup    = parseDouble(cols[9].trim(), r.speedup);
                        if (cols.length > 10) r.efficiency = parseDouble(cols[10].trim(), r.efficiency);
                        if (cols.length > 11) r.wordsPerMs = parseDouble(cols[11].trim(), r.wordsPerMs);
                        list.add(r);
                    }
                } catch (RuntimeException ignored) {
                    // skip malformed rows
                }
            }
        }
        return list;
    }

    private static int parseInt(String s, int fallback) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return fallback; }
    }

    private static double parseDouble(String s, double fallback) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return fallback; }
    }
}
