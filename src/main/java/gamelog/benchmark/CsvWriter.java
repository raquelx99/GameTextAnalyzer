package gamelog.benchmark;

import java.io.*;
import java.util.List;

/**
 * Grava resultados de benchmark em um arquivo CSV para análise e geração de gráficos posterior.
 */
public class CsvWriter {

    /**
     * Acrescenta (ou cria) um arquivo CSV com os resultados fornecidos.
     *
     * @param outputPath caminho para o arquivo CSV
     * @param results    lista de resultados a gravar
     * @param append     se {@code true}, acrescenta a um arquivo existente
     * @throws IOException se o arquivo não puder ser gravado
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
