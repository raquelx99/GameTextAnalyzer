package gamelog.ui;

import gamelog.benchmark.BenchmarkResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel that shows the generated chart PNGs grouped by analysis category.
 *
 * Instead of placing many charts in a single long tab bar, this panel uses:
 * 1. category tabs (general, CPU parallelism, GPU, narrative, etc.);
 * 2. arrow navigation inside each category;
 * 3. keyboard shortcuts with left/right arrows.
 */
public class ChartsPanel extends JPanel {

    private final MainWindow win;
    private JTabbedPane categoryTabs;
    private JLabel statusLabel;

    private static final class ChartInfo {
        final String title;
        final String description;
        final String fileName;

        ChartInfo(String title, String description, String fileName) {
            this.title = title;
            this.description = description;
            this.fileName = fileName;
        }

        File file() {
            return new File(MainWindow.CHARTS_DIR + "/" + fileName);
        }
    }

    private static final class ChartCategory {
        final String title;
        final String subtitle;
        final List<ChartInfo> charts;

        ChartCategory(String title, String subtitle, ChartInfo... charts) {
            this.title = title;
            this.subtitle = subtitle;
            this.charts = List.of(charts);
        }
    }

    public ChartsPanel(MainWindow win) {
        this.win = win;
        setBackground(MainWindow.BG_DARK);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.BORDER_COL),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(MainWindow.BG_PANEL);
        left.add(MainWindow.sectionTitle("Gráficos"));

        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(MainWindow.TEXT_DIM);
        left.add(statusLabel);
        header.add(left, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(MainWindow.BG_PANEL);
        JButton btnReload   = MainWindow.secondaryButton("Recarregar");
        JButton btnRegenCSV = MainWindow.primaryButton("Regenerar do CSV");
        btnReload.addActionListener(e   -> loadCharts());
        btnRegenCSV.addActionListener(e -> regenFromCsv());
        btnRow.add(btnRegenCSV);
        btnRow.add(btnReload);
        header.add(btnRow, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        categoryTabs = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        categoryTabs.setBackground(MainWindow.BG_DARK);
        categoryTabs.setForeground(MainWindow.TEXT_MAIN);
        categoryTabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        add(categoryTabs, BorderLayout.CENTER);

        loadCharts();
    }

    void loadCharts() {
        categoryTabs.removeAll();

        int loaded = 0;
        int total = 0;

        for (ChartCategory category : categories()) {
            CategoryCarouselPanel panel = new CategoryCarouselPanel(category);
            categoryTabs.addTab(category.title, panel);
            loaded += panel.loadedCount;
            total += category.charts.size();
        }

        if (loaded == 0) {
            statusLabel.setText("Nenhum gráfico encontrado — execute o benchmark primeiro.");
            statusLabel.setForeground(new Color(0xFFAA44));
        } else {
            statusLabel.setText(loaded + " de " + total + " gráficos carregados. Use as setas para navegar dentro de cada aba.");
            statusLabel.setForeground(MainWindow.ACCENT3);
        }
    }

    private List<ChartCategory> categories() {
        List<ChartCategory> result = new ArrayList<>();

        result.add(new ChartCategory(
                "Comparação direta",
                "Comparações diretas entre os métodos — responde à exigência central do enunciado.",
                new ChartInfo("Tempo mediano por método",
                        "Uma barra por método em cada obra. Menor = melhor. SerialCPU é o baseline.",
                        "chart_01_tempo_mediano.png"),
                new ChartInfo("Speedup vs. SerialCPU",
                        "Speedup = tempo_serial / tempo_método. Valor > 1 significa mais rápido que o serial.",
                        "chart_02_speedup_vs_serial.png")
        ));

        result.add(new ChartCategory(
                "Impacto de núcleos",
                "Efeito do número de threads no ParallelCPU — exigência explícita do enunciado.",
                new ChartInfo("Threads × Tempo",
                        "Uma curva por obra: como o tempo cai conforme threads aumentam.",
                        "chart_03_threads_tempo.png"),
                new ChartInfo("Threads × Speedup",
                        "Mostra onde o ganho para de crescer (lei de Amdahl na prática).",
                        "chart_04_threads_speedup.png"),
                new ChartInfo("Eficiência paralela",
                        "Eficiência = speedup / threads. Valor ideal = 1.0. Queda revela overhead.",
                        "chart_05_amdahl_efficiency.png")
        ));

        result.add(new ChartCategory(
                "Impacto do tamanho",
                "Como o volume do texto afeta o tempo — exigência explícita do enunciado.",
                new ChartInfo("Escalabilidade por tamanho",
                        "Eixo X = palavras do texto. Eixo Y = tempo mediano. Uma curva por método.",
                        "chart_06_escala_tamanho.png"),
                new ChartInfo("Tempo normalizado por 100 mil palavras",
                        "Remove o viés de tamanho: compara a velocidade real de cada método.",
                        "chart_07_tempo_normalizado.png")
        ));

        result.add(new ChartCategory(
                "Estabilidade",
                "Variabilidade entre as 3 execuções — análise estatística exigida no enunciado.",
                new ChartInfo("3 execuções individuais",
                        "Cada barra é uma execução. Barras iguais = método estável.",
                        "chart_08_tres_execucoes.png"),
                new ChartInfo("Média ± desvio padrão",
                        "Barras = média das 3 execuções. Whiskers = desvio padrão.",
                        "chart_09_media_desvio.png")
        ));

        result.add(new ChartCategory(
                "GPU vs CPU",
                "Comparação destacada da GPU — método mais complexo do trabalho.",
                new ChartInfo("GPU vs melhor CPU paralela vs Serial",
                        "Resume os três protagonistas: baseline, melhor CPU paralela e GPU/OpenCL.",
                        "chart_10_gpu_vs_cpu.png")
        ));

        return result;
    }

    private final class CategoryCarouselPanel extends JPanel {
        private final ChartCategory category;
        private final CardLayout cardLayout = new CardLayout();
        private final JPanel cardPanel = new JPanel(cardLayout);
        private final JLabel counterLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel descriptionLabel = new JLabel();
        private final JComboBox<String> chartSelector = new JComboBox<>();
        private int index = 0;
        private final int loadedCount;

        CategoryCarouselPanel(ChartCategory category) {
            this.category = category;
            setLayout(new BorderLayout());
            setBackground(MainWindow.BG_DARK);

            int loaded = 0;
            for (ChartInfo chart : category.charts) {
                if (chart.file().exists()) loaded++;
            }
            this.loadedCount = loaded;

            buildTopBar();
            buildCards();
            buildBottomBar();
            updateState();
            installKeyboardNavigation();
        }

        private void buildTopBar() {
            JPanel top = new JPanel(new BorderLayout(16, 8));
            top.setBackground(MainWindow.BG_DARK);
            top.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));

            JPanel text = new JPanel();
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setBackground(MainWindow.BG_DARK);

            JLabel categoryLabel = new JLabel(category.subtitle);
            categoryLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            categoryLabel.setForeground(MainWindow.TEXT_DIM);
            categoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
            titleLabel.setForeground(MainWindow.TEXT_MAIN);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            descriptionLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            descriptionLabel.setForeground(MainWindow.TEXT_DIM);
            descriptionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            text.add(categoryLabel);
            text.add(Box.createVerticalStrut(6));
            text.add(titleLabel);
            text.add(Box.createVerticalStrut(3));
            text.add(descriptionLabel);
            top.add(text, BorderLayout.CENTER);

            for (ChartInfo chart : category.charts) {
                chartSelector.addItem(chart.title);
            }
            chartSelector.setFont(new Font("SansSerif", Font.PLAIN, 12));
            chartSelector.setPreferredSize(new Dimension(270, 34));
            chartSelector.addActionListener(e -> {
                int selected = chartSelector.getSelectedIndex();
                if (selected >= 0 && selected != index) {
                    index = selected;
                    showCurrent();
                }
            });
            top.add(chartSelector, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
        }

        private void buildCards() {
            cardPanel.setBackground(MainWindow.BG_DARK);
            for (int i = 0; i < category.charts.size(); i++) {
                ChartInfo chart = category.charts.get(i);
                File f = chart.file();
                if (f.exists()) {
                    cardPanel.add(chartView(f), String.valueOf(i));
                } else {
                    cardPanel.add(placeholderView(chart), String.valueOf(i));
                }
            }
            add(cardPanel, BorderLayout.CENTER);
        }

        private void buildBottomBar() {
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.setBackground(Theme.BG_SURFACE);
            bottom.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.BORDER_COL),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)));

            JButton prev = MainWindow.secondaryButton("◀ Anterior");
            JButton next = MainWindow.secondaryButton("Próximo ▶");
            prev.addActionListener(e -> previous());
            next.addActionListener(e -> next());

            JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
            nav.setBackground(Theme.BG_SURFACE);
            nav.add(prev);
            counterLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
            counterLabel.setForeground(MainWindow.TEXT_MAIN);
            counterLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            nav.add(counterLabel);
            nav.add(next);

            JLabel hint = new JLabel("Dica: use as setas ← e → do teclado para navegar.");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
            hint.setForeground(MainWindow.TEXT_DIM);

            bottom.add(hint, BorderLayout.WEST);
            bottom.add(nav, BorderLayout.CENTER);
            add(bottom, BorderLayout.SOUTH);
        }

        private JPanel chartView(File imgFile) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(MainWindow.BG_DARK);

            try {
                BufferedImage img = ImageIO.read(imgFile);
                ZoomableImagePanel zoomPanel = new ZoomableImagePanel(img);

                JScrollPane scroll = new JScrollPane(zoomPanel);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                scroll.getViewport().setBackground(MainWindow.BG_DARK);
                scroll.setBackground(MainWindow.BG_DARK);
                scroll.getVerticalScrollBar().setUnitIncrement(24);
                scroll.getHorizontalScrollBar().setUnitIncrement(24);

                // ── Zoom toolbar ────────────────────────────────────────────
                JLabel zoomLabel = new JLabel("100%");
                zoomLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
                zoomLabel.setForeground(MainWindow.TEXT_MAIN);
                zoomLabel.setPreferredSize(new Dimension(52, 24));
                zoomLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JSlider slider = new JSlider(10, 300, 100);
                slider.setBackground(Theme.BG_SURFACE);
                slider.setForeground(MainWindow.TEXT_DIM);
                slider.setPreferredSize(new Dimension(200, 26));
                slider.setMajorTickSpacing(100);
                slider.setPaintTicks(true);
                slider.setSnapToTicks(false);

                slider.addChangeListener(e -> {
                    int pct = slider.getValue();
                    zoomPanel.setZoom(pct / 100.0);
                    zoomLabel.setText(pct + "%");
                });

                JButton fitBtn    = MainWindow.secondaryButton("Ajustar");
                JButton zoomInBtn = MainWindow.secondaryButton("+");
                JButton zoomOutBtn= MainWindow.secondaryButton("−");

                fitBtn.setToolTipText("Ajustar à janela");
                zoomInBtn.setPreferredSize(new Dimension(36, 28));
                zoomOutBtn.setPreferredSize(new Dimension(36, 28));

                fitBtn.addActionListener(e -> applyFitZoom(scroll, img, slider));
                zoomInBtn.addActionListener(e  -> slider.setValue(Math.min(300, slider.getValue() + 15)));
                zoomOutBtn.addActionListener(e -> slider.setValue(Math.max(10,  slider.getValue() - 15)));

                // Ctrl + scroll wheel
                scroll.addMouseWheelListener(e -> {
                    if (e.isControlDown()) {
                        int delta = e.getWheelRotation() < 0 ? 15 : -15;
                        slider.setValue(Math.max(10, Math.min(300, slider.getValue() + delta)));
                    } else {
                        // pass to scroll pane
                        scroll.getVerticalScrollBar().setValue(
                            scroll.getVerticalScrollBar().getValue() + e.getWheelRotation() * 24);
                    }
                });

                JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 5));
                toolbar.setBackground(Theme.BG_SURFACE);
                toolbar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.BORDER_COL));
                toolbar.add(zoomOutBtn);
                toolbar.add(slider);
                toolbar.add(zoomInBtn);
                toolbar.add(zoomLabel);
                toolbar.add(Box.createHorizontalStrut(8));
                toolbar.add(fitBtn);

                JLabel info = new JLabel(String.format(
                        "  %s  ·  %dx%d px  ·  %.1f KB  ·  Ctrl + scroll para zoom",
                        imgFile.getName(), img.getWidth(), img.getHeight(), imgFile.length() / 1024.0));
                info.setFont(new Font("SansSerif", Font.PLAIN, 11));
                info.setForeground(MainWindow.TEXT_DIM);
                info.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

                p.add(toolbar, BorderLayout.NORTH);
                p.add(scroll, BorderLayout.CENTER);
                p.add(info, BorderLayout.SOUTH);

                // Auto-fit after layout is realized
                SwingUtilities.invokeLater(() -> applyFitZoom(scroll, img, slider));

            } catch (IOException ex) {
                p.add(errorView("Erro ao carregar gráfico: " + ex.getMessage()), BorderLayout.CENTER);
            }
            return p;
        }

        private static void applyFitZoom(JScrollPane scroll, BufferedImage img, JSlider slider) {
            Dimension vp = scroll.getViewport().getSize();
            if (vp.width < 10 || vp.height < 10) return;
            double sx = (double) vp.width  / img.getWidth();
            double sy = (double) vp.height / img.getHeight();
            int pct = (int) Math.round(Math.min(sx, sy) * 93);
            slider.setValue(Math.max(10, Math.min(300, pct)));
        }

        private JPanel placeholderView(ChartInfo chart) {
            JPanel p = new JPanel(new GridBagLayout());
            p.setBackground(MainWindow.BG_DARK);

            JPanel box = MainWindow.card(new BoxLayout(null, BoxLayout.Y_AXIS));
            box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
            box.setPreferredSize(new Dimension(360, 180));

            JLabel ico = new JLabel("▧");
            ico.setFont(new Font("SansSerif", Font.BOLD, 40));
            ico.setForeground(MainWindow.TEXT_DIM);
            ico.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lbl = new JLabel("Gráfico não disponível");
            lbl.setFont(new Font("SansSerif", Font.BOLD, 15));
            lbl.setForeground(MainWindow.TEXT_MAIN);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel hint = new JLabel("Execute o benchmark ou regenere a partir do CSV.");
            hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
            hint.setForeground(MainWindow.TEXT_DIM);
            hint.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel file = new JLabel(chart.fileName);
            file.setFont(new Font("Monospaced", Font.PLAIN, 11));
            file.setForeground(MainWindow.TEXT_DIM);
            file.setAlignmentX(Component.CENTER_ALIGNMENT);

            box.add(Box.createVerticalGlue());
            box.add(ico);
            box.add(Box.createVerticalStrut(10));
            box.add(lbl);
            box.add(Box.createVerticalStrut(6));
            box.add(hint);
            box.add(Box.createVerticalStrut(8));
            box.add(file);
            box.add(Box.createVerticalGlue());

            p.add(box);
            return p;
        }

        private JComponent errorView(String message) {
            JLabel lbl = new JLabel(message, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            lbl.setForeground(new Color(0xFFAA44));
            return lbl;
        }

        private void previous() {
            index = (index - 1 + category.charts.size()) % category.charts.size();
            showCurrent();
        }

        private void next() {
            index = (index + 1) % category.charts.size();
            showCurrent();
        }

        private void showCurrent() {
            cardLayout.show(cardPanel, String.valueOf(index));
            updateState();
        }

        private void updateState() {
            ChartInfo chart = category.charts.get(index);
            titleLabel.setText(chart.title);
            descriptionLabel.setText(chart.description);
            counterLabel.setText((index + 1) + " / " + category.charts.size());
            if (chartSelector.getSelectedIndex() != index) {
                chartSelector.setSelectedIndex(index);
            }
        }

        private void installKeyboardNavigation() {
            InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
            ActionMap actionMap = getActionMap();
            inputMap.put(KeyStroke.getKeyStroke("LEFT"), "previousChart");
            inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "nextChart");
            actionMap.put("previousChart", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { previous(); }
            });
            actionMap.put("nextChart", new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) { next(); }
            });
        }
    }

    private static final class ZoomableImagePanel extends JPanel {
        private final BufferedImage img;
        private double zoom = 1.0;

        ZoomableImagePanel(BufferedImage img) {
            this.img = img;
            setBackground(MainWindow.BG_DARK);
        }

        void setZoom(double z) {
            this.zoom = Math.max(0.05, Math.min(5.0, z));
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(
                (int) Math.round(img.getWidth()  * zoom),
                (int) Math.round(img.getHeight() * zoom));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            int iw = (int) Math.round(img.getWidth()  * zoom);
            int ih = (int) Math.round(img.getHeight() * zoom);
            int x  = Math.max(0, (getWidth()  - iw) / 2);
            int y  = Math.max(0, (getHeight() - ih) / 2);
            g2.drawImage(img, x, y, iw, ih, null);
        }
    }

    private void regenFromCsv() {
        File csv = new File(MainWindow.CSV_PATH);
        if (!csv.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Arquivo CSV não encontrado.\nExecute o benchmark primeiro.",
                    "CSV não encontrado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusLabel.setText("Regenerando gráficos...");
        statusLabel.setForeground(MainWindow.ACCENT);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                List<BenchmarkResult> results = gamelog.CsvReader.read(MainWindow.CSV_PATH);
                ChartGenerator.generateAll(results, MainWindow.CHARTS_DIR);
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    loadCharts();
                } catch (Exception ex) {
                    statusLabel.setText("Erro ao regenerar gráficos.");
                    statusLabel.setForeground(new Color(0xFFAA44));
                    JOptionPane.showMessageDialog(ChartsPanel.this,
                            "Erro ao regenerar gráficos:\n" + ex.getMessage(),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        w.execute();
    }
}
