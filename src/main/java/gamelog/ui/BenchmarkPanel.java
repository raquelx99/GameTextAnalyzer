package gamelog.ui;

import gamelog.SampleConfig;
import gamelog.benchmark.*;
import gamelog.utils.FileUtils;
import gamelog.counters.WordCounter;
import gamelog.counters.StrategyRegistry;
import gamelog.ui.MainWindow;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Panel for configuring and running benchmarks.
 * Runs the benchmark on a background thread and streams output to a log area.
 */
public class BenchmarkPanel extends JPanel {

    private final MainWindow win;

    // ── Controls ─────────────────────────────────────────────────────────────
    private JComboBox<String> fileCombo;
    private JTextField        eventField;
    private JCheckBox         chkSerial, chkStreamSerial, chkCpu2, chkCpu4, chkCpu8, chkCpuN, chkForkJoin, chkParallelStream, chkVirtual50, chkVirtual100, chkGpu;
    private JCheckBox         chkFullBench;
    private JButton           btnRun;
    private JProgressBar      progress;
    private JTextArea         logArea;
    private JLabel            statusLabel;

    // ── Result table ─────────────────────────────────────────────────────────
    private ResultTablePanel  resultTable;

    public BenchmarkPanel(MainWindow win) {
        this.win = win;
        setBackground(MainWindow.BG_DARK);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        // ── Header ────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.BORDER_COL),
                BorderFactory.createEmptyBorder(18, 28, 18, 28)));
        header.add(MainWindow.sectionTitle("⚡  Benchmark Modular"), BorderLayout.WEST);
        header.add(MainWindow.subLabel("Compara estratégias seriais, CPU paralela, streams, virtual threads e GPU/OpenCL"), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ── Split: config left / output right ────────────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildConfigPanel(), buildOutputPanel());
        split.setDividerLocation(310);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(MainWindow.BG_DARK);
        add(split, BorderLayout.CENTER);
    }

    // ── Config panel (left) ───────────────────────────────────────────────────

    private JPanel buildConfigPanel() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BG_SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, MainWindow.BORDER_COL),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        // Full benchmark toggle
        chkFullBench = styledCheck("Benchmark oficial (Don Quixote, Dracula, Moby Dick)", true);
        chkFullBench.addActionListener(e -> updateFileComboState());
        p.add(chkFullBench);
        p.add(Box.createVerticalStrut(16));

        // File selector
        p.add(fieldLabel("Amostra / mundo narrativo"));
        p.add(Box.createVerticalStrut(4));
        fileCombo = new JComboBox<>();
        styleCombo(fileCombo);
        refreshFileList();
        fileCombo.setEnabled(false);
        p.add(fileCombo);
        p.add(Box.createVerticalStrut(14));

        // Event field
        p.add(fieldLabel("Palavra a buscar"));
        p.add(Box.createVerticalStrut(4));
        eventField = styledTextField("whale");
        eventField.setEnabled(false);
        p.add(eventField);
        p.add(Box.createVerticalStrut(6));

        // Suggested events
        JPanel hints = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        hints.setBackground(MainWindow.BG_PANEL);
        String[] suggestions = {"quijote", "sancho", "caballero", "blood", "night", "vampire",
                                 "whale", "sea", "ship", "ahab"};
        for (String s : suggestions) {
            JButton chip = new JButton(s);
            chip.setFont(new Font("SansSerif", Font.PLAIN, 10));
            chip.setBackground(MainWindow.BG_CARD);
            chip.setForeground(MainWindow.ACCENT);
            chip.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(MainWindow.ACCENT, 1),
                    BorderFactory.createEmptyBorder(2, 6, 2, 6)));
            chip.setFocusPainted(false);
            chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            chip.addActionListener(e -> eventField.setText(s));
            hints.add(chip);
        }
        p.add(hints);
        p.add(Box.createVerticalStrut(16));

        // Methods
        p.add(fieldLabel("Estratégias a comparar"));
        p.add(Box.createVerticalStrut(6));
        chkSerial         = styledCheck("SerialCPU  (loop simples)", true);
        chkStreamSerial   = styledCheck("SerialStream  (stream sequencial)", true);
        chkCpu2           = styledCheck("ParallelCPU  2 threads", true);
        chkCpu4           = styledCheck("ParallelCPU  4 threads", true);
        chkCpu8           = styledCheck("ParallelCPU  8 threads", true);
        chkCpuN           = styledCheck("ParallelCPU  " + BenchmarkRunner.CPU_CORES + " threads  (esta máquina)", true);
        chkForkJoin       = styledCheck("ForkJoinCPU  (divide-and-conquer)", true);
        chkParallelStream = styledCheck("ParallelStream  (ForkJoin comum)", true);
        chkVirtual50      = styledCheck("VirtualThreads  50 chunks", true);
        chkVirtual100     = styledCheck("VirtualThreads  100 chunks", true);
        chkGpu            = styledCheck("ParallelGPU  (OpenCL/fallback)", true);
        for (JCheckBox cb : allStrategyChecks()) {
            cb.setEnabled(true);
            p.add(cb);
            p.add(Box.createVerticalStrut(2));
        }

        JLabel vtHint = new JLabel("Virtual Threads exigem Java 21+. São úteis para comparar overhead em tarefa CPU-bound.");
        vtHint.setFont(new Font("SansSerif", Font.PLAIN, 10));
        vtHint.setForeground(MainWindow.TEXT_DIM);
        vtHint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(Box.createVerticalStrut(6));
        p.add(vtHint);

        p.add(Box.createVerticalGlue());

        // Buttons
        btnRun = MainWindow.primaryButton("▶  Executar Benchmark");
        btnRun.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btnRun.setAlignmentX(LEFT_ALIGNMENT);
        btnRun.addActionListener(e -> runBenchmark());
        p.add(btnRun);
        p.add(Box.createVerticalStrut(8));

        JButton btnRefresh = MainWindow.secondaryButton("🔄  Atualizar arquivos");
        btnRefresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnRefresh.setAlignmentX(LEFT_ALIGNMENT);
        btnRefresh.addActionListener(e -> refreshFileList());
        p.add(btnRefresh);

        return p;
    }

    // ── Output panel (right) ──────────────────────────────────────────────────

    private JPanel buildOutputPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(MainWindow.BG_DARK);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        // Progress + status
        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setBackground(MainWindow.BG_DARK);

        progress = new JProgressBar(0, 100);
        progress.setStringPainted(false);
        progress.setBackground(MainWindow.BG_PANEL);
        progress.setForeground(MainWindow.ACCENT3);
        progress.setPreferredSize(new Dimension(0, 8));
        progress.setBorderPainted(false);

        statusLabel = new JLabel("Pronto.");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(MainWindow.TEXT_DIM);

        topBar.add(statusLabel, BorderLayout.WEST);
        topBar.add(progress,    BorderLayout.SOUTH);
        p.add(topBar, BorderLayout.NORTH);

        // Tab: log / results
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(MainWindow.BG_DARK);
        tabs.setForeground(MainWindow.TEXT_MAIN);
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 12));

        logArea = MainWindow.logArea();
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.getViewport().setBackground(new Color(0x0A0A14));
        tabs.addTab("📋  Console", logScroll);

        resultTable = new ResultTablePanel();
        tabs.addTab("📊  Resultados", resultTable);

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    // ── Benchmark execution ───────────────────────────────────────────────────

    private void runBenchmark() {
        boolean fullBench = chkFullBench.isSelected();
        String  filePath  = fullBench ? null : (String) fileCombo.getSelectedItem();
        String  event     = FileUtils.normalizeQuery(eventField.getText().trim());

        if (!fullBench && (filePath == null || filePath.isEmpty())) {
            JOptionPane.showMessageDialog(this, "Selecione uma amostra.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!fullBench && event.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite uma palavra para buscar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnRun.setEnabled(false);
        logArea.setText("");
        progress.setIndeterminate(true);
        statusLabel.setText("Executando benchmark...");

        // Redirect System.out to the log area
        PrintStream logStream = new PrintStream(new OutputStream() {
            private final StringBuilder sb = new StringBuilder();
            public void write(int b) {
                sb.append((char) b);
                if (b == '\n') flush();
            }
            public void flush() {
                String txt = sb.toString();
                sb.setLength(0);
                SwingUtilities.invokeLater(() -> {
                    logArea.append(txt);
                    logArea.setCaretPosition(logArea.getDocument().getLength());
                });
            }
        });

        PrintStream originalOut = System.out;
        System.setOut(logStream);

        SwingWorker<List<BenchmarkResult>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<BenchmarkResult> doInBackground() throws Exception {
                ensureOfficialSamplesExist();
                BenchmarkRunner runner = new BenchmarkRunner();
                List<BenchmarkResult> allResults = new java.util.ArrayList<>();
                List<WordCounter> selectedStrategies = selectedStrategies();

                if (selectedStrategies.isEmpty()) {
                    throw new IllegalStateException("Selecione pelo menos uma estratégia de benchmark.");
                }

                if (fullBench) {
                    new File(MainWindow.CSV_PATH).delete();
                    for (String[] entry : SampleConfig.officialBenchmarkEntries()) {
                        List<BenchmarkResult> r = runner.run(entry[0], entry[1], selectedStrategies);
                        allResults.addAll(r);
                        CsvWriter.write(MainWindow.CSV_PATH, r, true);
                    }
                } else {
                    List<BenchmarkResult> r = runner.run(filePath, event, selectedStrategies);
                    allResults.addAll(r);
                    CsvWriter.write(MainWindow.CSV_PATH, r, true);
                }

                System.out.println("\n  ✓ Benchmark concluído! Gerando gráficos...");
                ChartGenerator.generateAll(allResults, MainWindow.CHARTS_DIR);
                return allResults;
            }

            @Override
            protected void done() {
                System.setOut(originalOut);
                progress.setIndeterminate(false);
                progress.setValue(100);
                btnRun.setEnabled(true);
                try {
                    List<BenchmarkResult> results = get();
                    resultTable.setResults(results);
                    statusLabel.setText("✓ Benchmark concluído — " + results.size() + " execuções registradas.");
                    statusLabel.setForeground(MainWindow.ACCENT3);
                } catch (Exception ex) {
                    statusLabel.setText("Erro: " + ex.getMessage());
                    statusLabel.setForeground(Color.RED);
                }
            }
        };
        worker.execute();
    }

    private void ensureOfficialSamplesExist() throws IOException {
        SampleConfig.ensureSampleDirectory();
        java.util.List<String> missing = new java.util.ArrayList<>();
        for (SampleConfig sample : SampleConfig.OFFICIAL_SAMPLES) {
            if (!new File(sample.filePath).exists()) missing.add(sample.filePath);
        }
        if (!missing.isEmpty()) {
            System.out.println("  Amostras oficiais não encontradas.");
            System.out.println("  Coloque os arquivos do Amostras.zip em: " + SampleConfig.SAMPLE_DIR);
            for (String m : missing) System.out.println("   - " + m);
            throw new FileNotFoundException("Amostras oficiais ausentes");
        }
    }

    private void updateFileComboState() {
        boolean full = chkFullBench.isSelected();
        fileCombo.setEnabled(!full);
        eventField.setEnabled(!full);
        for (JCheckBox cb : allStrategyChecks()) {
            cb.setEnabled(true);
        }
    }

    private void refreshFileList() {
        fileCombo.removeAllItems();
        File dir = new File(SampleConfig.SAMPLE_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) fileCombo.addItem(f.getPath());
            }
        }
        if (fileCombo.getItemCount() == 0) fileCombo.addItem("(nenhuma amostra encontrada)");
    }

    private JCheckBox[] allStrategyChecks() {
        return new JCheckBox[]{
                chkSerial, chkStreamSerial, chkCpu2, chkCpu4, chkCpu8, chkCpuN,
                chkForkJoin, chkParallelStream, chkVirtual50, chkVirtual100, chkGpu
        };
    }

    private List<WordCounter> selectedStrategies() {
        List<WordCounter> strategies = new java.util.ArrayList<>();
        if (chkSerial != null && chkSerial.isSelected()) strategies.add(new gamelog.counters.SerialCPUCounter());
        if (chkStreamSerial != null && chkStreamSerial.isSelected()) strategies.add(new gamelog.counters.SerialStreamCounter());
        if (chkCpu2 != null && chkCpu2.isSelected()) strategies.add(new gamelog.counters.ParallelCPUCounter(2));
        if (chkCpu4 != null && chkCpu4.isSelected()) strategies.add(new gamelog.counters.ParallelCPUCounter(4));
        if (chkCpu8 != null && chkCpu8.isSelected()) strategies.add(new gamelog.counters.ParallelCPUCounter(8));
        if (chkCpuN != null && chkCpuN.isSelected()) strategies.add(new gamelog.counters.ParallelCPUCounter(BenchmarkRunner.CPU_CORES));
        if (chkForkJoin != null && chkForkJoin.isSelected()) strategies.add(new gamelog.counters.ForkJoinCPUCounter());
        if (chkParallelStream != null && chkParallelStream.isSelected()) strategies.add(new gamelog.counters.ParallelStreamCounter());
        if (chkVirtual50 != null && chkVirtual50.isSelected()) strategies.add(new gamelog.counters.VirtualThreadCounter(50));
        if (chkVirtual100 != null && chkVirtual100.isSelected()) strategies.add(new gamelog.counters.VirtualThreadCounter(100));
        if (chkGpu != null && chkGpu.isSelected()) strategies.add(new gamelog.counters.ParallelGPUCounter());
        return strategies;
    }

    // ── Style helpers ─────────────────────────────────────────────────────────

    private JLabel fieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        lbl.setForeground(MainWindow.TEXT_DIM);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    private JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField(placeholder);
        tf.setBackground(MainWindow.BG_CARD);
        tf.setForeground(MainWindow.TEXT_MAIN);
        tf.setCaretColor(MainWindow.TEXT_MAIN);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MainWindow.BORDER_COL),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        tf.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        tf.setAlignmentX(LEFT_ALIGNMENT);
        return tf;
    }

    private JCheckBox styledCheck(String text, boolean selected) {
        JCheckBox cb = new JCheckBox(text, selected);
        cb.setBackground(MainWindow.BG_PANEL);
        cb.setForeground(MainWindow.TEXT_MAIN);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setAlignmentX(LEFT_ALIGNMENT);
        cb.setFocusPainted(false);
        return cb;
    }

    private void styleCombo(JComboBox<String> cb) {
        cb.setBackground(MainWindow.BG_CARD);
        cb.setForeground(MainWindow.TEXT_MAIN);
        cb.setFont(new Font("SansSerif", Font.PLAIN, 12));
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        cb.setAlignmentX(LEFT_ALIGNMENT);
    }
}
