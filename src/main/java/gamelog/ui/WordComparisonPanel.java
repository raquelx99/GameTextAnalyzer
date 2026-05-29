package gamelog.ui;

import gamelog.SampleConfig;
import gamelog.utils.FileUtils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Compares selected words across the official literary samples.
 *
 * This panel keeps the gamified angle without changing the required datasets:
 * each word is treated as a narrative signal and each book as a game world.
 */
public class WordComparisonPanel extends JPanel {

    private static final File CATEGORIES_DIR  = new File(System.getProperty("user.home"), ".gameloganalyzer");
    private static final File CATEGORIES_FILE = new File(CATEGORIES_DIR, "word_categories.properties");

    private JTextArea wordsArea;
    private JButton analyzeBtn;
    private JButton exportBtn;
    private JLabel statusLabel;
    private DefaultTableModel tableModel;
    private JTable table;
    private ComparisonChart chart;
    private JComboBox<String> wordCombo;
    private boolean syncingCombo = false;
    private List<ComparisonRow> rows = new ArrayList<>();

    private JTabbedPane controlTabs;
    private JPanel categoryListPanel;
    private final LinkedHashMap<String, String> categories = new LinkedHashMap<>();

    public WordComparisonPanel(MainWindow win) {
        setBackground(Theme.BG_BASE);
        setLayout(new BorderLayout());
        build();
        loadCategories();
    }

    private void build() {
        add(header(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controls(), results());
        split.setDividerLocation(330);
        split.setDividerSize(3);
        split.setBorder(null);
        split.setBackground(Theme.BG_BASE);
        add(split, BorderLayout.CENTER);
    }

    private JPanel header() {
        JPanel h = new JPanel(new BorderLayout(12, 0));
        h.setBackground(Theme.BG_SURFACE);
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setOpaque(false);
        left.add(Theme.h2("◇  Comparador Narrativo"));
        JLabel subtitle = Theme.muted("Compare sinais narrativos entre Don Quixote, Dracula e Moby Dick");
        left.add(subtitle);
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JPanel controls() {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG_SURFACE);
        wrapper.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.DIVIDER));

        controlTabs = new JTabbedPane();
        controlTabs.setBackground(Theme.BG_SURFACE);
        controlTabs.setForeground(Theme.TEXT_SEC);
        controlTabs.setFont(Theme.FONT_BODY);
        controlTabs.addTab("Análise", analysisPane());
        controlTabs.addTab("Categorias", categoriesPane());

