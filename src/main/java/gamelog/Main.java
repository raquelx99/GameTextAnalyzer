package gamelog;

import gamelog.benchmark.*;
import gamelog.ui.ChartGenerator;
import gamelog.ui.MainWindow;

import javax.swing.*;
import java.io.*;
import java.util.*;

/**
 * Entry point.
 *  - Default: launches the Swing GUI
 *  - With --console flag: runs the original text menu
 */
public class Main {

    static final String DATA_DIR    = "data";
    static final String RESULTS_DIR = "results";
    static final String CHARTS_DIR  = "charts";
    static final String CSV_PATH    = RESULTS_DIR + "/resultados.csv";

    static final String SAMPLE_DIR = DATA_DIR + "/samples";

    /** Official assignment samples interpreted as narrative game worlds. */
    static final String[][] OFFICIAL_FILES = SampleConfig.officialBenchmarkEntries();

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equalsIgnoreCase("--console")) {
            System.setProperty("java.awt.headless", "true");
            runConsole();
            return;
        }
        setupLookAndFeel();
        MainWindow.launch();
    }

    private static void setupLookAndFeel() {
        try {
            // FlatLaf is optional. The project still runs with pure Swing if the jar is absent.
            Class<?> laf = Class.forName("com.formdev.flatlaf.FlatDarkLaf");
            laf.getMethod("setup").invoke(null);
        } catch (Exception ignored) {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignoredToo) {}
        }
    }

    static void runConsole() throws Exception {
        printBanner();
        Scanner sc = new Scanner(System.in);
        boolean running = true;
        while (running) {
            printMenu();
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> checkOfficialSamples();
                case "2" -> runFullBenchmark();
                case "3" -> runCustomBenchmark(sc);
                case "4" -> generateChartsOnly();
                case "5" -> { running = false; System.out.println("\n  Ate a proxima!\n"); }
                default  -> System.out.println("  Opcao invalida.");
            }
        }
    }

    static void checkOfficialSamples() {
        SampleConfig.ensureSampleDirectory();
        System.out.println("\n  Amostras oficiais esperadas em: " + SAMPLE_DIR);
        for (SampleConfig sample : SampleConfig.OFFICIAL_SAMPLES) {
            File f = new File(sample.filePath);
            System.out.printf("  %-28s  %-32s  %s%n",
                    f.exists() ? "[OK]" : "[FALTA]",
                    f.getName(),
                    sample.gameWorld);
            System.out.println("      Palavra padrão: " + sample.defaultWord);
            System.out.println("      Sugestões: " + String.join(", ", sample.suggestedWords));
        }
    }

    static void runFullBenchmark() throws Exception {
        ensureOfficialSamplesExist();
        BenchmarkRunner runner = new BenchmarkRunner();
        List<BenchmarkResult> allResults = new ArrayList<>();
        new File(CSV_PATH).delete();
        for (String[] entry : OFFICIAL_FILES) {
            List<BenchmarkResult> results = runner.run(entry[0], entry[1]);
            allResults.addAll(results);
            CsvWriter.write(CSV_PATH, results, true);
        }
        System.out.println("\n  Benchmark concluido. Gerando graficos...");
        ChartGenerator.generateAll(allResults, CHARTS_DIR);
        System.out.println("  Tudo pronto!");
    }

    static void runCustomBenchmark(Scanner sc) throws Exception {
        ensureOfficialSamplesExist();
        File dir = new File(SAMPLE_DIR);
        File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
        if (files == null || files.length == 0) { System.out.println("  Nenhum arquivo encontrado."); return; }
        for (int i = 0; i < files.length; i++) System.out.printf("    [%d] %s%n", i + 1, files[i].getName());
        System.out.print("  Escolha: ");
        int idx = Integer.parseInt(sc.nextLine().trim()) - 1;
        if (idx < 0 || idx >= files.length) { System.out.println("  Indice invalido."); return; }
        System.out.print("  Palavra a buscar: ");
        String event = gamelog.utils.FileUtils.normalizeQuery(sc.nextLine().trim());
        BenchmarkRunner runner = new BenchmarkRunner();
        List<BenchmarkResult> r = runner.run(files[idx].getAbsolutePath(), event);
        CsvWriter.write(CSV_PATH, r, true);
        System.out.print("  Gerar graficos? (s/n): ");
        if (sc.nextLine().trim().equalsIgnoreCase("s")) ChartGenerator.generateAll(r, CHARTS_DIR);
    }

    static void generateChartsOnly() throws Exception {
        File csv = new File(CSV_PATH);
        if (!csv.exists()) { System.out.println("  CSV nao encontrado."); return; }
        ChartGenerator.generateAll(CsvReader.read(CSV_PATH), CHARTS_DIR);
    }

    static void ensureOfficialSamplesExist() throws IOException {
        SampleConfig.ensureSampleDirectory();
        List<String> missing = new ArrayList<>();
        for (String[] e : OFFICIAL_FILES) {
            if (!new File(e[0]).exists()) missing.add(e[0]);
        }
        if (!missing.isEmpty()) {
            System.out.println("\n  ERRO: amostras oficiais não encontradas.");
            System.out.println("  Coloque os arquivos do Amostras.zip em: " + SAMPLE_DIR);
            for (String m : missing) System.out.println("   - " + m);
            throw new FileNotFoundException("Amostras oficiais ausentes");
        }
    }

    static void printBanner() {
        System.out.println("\n  ================================================");
        System.out.println("           GameText Analyzer v2.0");
        System.out.println("      Textos literários como mundos de jogo");
        System.out.println("  ================================================\n");
    }

    static void printMenu() {
        System.out.println("\n  [1] Verificar amostras oficiais  [2] Benchmark oficial");
        System.out.println("  [3] Benchmark personalizado      [4] Graficos");
        System.out.println("  [5] Sair");
        System.out.print("  Escolha: ");
    }
}
