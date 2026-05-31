package gamelog.ui;

import gamelog.SampleConfig;
import gamelog.utils.FileUtils;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;

/**

* Painel de Análise de Texto, permite ao usuário navegar, pesquisar e obter estatísticas sobre uma amostra oficial
* sem executar um benchmark completo.
*
* Recursos:
* Seletor de arquivos (amostras .txt oficiais em data/samples/)
* Tabela de frequência de palavras (classificada)
* Mini gráfico de barras embutido de distribuição
* Pesquisa/filtro: contagem de ocorrências de qualquer palavra
* Visualizador de tokens brutos paginado (tamanho da página configurável)
* Estatísticas básicas: total de palavras, palavras únicas, mais frequentes, mais raras
*/
public class LogAnalysisPanel extends JPanel {

    private final MainWindow win;

    // Estado
    private String[] currentLines = new String[0];
    private String   currentFile  = "";

    // Controles
    private JComboBox<String> fileCombo;
    private JTextField        searchField;
    private JLabel            searchResult;
    private JLabel            statusLbl;
    private JProgressBar      loadBar;

    // Rótulos de estatísticas
    private JLabel lblTotal, lblUnique, lblMostFreq, lblRarest, lblAvgLine;

    // Tabela de frequência
    private DefaultTableModel freqModel;
    private JTable            freqTable;

    // Visualizador de texto tokenizado
    private JTextArea         rawArea;
    private JLabel            pageLabel;
    private int               currentPage = 0;
    private int               pageSize    = 500;
    private JComboBox<Integer> pageSizeCombo;

    // Mini gráfico (distribuição)
    private DistributionChart distChart;

    public LogAnalysisPanel(MainWindow win) {
        this.win = win;
        setBackground(Theme.BG_BASE);
        setLayout(new BorderLayout());
        build();
    }

    // ── Construção ─────────────────────────────────────────────────────────────────

