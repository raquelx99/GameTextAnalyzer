package gamelog.benchmark;

import java.io.*;
import java.util.List;

/**
 * Writes benchmark results to a CSV file for later analysis/charting.
 */
public class CsvWriter {

    /**
     * Appends (or creates) a CSV file with the provided results.
     *
     * @param outputPath path to the CSV file
     * @param results    list of results to write
     * @param append     if {@code true}, appends to an existing file
     * @throws IOException if the file cannot be written
     */
    public static void write(String outputPath, List<BenchmarkResult> results, boolean append)
            throws IOException {

        File file = new File(outputPath);
        file.getParentFile().mkdirs();

        boolean writeHeader = !append || !file.exists() || file.length() == 0;

        try (PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file, append)))) {
            if (writeHeader) {
                pw.println(BenchmarkResult.csvHeader());
            }
            for (BenchmarkResult r : results) {
                pw.println(r.toCsvRow());
            }
        }

        System.out.printf("%n  CSV saved → %s%n", file.getAbsolutePath());
    }
}
