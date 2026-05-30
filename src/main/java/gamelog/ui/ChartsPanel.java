package gamelog.ui;

import gamelog.benchmark.BenchmarkResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

/**
 * Panel that shows the generated chart PNGs in a tabbed view.
 * Also allows regenerating charts from the existing CSV.
 */
public class ChartsPanel extends JPanel {

    private final MainWindow win;
    private JTabbedPane tabs;
    private JLabel statusLabel;

    public ChartsPanel(MainWindow win) {
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
                BorderFactory.createEmptyBorder(16, 28, 16, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(MainWindow.BG_PANEL);
        left.add(MainWindow.sectionTitle("📊  Gráficos"));

        statusLabel = new JLabel("");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(MainWindow.TEXT_DIM);
        left.add(statusLabel);

        header.add(left, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setBackground(MainWindow.BG_PANEL);
        JButton btnReload   = MainWindow.secondaryButton("🔄  Recarregar");
        JButton btnRegenCSV = MainWindow.primaryButton("📄  Regenerar do CSV");
        btnReload.addActionListener(e   -> loadCharts());
        btnRegenCSV.addActionListener(e -> regenFromCsv());
        btnRow.add(btnRegenCSV);
        btnRow.add(btnReload);
        header.add(btnRow, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // ── Tabs ──────────────────────────────────────────────────────────
        tabs = new JTabbedPane();
        tabs.setBackground(MainWindow.BG_DARK);
        tabs.setForeground(MainWindow.TEXT_MAIN);
        tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));
        add(tabs, BorderLayout.CENTER);

        loadCharts();
    }

    void loadCharts() {
        tabs.removeAll();

        String[][] charts = {
            { "⏱  Tempo Mediano",        MainWindow.CHARTS_DIR + "/chart_median_time.png"          },
            { "🧵  Threads",              MainWindow.CHARTS_DIR + "/chart_thread_impact.png"        },
            { "🚀  Speedup",              MainWindow.CHARTS_DIR + "/chart_speedup.png"              },
            { "📈  Throughput",           MainWindow.CHARTS_DIR + "/chart_throughput.png"           },
            { "🔁  3 Execuções",          MainWindow.CHARTS_DIR + "/chart_run_variation.png"        },
            { "📊  Média + Desvio",       MainWindow.CHARTS_DIR + "/chart_mean_stddev.png"          },
            { "📚  Tamanho do Texto",     MainWindow.CHARTS_DIR + "/chart_size_impact.png"          },
            { "⚖️  Normalizado",          MainWindow.CHARTS_DIR + "/chart_normalized_100k.png"      },
            { "🧮  Speedup Threads",      MainWindow.CHARTS_DIR + "/chart_cpu_speedup_threads.png"   },
            { "✅  Eficiência",           MainWindow.CHARTS_DIR + "/chart_parallel_efficiency.png"  },
            { "⚔️  CPU vs GPU",           MainWindow.CHARTS_DIR + "/chart_cpu_vs_gpu.png"            },
            { "🏆  Ranking",              MainWindow.CHARTS_DIR + "/chart_ranking_world.png"        },
            { "🔎  Densidade",            MainWindow.CHARTS_DIR + "/chart_word_density.png"         },
        };

        int loaded = 0;
        for (String[] entry : charts) {
            File f = new File(entry[1]);
            if (f.exists()) {
                tabs.addTab(entry[0], chartTab(f));
                loaded++;
            } else {
                tabs.addTab(entry[0], placeholderTab(entry[0]));
            }
        }

        if (loaded == 0) {
            statusLabel.setText("Nenhum gráfico encontrado — execute o benchmark primeiro.");
            statusLabel.setForeground(new Color(0xFFAA44));
        } else {
            statusLabel.setText(loaded + " de " + charts.length + " gráficos carregados.");
            statusLabel.setForeground(MainWindow.ACCENT3);
        }
    }

    private JPanel chartTab(File imgFile) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(MainWindow.BG_DARK);

        try {
            BufferedImage img = ImageIO.read(imgFile);
            JLabel imgLabel   = new JLabel(new ImageIcon(img));
            imgLabel.setHorizontalAlignment(SwingConstants.CENTER);

            JScrollPane scroll = new JScrollPane(imgLabel);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(MainWindow.BG_DARK);
            scroll.setBackground(MainWindow.BG_DARK);
            p.add(scroll, BorderLayout.CENTER);

            // File info bar
            JLabel info = new JLabel(String.format(
                    "  %s  ·  %dx%d px  ·  %.1f KB",
                    imgFile.getName(), img.getWidth(), img.getHeight(),
                    imgFile.length() / 1024.0));
            info.setFont(new Font("SansSerif", Font.PLAIN, 11));
            info.setForeground(MainWindow.TEXT_DIM);
            info.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(1, 0, 0, 0, MainWindow.BORDER_COL),
                    BorderFactory.createEmptyBorder(5, 8, 5, 8)));
            p.add(info, BorderLayout.SOUTH);

        } catch (IOException ex) {
            p.add(new JLabel("Erro ao carregar: " + ex.getMessage()), BorderLayout.CENTER);
        }
        return p;
    }

    private JPanel placeholderTab(String name) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(MainWindow.BG_DARK);

        JPanel box = MainWindow.card(new BoxLayout(null, BoxLayout.Y_AXIS));
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JLabel ico = new JLabel("📊");
        ico.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        ico.setAlignmentX(CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Gráfico não disponível");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(MainWindow.TEXT_DIM);
        lbl.setAlignmentX(CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Execute o benchmark para gerar " + name);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(MainWindow.TEXT_DIM);
        hint.setAlignmentX(CENTER_ALIGNMENT);

        box.add(Box.createVerticalStrut(8));
        box.add(ico);
        box.add(Box.createVerticalStrut(12));
        box.add(lbl);
        box.add(Box.createVerticalStrut(6));
        box.add(hint);
        box.add(Box.createVerticalStrut(8));

        p.add(box);
        return p;
    }

    private void regenFromCsv() {
        File csv = new File(MainWindow.CSV_PATH);
        if (!csv.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Arquivo CSV não encontrado.\nExecute o benchmark primeiro.",
                    "CSV não encontrado", JOptionPane.WARNING_MESSAGE);
            return;
        }
        statusLabel.setText("Regenerando...");
        statusLabel.setForeground(MainWindow.ACCENT);

        SwingWorker<Void, Void> w = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                List<BenchmarkResult> results = gamelog.CsvReader.read(MainWindow.CSV_PATH);
                ChartGenerator.generateAll(results, MainWindow.CHARTS_DIR);
                return null;
            }
            @Override protected void done() {
                loadCharts();
            }
        };
        w.execute();
    }
}