    private void build() {
        add(buildHeader(), BorderLayout.NORTH);

        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        main.setDividerLocation(370);
        main.setDividerSize(3);
        main.setBorder(null);
        main.setBackground(Theme.BG_BASE);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout(16, 0));
        h.setBackground(Theme.BG_SURFACE);
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(14, 28, 14, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(Theme.BG_SURFACE);
        left.add(Theme.h2("◎  Análise de Textos"));
        statusLbl = Theme.muted("Selecione uma amostra para começar");
        left.add(statusLbl);
        h.add(left, BorderLayout.WEST);

        loadBar = Theme.progressBar();
        loadBar.setPreferredSize(new Dimension(0, 3));
        h.add(loadBar, BorderLayout.SOUTH);
        return h;
    }

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel();
        p.setBackground(Theme.BG_SURFACE);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0,0,0,1,Theme.DIVIDER),
                BorderFactory.createEmptyBorder(20,20,20,20)));

        // ── Seletor de arquivo ─────────────────────────────────────────────────
        p.add(sectionLbl("Amostra literária"));
        p.add(Box.createVerticalStrut(6));
        fileCombo = Theme.comboBox();
        fileCombo.setAlignmentX(LEFT_ALIGNMENT);
        refreshFileList();
        p.add(fileCombo);
        p.add(Box.createVerticalStrut(8));

        JButton btnLoad = Theme.primaryBtn("⊙  Carregar e Analisar");
        btnLoad.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btnLoad.setAlignmentX(LEFT_ALIGNMENT);
        btnLoad.addActionListener(e -> loadFile());
        p.add(btnLoad);
        p.add(Box.createVerticalStrut(20));
        p.add(Theme.divider());
        p.add(Box.createVerticalStrut(16));

        // ── Cards de estatísticas ───────────────────────────────────────────────────
        p.add(sectionLbl("Estatísticas"));
        p.add(Box.createVerticalStrut(10));

        lblTotal     = statRow("Total de palavras",  "—");
        lblUnique    = statRow("Palavras únicas",       "—");
        lblMostFreq  = statRow("Palavra mais comum",  "—");
        lblRarest    = statRow("Palavra mais rara",   "—");
        lblAvgLine   = statRow("Comprimento médio",  "—");

        for (JLabel[] pair : new JLabel[][]{{lblTotal,null},{lblUnique,null},
                {lblMostFreq,null},{lblRarest,null},{lblAvgLine,null}}) {
            // já adicionados via helper statRow abaixo
        }
        p.add(buildStatCard());
        p.add(Box.createVerticalStrut(20));
        p.add(Theme.divider());
        p.add(Box.createVerticalStrut(16));

        // ── Busca ────────────────────────────────────────────────────────
        p.add(sectionLbl("Buscar palavra"));
        p.add(Box.createVerticalStrut(6));
        searchField = Theme.inputField("Ex: whale, blood, quijote");
        searchField.setAlignmentX(LEFT_ALIGNMENT);
        p.add(searchField);
        p.add(Box.createVerticalStrut(6));

        JButton btnSearch = Theme.ghostBtn("Contar ocorrências");
        btnSearch.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnSearch.setAlignmentX(LEFT_ALIGNMENT);
        btnSearch.addActionListener(e -> searchEvent());
        p.add(btnSearch);
        p.add(Box.createVerticalStrut(8));

        searchResult = new JLabel(" ");
        searchResult.setFont(Theme.FONT_BODY);
        searchResult.setForeground(Theme.GREEN);
        searchResult.setAlignmentX(LEFT_ALIGNMENT);
        p.add(searchResult);

        p.add(Box.createVerticalGlue());

        JButton btnRefresh = Theme.ghostBtn("↺  Atualizar lista");
        btnRefresh.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        btnRefresh.setAlignmentX(LEFT_ALIGNMENT);
        btnRefresh.addActionListener(e -> refreshFileList());
        p.add(btnRefresh);

        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_BASE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_BASE);
        tabs.setForeground(Theme.TEXT_SEC);
        tabs.setFont(Theme.FONT_BODY);

        tabs.addTab("⬟  Distribuição",   buildDistributionTab());
        tabs.addTab("≡  Frequências",    buildFreqTab());
        tabs.addTab("⊞  Texto tokenizado", buildRawTab());

        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    // ── Abas ──────────────────────────────────────────────────────────────────

    private JPanel buildDistributionTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        distChart = new DistributionChart();
        distChart.setPreferredSize(new Dimension(0, 340));
        p.add(distChart, BorderLayout.CENTER);

        JLabel hint = Theme.muted("  Carregue uma amostra para visualizar a distribuição de palavras.");
        hint.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
        p.add(hint, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildFreqTab() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        String[] cols = {"#", "Palavra", "Ocorrências", "% do total", "Barra"};
        freqModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        freqTable = new JTable(freqModel);
        freqTable.setBackground(Theme.BG_CARD);
        freqTable.setForeground(Theme.TEXT_PRI);
        freqTable.setFont(Theme.FONT_MONO_SM);
        freqTable.setRowHeight(28);
        freqTable.setGridColor(Theme.DIVIDER);
        freqTable.setShowHorizontalLines(true);
        freqTable.setShowVerticalLines(false);
        freqTable.setFillsViewportHeight(true);
        freqTable.setSelectionBackground(Theme.BG_ACTIVE);
        freqTable.setSelectionForeground(Theme.TEXT_PRI);

        // Renderer personalizado: colore o nome da palavra, desenha barra na última coluna
        freqTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setBackground(sel ? Theme.BG_ACTIVE :
                              row % 2 == 0 ? Theme.BG_CARD : new Color(0x181826));
                setForeground(Theme.TEXT_PRI);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                if (col == 1 && v != null) {
                    setForeground(Theme.methodColor(null));
                    setForeground(Theme.ACCENT);
                    setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                }
                if (col == 4) {
                    // Desenha mini barra inline em vez de texto
                    setText("");
                    return this;
                }
                return this;
            }
        });

        // Renderer da coluna de barra
        freqTable.getColumnModel().getColumn(4).setCellRenderer(
            new TableCellRenderer() {
                @Override public Component getTableCellRendererComponent(
                        JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                    double pct = 0;
                    try { pct = Double.parseDouble(
                            t.getValueAt(row, 3).toString().replace("%","").trim()); }
                    catch (Exception ignored) {}
                    final double barPct = pct;
                    return new JPanel() {
                        { setBackground(sel ? Theme.BG_ACTIVE :
                                        row % 2 == 0 ? Theme.BG_CARD : new Color(0x181826)); }
                        @Override protected void paintComponent(Graphics g) {
                            super.paintComponent(g);
                            int w = (int)(getWidth() * barPct / 100.0);
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(new Color(Theme.ACCENT.getRed(),Theme.ACCENT.getGreen(),
                                    Theme.ACCENT.getBlue(), 40));
                            g2.fillRoundRect(4, 6, getWidth()-8, getHeight()-12, 4, 4);
                            g2.setColor(Theme.ACCENT);
                            if (w > 0)
                                g2.fillRoundRect(4, 6, Math.max(w - 8, 4), getHeight()-12, 4, 4);
                            g2.dispose();
                        }
                    };
                }
            });

        JTableHeader hdr = freqTable.getTableHeader();
        hdr.setBackground(Theme.BG_SURFACE);
        hdr.setForeground(Theme.TEXT_MUTED);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 11));

        int[] widths = {36, 180, 100, 80, 200};
        for (int i = 0; i < widths.length; i++)
            freqTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JScrollPane sc = new JScrollPane(freqTable);
        sc.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        sc.getViewport().setBackground(Theme.BG_CARD);
        p.add(sc, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildRawTab() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.BG_BASE);
        p.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // Barra de controles
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setBackground(Theme.BG_BASE);

        JButton prev = Theme.ghostBtn("◀  Anterior");
        JButton next = Theme.ghostBtn("Próxima  ▶");
        pageLabel = new JLabel("—");
        pageLabel.setForeground(Theme.TEXT_SEC);
        pageLabel.setFont(Theme.FONT_BODY);

        pageSizeCombo = new JComboBox<>(new Integer[]{100, 250, 500, 1000, 2000});
        pageSizeCombo.setSelectedItem(500);
        pageSizeCombo.setFont(Theme.FONT_BODY);
        pageSizeCombo.setBackground(Theme.BG_CARD);
        pageSizeCombo.setForeground(Theme.TEXT_PRI);
        pageSizeCombo.setMaximumSize(new Dimension(90, 34));

        prev.addActionListener(e -> { currentPage = Math.max(0, currentPage-1); renderPage(); });
        next.addActionListener(e -> {
            int maxPage = Math.max(0, (currentLines.length - 1) / pageSize);
            currentPage = Math.min(currentPage+1, maxPage);
            renderPage();
        });
        pageSizeCombo.addActionListener(e -> {
            pageSize = (Integer) pageSizeCombo.getSelectedItem();
            currentPage = 0;
            renderPage();
        });

        bar.add(prev);
        bar.add(pageLabel);
        bar.add(next);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(Theme.muted("Tokens por página:"));
        bar.add(pageSizeCombo);

        rawArea = Theme.terminal();
        rawArea.setText("  Carregue uma amostra para visualizar os tokens do texto.\n");

        JScrollPane sc = new JScrollPane(rawArea);
        sc.setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        sc.getViewport().setBackground(new Color(0x0C0C14));

        p.add(bar, BorderLayout.NORTH);
        p.add(sc,  BorderLayout.CENTER);
        return p;
    }

    // ── Lógica ─────────────────────────────────────────────────────────────────

    private void loadFile() {
        String sel = (String) fileCombo.getSelectedItem();
        if (sel == null || sel.contains("nenhum")) return;

        loadBar.setIndeterminate(true);
        statusLbl.setText("Carregando " + new File(sel).getName() + "…");

        SwingWorker<String[], Void> w = new SwingWorker<>() {
            @Override protected String[] doInBackground() throws Exception {
                return FileUtils.readLines(sel);
            }
            @Override protected void done() {
                try {
                    currentLines = get();
                    currentFile  = sel;
                    currentPage  = 0;
                    loadBar.setIndeterminate(false);
                    loadBar.setValue(100);
                    statusLbl.setText(new File(sel).getName() + "  ·  " +
                            String.format("%,d", currentLines.length) + " palavras carregadas");
                    updateStats();
                    updateFreqTable();
                    distChart.setData(buildFreqMap());
                    renderPage();
                } catch (Exception ex) {
                    statusLbl.setText("Erro: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void updateStats() {
        if (currentLines.length == 0) return;
        Map<String, Long> freq = buildFreqMap();
        Optional<Map.Entry<String,Long>> maxE = freq.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        Optional<Map.Entry<String,Long>> minE = freq.entrySet().stream()
                .min(Map.Entry.comparingByValue());
        double avgLen = Arrays.stream(currentLines)
                .mapToInt(String::length).average().orElse(0);

        lblTotal.setText(String.format("%,d", currentLines.length));
        lblUnique.setText(String.valueOf(freq.size()));
        lblMostFreq.setText(maxE.map(e -> e.getKey() +
                " (" + String.format("%,d", e.getValue()) + ")").orElse("—"));
        lblRarest.setText(minE.map(e -> e.getKey() +
                " (" + e.getValue() + ")").orElse("—"));
        lblAvgLine.setText(String.format("%.1f chars", avgLen));
    }

    private void updateFreqTable() {
        freqModel.setRowCount(0);
        Map<String, Long> freq = buildFreqMap();
        List<Map.Entry<String,Long>> sorted = freq.entrySet().stream()
                .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                .collect(Collectors.toList());
        long total = currentLines.length;
        int rank = 1;
        for (Map.Entry<String,Long> e : sorted) {
            double pct = 100.0 * e.getValue() / total;
            freqModel.addRow(new Object[]{rank++, e.getKey(),
                    String.format("%,d", e.getValue()),
                    String.format("%.1f%%", pct), ""});
        }
    }

    private void searchEvent() {
        if (currentLines.length == 0) {
            searchResult.setText("⚠  Carregue um arquivo primeiro.");
            searchResult.setForeground(Theme.ORANGE);
            return;
        }
        String q = FileUtils.normalizeQuery(searchField.getText().trim());
        if (q.isEmpty()) return;
        long count = Arrays.stream(currentLines).filter(l -> l.equals(q)).count();
        double pct = 100.0 * count / currentLines.length;
        searchResult.setText(String.format(
                "  %,d ocorrências de \"%s\"  (%.2f%%)", count, q, pct));
        searchResult.setForeground(count > 0 ? Theme.GREEN : Theme.TEXT_MUTED);
    }

    private void renderPage() {
        if (currentLines.length == 0) return;
        int total = currentLines.length;
        int maxPage = Math.max(0, (total - 1) / pageSize);
        currentPage = Math.min(currentPage, maxPage);

        int from = currentPage * pageSize;
        int to   = Math.min(from + pageSize, total);

        StringBuilder sb = new StringBuilder();
        for (int i = from; i < to; i++) {
            sb.append(String.format("%7d  %s%n", i + 1, currentLines[i]));
        }
        rawArea.setText(sb.toString());
        rawArea.setCaretPosition(0);

        pageLabel.setText(String.format("Página %d / %d  (%,d – %,d de %,d)",
                currentPage+1, maxPage+1, from+1, to, total));
    }

    private Map<String, Long> buildFreqMap() {
        return Arrays.stream(currentLines)
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
    }

    private void refreshFileList() {
        fileCombo.removeAllItems();
        File dir = new File(SampleConfig.SAMPLE_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles((d,n) -> n.endsWith(".txt"));
            if (files != null) {
                Arrays.sort(files);
                for (File f : files) fileCombo.addItem(f.getPath());
            }
        }
        if (fileCombo.getItemCount() == 0) fileCombo.addItem("(nenhuma amostra encontrada)");
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────────

    private JLabel sectionLbl(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Theme.TEXT_MUTED);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    // Os rótulos de statRow são armazenados em campos; construídos inline
    private JLabel statRow(String label, String defaultVal) {
        return new JLabel(defaultVal);  // configuração real em buildStatCard
    }

    private JPanel buildStatCard() {
        JPanel card = Theme.card(new GridLayout(5, 2, 8, 6));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        String[] labels = {"Total palavras", "Palavras únicas", "Mais comum", "Mais rara", "Comprimento médio"};
        JLabel[] values = {lblTotal, lblUnique, lblMostFreq, lblRarest, lblAvgLine};

        for (int i = 0; i < labels.length; i++) {
            JLabel lKey = new JLabel(labels[i]);
            lKey.setFont(Theme.FONT_SMALL);
            lKey.setForeground(Theme.TEXT_MUTED);

            values[i].setFont(Theme.FONT_SMALL);
            values[i].setForeground(Theme.TEXT_PRI);

            card.add(lKey);
            card.add(values[i]);
        }
        return card;
    }

    // ── Interno: gráfico de barras de distribuição ─────────────────────────────────────────

    static class DistributionChart extends JPanel {
        private Map<String, Long> data = new LinkedHashMap<>();
        private long total = 1;

        DistributionChart() {
            setBackground(Theme.BG_CARD);
            setBorder(BorderFactory.createLineBorder(Theme.DIVIDER_LT));
        }

        void setData(Map<String, Long> freq) {
            data = freq.entrySet().stream()
                    .sorted(Map.Entry.<String,Long>comparingByValue().reversed())
                    .limit(12)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (a, b) -> a, LinkedHashMap::new));
            total = freq.values().stream().mapToLong(Long::longValue).sum();
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data.isEmpty()) {
                g.setColor(Theme.TEXT_MUTED);
                g.setFont(Theme.FONT_BODY);
                String msg = "Carregue uma amostra para ver a distribuição";
                FontMetrics fm = g.getFontMetrics();
                g.drawString(msg, (getWidth()-fm.stringWidth(msg))/2, getHeight()/2);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padL = 160, padR = 24, padT = 24, padB = 16;
            int plotW = getWidth() - padL - padR;
            int plotH = getHeight() - padT - padB;

            List<Map.Entry<String,Long>> entries = new ArrayList<>(data.entrySet());
            int n = entries.size();
            int barH = Math.max(4, (plotH - (n-1)*6) / n);
            int gap  = 6;

            long maxVal = entries.stream().mapToLong(Map.Entry::getValue).max().orElse(1);

            // Título
            g2.setFont(new Font("SansSerif", Font.BOLD, 13));
            g2.setColor(Theme.TEXT_PRI);
            g2.drawString("Distribuição de Palavras (top " + n + ")", padL, padT - 6);

            Color[] palette = {Theme.ACCENT, Theme.GREEN, Theme.ORANGE,
                    Theme.RED, Theme.PURPLE, new Color(0x4EC9D8),
                    new Color(0xF5D74B), new Color(0xE573A8),
                    new Color(0x73E5A8), new Color(0xA873E5),
                    new Color(0xE5A873), new Color(0x73A8E5)};

            for (int i = 0; i < n; i++) {
                Map.Entry<String,Long> e = entries.get(i);
                int y = padT + i * (barH + gap);
                int w = (int)((double) e.getValue() / maxVal * plotW);

                Color c = palette[i % palette.length];

                // Trilha de fundo
                g2.setColor(new Color(c.getRed(),c.getGreen(),c.getBlue(),20));
                g2.fillRoundRect(padL, y, plotW, barH, 4, 4);

                // Barra
                g2.setColor(c);
                g2.fillRoundRect(padL, y, Math.max(w, 6), barH, 4, 4);

                // Nome da palavra
                g2.setFont(new Font(Font.MONOSPACED, Font.BOLD, 11));
                g2.setColor(Theme.TEXT_PRI);
                FontMetrics fm = g2.getFontMetrics();
                int ty = y + barH/2 + fm.getAscent()/2 - 1;
                g2.drawString(e.getKey(), padL - fm.stringWidth(e.getKey()) - 8, ty);

                // Contagem + percentual
                double pct = 100.0 * e.getValue() / total;
                String info = String.format("%,d  (%.1f%%)", e.getValue(), pct);
                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.TEXT_SEC);
                g2.drawString(info, padL + w + 8, ty);
            }
            g2.dispose();
        }
    }
}