        wrapper.add(controlTabs, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel analysisPane() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BG_SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        p.add(label("Palavras para comparar"));
        p.add(Box.createVerticalStrut(8));

        wordsArea = Theme.terminal();
        wordsArea.setEditable(true);
        wordsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        wordsArea.setText("quijote, sancho, caballero\n" +
                          "dracula, vampire, blood, night\n" +
                          "whale, sea, ship, ahab");
        JScrollPane sp = new JScrollPane(wordsArea);
        sp.setPreferredSize(new Dimension(0, 160));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));
        sp.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        p.add(sp);
        p.add(Box.createVerticalStrut(12));

        JLabel hint = new JLabel("<html><body style='width:260px;color:#8E8EAE'>" +
                "Digite palavras separadas por vírgula, espaço ou quebra de linha. " +
                "A busca usa a mesma normalização do benchmark: minúsculas, sem acentos e sem pontuação." +
                "</body></html>");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_SEC);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hint);
        p.add(Box.createVerticalStrut(18));

        analyzeBtn = Theme.primaryBtn("Analisar palavras");
        analyzeBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        analyzeBtn.setAlignmentX(LEFT_ALIGNMENT);
        analyzeBtn.addActionListener(e -> analyze());
        p.add(analyzeBtn);
        p.add(Box.createVerticalStrut(8));

        JButton suggestedBtn = Theme.ghostBtn("Usar sugestões oficiais");
        suggestedBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        suggestedBtn.setAlignmentX(LEFT_ALIGNMENT);
        suggestedBtn.addActionListener(e -> fillSuggestedWords());
        p.add(suggestedBtn);
        p.add(Box.createVerticalStrut(8));

        exportBtn = Theme.ghostBtn("Exportar comparação CSV");
        exportBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        exportBtn.setAlignmentX(LEFT_ALIGNMENT);
        exportBtn.setEnabled(false);
        exportBtn.addActionListener(e -> exportCsv());
        p.add(exportBtn);

        p.add(Box.createVerticalGlue());

        JPanel note = Theme.card(new BorderLayout(0, 6));
        note.setBorder(BorderFactory.createCompoundBorder(note.getBorder(),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        note.setAlignmentX(LEFT_ALIGNMENT);
        note.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        note.add(Theme.h3("Como funciona"), BorderLayout.NORTH);
        note.add(new JLabel("<html><body style='width:250px;color:#A8A8C8;line-height:1.5'>" +
                "A densidade normaliza as contagens por <b style='color:#C8C8E8'>10.000 palavras</b>, " +
                "permitindo comparar obras de tamanhos muito diferentes sem distorção.<br><br>" +
                "Uma palavra com densidade <b style='color:#C8C8E8'>alta</b> em um livro indica que " +
                "ela é um sinal narrativo relevante naquele texto: personagem recorrente, tema central ou elemento de cena." +
                "</body></html>"), BorderLayout.CENTER);
        p.add(note);
        return p;
    }

    private JPanel categoriesPane() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BG_SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        p.add(label("Nova categoria"));
        p.add(Box.createVerticalStrut(8));

        JTextField nameField = new JTextField();
        nameField.setFont(Theme.FONT_BODY);
        nameField.setBackground(Theme.BG_CARD);
        nameField.setForeground(Theme.TEXT_PRI);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.DIVIDER_LT),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        nameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        nameField.setAlignmentX(LEFT_ALIGNMENT);
        p.add(nameField);
        p.add(Box.createVerticalStrut(6));

        JTextArea catWordsArea = Theme.terminal();
        catWordsArea.setEditable(true);
        catWordsArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane catSp = new JScrollPane(catWordsArea);
        catSp.setPreferredSize(new Dimension(0, 80));
        catSp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        catSp.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        catSp.setAlignmentX(LEFT_ALIGNMENT);
        p.add(catSp);
        p.add(Box.createVerticalStrut(6));

        JButton saveBtn = Theme.primaryBtn("Salvar categoria");
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        saveBtn.setAlignmentX(LEFT_ALIGNMENT);
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String words = catWordsArea.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite um nome para a categoria.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (words.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite ao menos uma palavra.", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            categories.put(name, words);
            saveCategories();
            renderCategoryList();
            nameField.setText("");
            catWordsArea.setText("");
        });
        p.add(saveBtn);
        p.add(Box.createVerticalStrut(4));

        JButton importBtn = Theme.ghostBtn("Importar palavras da análise");
        importBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        importBtn.setAlignmentX(LEFT_ALIGNMENT);
        importBtn.addActionListener(e -> catWordsArea.setText(wordsArea.getText()));
        p.add(importBtn);

        p.add(Box.createVerticalStrut(18));
        p.add(label("Categorias salvas"));
        p.add(Box.createVerticalStrut(8));

        categoryListPanel = new JPanel();
        categoryListPanel.setLayout(new BoxLayout(categoryListPanel, BoxLayout.Y_AXIS));
        categoryListPanel.setBackground(Theme.BG_BASE);

        JScrollPane listScroll = new JScrollPane(categoryListPanel);
        listScroll.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        listScroll.getViewport().setBackground(Theme.BG_BASE);
        listScroll.setAlignmentX(LEFT_ALIGNMENT);
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.add(listScroll);

        return p;
    }

    private JPanel results() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        statusLabel = Theme.muted("Pronto para analisar.");
        p.add(statusLabel, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_BASE);
        tabs.setForeground(Theme.TEXT_SEC);
        tabs.setFont(Theme.FONT_BODY);

        tableModel = new DefaultTableModel(new String[]{
                "Palavra", "Don Quixote", "Dracula", "Moby Dick", "Mais presente", "Densidade máx./10k"
        }, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);
        table.getSelectionModel().addListSelectionListener(e -> updateChartFromSelection());

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        tableScroll.getViewport().setBackground(Theme.BG_CARD);
        tabs.addTab("Tabela comparativa", tableScroll);

        chart = new ComparisonChart();

        wordCombo = new JComboBox<>();
        wordCombo.setFont(Theme.FONT_BODY);
        wordCombo.setBackground(Theme.BG_CARD);
        wordCombo.setForeground(Theme.TEXT_PRI);
        wordCombo.setEnabled(false);
        wordCombo.addActionListener(e -> {
            if (syncingCombo) return;
            String selected = (String) wordCombo.getSelectedItem();
            if (selected == null) return;
            rows.stream().filter(r -> r.word.equals(selected)).findFirst().ifPresent(r -> {
                chart.setRow(r);
                syncTableToWord(r.word);
            });
        });

        JLabel comboLabel = new JLabel("Palavra:");
        comboLabel.setFont(Theme.FONT_BODY);
        comboLabel.setForeground(Theme.TEXT_SEC);

        JPanel comboBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        comboBar.setBackground(Theme.BG_SURFACE);
        comboBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER_LT));
        comboBar.add(comboLabel);
        comboBar.add(wordCombo);

        JPanel chartTab = new JPanel(new BorderLayout());
        chartTab.setBackground(Theme.BG_CARD);
        chartTab.add(comboBar, BorderLayout.NORTH);
        chartTab.add(chart, BorderLayout.CENTER);

        tabs.addTab("Gráfico por palavra", chartTab);

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    private JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(Theme.TEXT_SEC);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void fillSuggestedWords() {
        StringBuilder sb = new StringBuilder();
        for (SampleConfig sample : SampleConfig.OFFICIAL_SAMPLES) {
            sb.append(String.join(", ", sample.suggestedWords)).append('\n');
        }
        wordsArea.setText(sb.toString());
    }

    private List<String> parseWords() {
        return Arrays.stream(wordsArea.getText().split("[,;\\s]+"))
                .map(FileUtils::normalizeQuery)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void analyze() {
        List<String> words = parseWords();
        if (words.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Digite ao menos uma palavra.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        analyzeBtn.setEnabled(false);
        exportBtn.setEnabled(false);
        statusLabel.setText("Analisando amostras...");

        SwingWorker<List<ComparisonRow>, Void> worker = new SwingWorker<>() {
            @Override protected List<ComparisonRow> doInBackground() throws Exception {
                Map<SampleConfig, String[]> tokens = new LinkedHashMap<>();
                for (SampleConfig sample : SampleConfig.OFFICIAL_SAMPLES) {
                    File f = new File(sample.filePath);
                    if (!f.exists()) throw new FileNotFoundException("Amostra ausente: " + sample.filePath);
                    tokens.put(sample, FileUtils.readTokens(sample.filePath));
                }

                List<ComparisonRow> result = new ArrayList<>();
                for (String word : words) {
                    long[] counts = new long[SampleConfig.OFFICIAL_SAMPLES.size()];
                    double[] density = new double[counts.length];
                    for (int i = 0; i < SampleConfig.OFFICIAL_SAMPLES.size(); i++) {
                        SampleConfig sample = SampleConfig.OFFICIAL_SAMPLES.get(i);
                        String[] arr = tokens.get(sample);
                        long c = Arrays.stream(arr).filter(word::equals).count();
                        counts[i] = c;
                        density[i] = arr.length == 0 ? 0.0 : (c * 10_000.0 / arr.length);
                    }
                    result.add(new ComparisonRow(word, counts, density));
                }
                return result;
            }

            @Override protected void done() {
                analyzeBtn.setEnabled(true);
                try {
                    rows = get();
                    renderRows();
                    exportBtn.setEnabled(!rows.isEmpty());
                    statusLabel.setText(rows.size() + " palavras comparadas entre as amostras oficiais.");
                    statusLabel.setForeground(Theme.GREEN);
                } catch (Exception ex) {
                    statusLabel.setText("Erro: " + ex.getMessage());
                    statusLabel.setForeground(Theme.RED);
                    JOptionPane.showMessageDialog(WordComparisonPanel.this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    private void renderRows() {
        tableModel.setRowCount(0);
        syncingCombo = true;
        wordCombo.removeAllItems();
        for (ComparisonRow row : rows) {
            tableModel.addRow(new Object[]{
                    row.word,
                    String.format("%,d", row.counts[0]),
                    String.format("%,d", row.counts[1]),
                    String.format("%,d", row.counts[2]),
                    row.bestWorld(),
                    String.format(Locale.US, "%.2f", row.maxDensity())
            });
            wordCombo.addItem(row.word);
        }
        syncingCombo = false;
        wordCombo.setEnabled(!rows.isEmpty());
        if (!rows.isEmpty()) {
            table.setRowSelectionInterval(0, 0);
            chart.setRow(rows.get(0));
            wordCombo.setSelectedIndex(0);
        }
    }

    private void updateChartFromSelection() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0 || rows.isEmpty()) return;
        int modelRow = table.convertRowIndexToModel(viewRow);
        if (modelRow >= 0 && modelRow < rows.size()) {
            ComparisonRow r = rows.get(modelRow);
            chart.setRow(r);
            syncingCombo = true;
            wordCombo.setSelectedItem(r.word);
            syncingCombo = false;
        }
    }

    private void syncTableToWord(String word) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).word.equals(word)) {
                int viewRow = table.convertRowIndexToView(i);
                if (viewRow >= 0) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
                }
                return;
            }
        }
    }

    private void exportCsv() {
        if (rows.isEmpty()) return;
        File outDir = new File(MainWindow.RESULTS_DIR);
        outDir.mkdirs();
        File out = new File(outDir, "comparacao_palavras.csv");
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(out), java.nio.charset.StandardCharsets.UTF_8))) {
            pw.println("word,don_quixote_count,dracula_count,moby_dick_count,don_quixote_per_10k,dracula_per_10k,moby_dick_per_10k,best_world");
            for (ComparisonRow r : rows) {
                pw.printf(Locale.US, "%s,%d,%d,%d,%.4f,%.4f,%.4f,%s%n",
                        r.word, r.counts[0], r.counts[1], r.counts[2],
                        r.density[0], r.density[1], r.density[2], r.bestWorld());
            }
            JOptionPane.showMessageDialog(this, "CSV exportado em:\n" + out.getPath(), "Exportado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro ao exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadCategories() {
        categories.clear();
        if (!CATEGORIES_FILE.exists()) { renderCategoryList(); return; }
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CATEGORIES_FILE)) {
            props.load(in);
            for (String key : props.stringPropertyNames()) categories.put(key, props.getProperty(key));
        } catch (IOException ignored) {}
        renderCategoryList();
    }

    private void saveCategories() {
        try {
            CATEGORIES_DIR.mkdirs();
            Properties props = new Properties();
            categories.forEach(props::setProperty);
            try (OutputStream out = new FileOutputStream(CATEGORIES_FILE)) {
                props.store(out, null);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar categorias: " + ex.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderCategoryList() {
        if (categoryListPanel == null) return;
        categoryListPanel.removeAll();
        if (categories.isEmpty()) {
            JLabel empty = new JLabel("  Nenhuma categoria salva.");
            empty.setFont(Theme.FONT_SMALL);
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setBorder(BorderFactory.createEmptyBorder(12, 8, 12, 8));
            categoryListPanel.add(empty);
        } else {
            boolean first = true;
            for (Map.Entry<String, String> e : categories.entrySet()) {
                if (!first) categoryListPanel.add(new JSeparator());
                categoryListPanel.add(categoryRow(e.getKey(), e.getValue()));
                first = false;
            }
        }
        categoryListPanel.revalidate();
        categoryListPanel.repaint();
    }

    private JPanel categoryRow(String name, String words) {
        JPanel row = new JPanel(new BorderLayout(6, 2));
        row.setBackground(Theme.BG_CARD);
        row.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        nameLabel.setForeground(Theme.ACCENT);

        String preview = words.replaceAll("\\s+", " ").trim();
        if (preview.length() > 38) preview = preview.substring(0, 35) + "…";
        JLabel wordsLabel = new JLabel(preview);
        wordsLabel.setFont(Theme.FONT_SMALL);
        wordsLabel.setForeground(Theme.TEXT_MUTED);

        JPanel textCol = new JPanel(new BorderLayout(0, 2));
        textCol.setOpaque(false);
        textCol.add(nameLabel, BorderLayout.NORTH);
        textCol.add(wordsLabel, BorderLayout.CENTER);
        row.add(textCol, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);

        JButton useBtn = Theme.ghostBtn("Usar");
        useBtn.setFont(Theme.FONT_SMALL);
        useBtn.addActionListener(e -> {
            wordsArea.setText(words);
            controlTabs.setSelectedIndex(0);
        });
        btns.add(useBtn);

        JButton delBtn = Theme.ghostBtn("×");
        delBtn.setForeground(Theme.RED);
        delBtn.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Excluir a categoria \"" + name + "\"?", "Confirmar", JOptionPane.YES_NO_OPTION);
            if (ok == JOptionPane.YES_OPTION) {
                categories.remove(name);
                saveCategories();
                renderCategoryList();
            }
        });
        btns.add(delBtn);

        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private void styleTable(JTable t) {
        t.setBackground(Theme.BG_CARD);
        t.setForeground(Theme.TEXT_PRI);
        t.setFont(Theme.FONT_MONO_SM);
        t.setRowHeight(30);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setGridColor(Theme.DIVIDER);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.setSelectionBackground(Theme.BG_ACTIVE);
        t.setSelectionForeground(Theme.TEXT_PRI);
        t.getTableHeader().setBackground(Theme.BG_SURFACE);
        t.getTableHeader().setForeground(Theme.TEXT_SEC);
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 11));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int col) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, col);
                if (!selected) setBackground(row % 2 == 0 ? Theme.BG_CARD : new Color(0x171725));
                setForeground(col == 0 ? Theme.ACCENT : Theme.TEXT_PRI);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
    }

    static class ComparisonRow {
        final String word;
        final long[] counts;
        final double[] density;

        ComparisonRow(String word, long[] counts, double[] density) {
            this.word = word;
            this.counts = counts;
            this.density = density;
        }

        String bestWorld() {
            int best = 0;
            for (int i = 1; i < density.length; i++) if (density[i] > density[best]) best = i;
            return SampleConfig.OFFICIAL_SAMPLES.get(best).gameWorld;
        }

        double maxDensity() {
            double max = 0;
            for (double d : density) max = Math.max(max, d);
            return max;
        }
    }

    static class ComparisonChart extends JPanel {
        private ComparisonRow row;

        ComparisonChart() {
            setBackground(Theme.BG_CARD);
            setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        }

        void setRow(ComparisonRow row) {
            this.row = row;
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (row == null) {
                g2.setColor(Theme.TEXT_MUTED);
                g2.setFont(Theme.FONT_BODY);
                String msg = "Selecione uma palavra na tabela para visualizar o comparativo";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(msg, (getWidth() - fm.stringWidth(msg)) / 2, getHeight() / 2);
                g2.dispose();
                return;
            }

            int left = 190, right = 50, top = 70, barH = 42, gap = 34;
            int width = Math.max(80, getWidth() - left - right);
            double max = Math.max(0.01, row.maxDensity());

            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            g2.setColor(Theme.TEXT_PRI);
            g2.drawString("Densidade da palavra: " + row.word, 28, 36);
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_SEC);
            g2.drawString("Valores normalizados por 10.000 palavras para comparar textos de tamanhos diferentes.", 28, 56);

            Color[] colors = {Theme.ACCENT, Theme.RED, Theme.GREEN};
            for (int i = 0; i < SampleConfig.OFFICIAL_SAMPLES.size(); i++) {
                SampleConfig sample = SampleConfig.OFFICIAL_SAMPLES.get(i);
                int y = top + i * (barH + gap);
                int w = (int) Math.round((row.density[i] / max) * width);
                Color c = colors[i % colors.length];

                g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                g2.setColor(Theme.TEXT_PRI);
                g2.drawString(sample.title, 28, y + 17);
                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(sample.gameWorld, 28, y + 34);

                g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 24));
                g2.fillRoundRect(left, y, width, barH, 10, 10);
                g2.setColor(c);
                g2.fillRoundRect(left, y, Math.max(3, w), barH, 10, 10);

                String label = String.format(Locale.US, "%,d ocorr.  ·  %.2f / 10k", row.counts[i], row.density[i]);
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
                g2.setColor(Theme.TEXT_PRI);
                g2.drawString(label, left + Math.min(w + 10, width - 210), y + 27);
            }
            g2.dispose();
        }
    }
}
