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
                "Visão geral",
                "Comparações principais entre todas as estratégias e amostras.",
                new ChartInfo("Tempo mediano por método", "Compara o tempo mediano das estratégias em cada obra.", "chart_median_time.png"),
                new ChartInfo("Impacto do tamanho do texto", "Mostra como o total de palavras impacta o tempo de execução.", "chart_size_impact.png"),
                new ChartInfo("Tempo normalizado por 100 mil palavras", "Permite comparar textos de tamanhos diferentes de forma mais justa.", "chart_normalized_100k.png"),
                new ChartInfo("Tempo médio com desvio padrão", "Evidencia média e estabilidade estatística dos métodos.", "chart_mean_stddev.png"),
                new ChartInfo("Variação entre as 3 execuções", "Mostra as amostras exigidas no enunciado e a oscilação dos tempos.", "chart_run_variation.png")
        ));

        result.add(new ChartCategory(
                "Paralelismo CPU",
                "Gráficos focados em threads tradicionais, ForkJoin e ParallelStream.",
                new ChartInfo("Impacto do número de threads", "Analisa 2, 4, 8 e núcleos disponíveis no ParallelCPU.", "chart_thread_impact.png"),
                new ChartInfo("Speedup da CPU paralela por threads", "Mostra o ganho em relação ao SerialCPU conforme o paralelismo aumenta.", "chart_cpu_speedup_threads.png"),
                new ChartInfo("Eficiência paralela", "Mostra o aproveitamento das threads usando speedup dividido por threads.", "chart_parallel_efficiency.png"),
                new ChartInfo("Vertente CPU paralela", "Compara FixedThreadPool, ForkJoin e ParallelStream.", "chart_cpu_strategy_branch.png")
        ));

        result.add(new ChartCategory(
                "Speedup e vazão",
                "Métricas derivadas para avaliar ganho real e capacidade de processamento.",
                new ChartInfo("Speedup em relação ao SerialCPU", "Indica quantas vezes cada método foi mais rápido ou mais lento que o serial.", "chart_speedup.png"),
                new ChartInfo("Throughput", "Mostra quantas palavras foram processadas por milissegundo.", "chart_throughput.png"),
                new ChartInfo("Melhor desempenho por família", "Compara a melhor estratégia de cada família de paralelização.", "chart_best_by_family.png")
        ));

        result.add(new ChartCategory(
                "GPU e virtual threads",
                "Gráficos para investigar OpenCL/GPU, fallback e virtual threads.",
                new ChartInfo("SerialCPU vs melhor ParallelCPU vs GPU", "Resume os principais competidores em cada amostra.", "chart_cpu_vs_gpu.png"),
                new ChartInfo("Vertente GPU/OpenCL", "Compara GPU/fallback contra serial, melhor CPU paralela e virtual threads.", "chart_gpu_branch.png"),
                new ChartInfo("Vertente Virtual Threads", "Compara granularidades diferentes de virtual threads.", "chart_virtual_threads_branch.png")
        ));

        result.add(new ChartCategory(
                "Análise narrativa",
                "Gráficos que sustentam a camada gamificada dos mundos literários.",
                new ChartInfo("Ranking por mundo narrativo", "Apresenta o método vencedor em cada obra/mundo narrativo.", "chart_ranking_world.png"),
                new ChartInfo("Densidade da palavra-tema", "Mostra a frequência da palavra buscada a cada 10 mil palavras.", "chart_word_density.png")
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
                JLabel imgLabel = new JLabel(new ImageIcon(img));
                imgLabel.setHorizontalAlignment(SwingConstants.CENTER);
                imgLabel.setVerticalAlignment(SwingConstants.CENTER);

                JScrollPane scroll = new JScrollPane(imgLabel);
                scroll.setBorder(BorderFactory.createEmptyBorder());
                scroll.getViewport().setBackground(MainWindow.BG_DARK);
                scroll.setBackground(MainWindow.BG_DARK);
                scroll.getVerticalScrollBar().setUnitIncrement(18);
                scroll.getHorizontalScrollBar().setUnitIncrement(18);
                p.add(scroll, BorderLayout.CENTER);

                JLabel info = new JLabel(String.format(
                        "  %s  ·  %dx%d px  ·  %.1f KB",
                        imgFile.getName(), img.getWidth(), img.getHeight(), imgFile.length() / 1024.0));
                info.setFont(new Font("SansSerif", Font.PLAIN, 11));
                info.setForeground(MainWindow.TEXT_DIM);
                info.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                p.add(info, BorderLayout.SOUTH);
            } catch (IOException ex) {
                p.add(errorView("Erro ao carregar gráfico: " + ex.getMessage()), BorderLayout.CENTER);
            }
            return p;
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
