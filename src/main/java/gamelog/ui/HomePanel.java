package gamelog.ui;

import javax.swing.*;
import java.awt.*;
import java.io.*;

public class HomePanel extends JPanel {

    private final MainWindow win;

    public HomePanel(MainWindow win) {
        this.win = win;
        setBackground(Theme.BG_BASE);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        JPanel scroll = new JPanel();
        scroll.setBackground(Theme.BG_BASE);
        scroll.setLayout(new BoxLayout(scroll, BoxLayout.Y_AXIS));
        scroll.setBorder(BorderFactory.createEmptyBorder(32, 36, 32, 36));

        // ── Hero ──────────────────────────────────────────────────────────
        JPanel hero = new JPanel(new BorderLayout(0, 6));
        hero.setBackground(Theme.BG_BASE);
        hero.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("GameText Analyzer");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRI);

        JLabel desc = Theme.muted("Benchmark paralelo de palavras em textos literários · SerialCPU · ParallelCPU · ParallelGPU");
        hero.add(title, BorderLayout.NORTH);
        hero.add(desc,  BorderLayout.CENTER);
        scroll.add(hero);
        scroll.add(Box.createVerticalStrut(28));

        // ── KPI row ───────────────────────────────────────────────────────
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 14, 0));
        kpiRow.setBackground(Theme.BG_BASE);
        kpiRow.setAlignmentX(LEFT_ALIGNMENT);
        kpiRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));

        File dataDir   = new File(MainWindow.SAMPLE_DIR);
        File csvFile   = new File(MainWindow.CSV_PATH);
        File chartsDir = new File(MainWindow.CHARTS_DIR);
        int  logs      = countFiles(dataDir,   ".txt");
        int  charts    = countFiles(chartsDir, ".png");
        long runs      = countCsvLines(csvFile);

        kpiRow.add(kpi("Amostras",    String.valueOf(logs),  "textos",  Theme.ACCENT));
        kpiRow.add(kpi("Execuções CSV",   String.valueOf(runs),  "registros", runs>0 ? Theme.GREEN : Theme.TEXT_MUTED));
        kpiRow.add(kpi("Gráficos",        String.valueOf(charts),"gerados",   charts>0 ? Theme.ORANGE : Theme.TEXT_MUTED));
        kpiRow.add(kpi("Configurações",   "5",                   "Serial · CPU 2/4/8 · GPU", Theme.PURPLE));
        scroll.add(kpiRow);
        scroll.add(Box.createVerticalStrut(28));

        // ── Quick actions row ─────────────────────────────────────────────
        JLabel actTitle = Theme.h3("Acesso rápido");
        actTitle.setAlignmentX(LEFT_ALIGNMENT);
        scroll.add(actTitle);
        scroll.add(Box.createVerticalStrut(10));

        JPanel actions = new JPanel(new GridLayout(1, 4, 14, 0));
        actions.setBackground(Theme.BG_BASE);
        actions.setAlignmentX(LEFT_ALIGNMENT);
        actions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        actions.add(actionCard("▣  Amostras",      "Veja os textos obrigatórios",      Theme.ORANGE, "generate"));
        actions.add(actionCard("▷  Benchmark","Compare Serial, CPU e GPU",     Theme.ACCENT,  "benchmark"));
        actions.add(actionCard("◎  Explorar",    "Frequência e distribuição",Theme.GREEN,   "analysis"));
        actions.add(actionCard("◇  Comparar",    "Compare termos entre obras",Theme.PURPLE,   "compare"));
        scroll.add(actions);
        scroll.add(Box.createVerticalStrut(28));

        // ── About card ────────────────────────────────────────────────────
        JPanel about = Theme.card(new BorderLayout(0, 10));
        about.setBorder(BorderFactory.createCompoundBorder(about.getBorder(),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        about.setAlignmentX(LEFT_ALIGNMENT);
        about.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel aboutTitle = Theme.h3("Sobre o projeto");
        String html =
            "<html><body style='font-family:SansSerif;font-size:12px;color:#9898B8;width:700px'>" +
            "O <b style='color:#EDEDF5'>GameText Analyzer</b> compara três abordagens de " +
            "processamento para contar ocorrências de palavras em textos literários: " +
            "<b style='color:#EDEDF5'>SerialCPU</b>, <b style='color:#EDEDF5'>ParallelCPU</b> e " +
            "<b style='color:#EDEDF5'>ParallelGPU</b>.<br><br>" +
            "As amostras obrigatórias são tratadas como mundos narrativos de jogo: " +
            "<b>Don Quixote</b> como RPG de cavalaria, <b>Dracula</b> como survival horror " +
            "e <b>Moby Dick</b> como aventura naval/boss hunt. Os resultados são salvos em CSV " +
            "e exibidos em gráficos de tempo, speedup, eficiência e throughput." +
            "</body></html>";
        JLabel aboutDesc = new JLabel(html);

        about.add(aboutTitle,  BorderLayout.NORTH);
        about.add(aboutDesc,   BorderLayout.CENTER);
        scroll.add(about);
        scroll.add(Box.createVerticalStrut(18));
        scroll.add(featureGrid());

        JScrollPane sp = new JScrollPane(scroll);
        sp.setBorder(null);
        sp.getViewport().setBackground(Theme.BG_BASE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(sp, BorderLayout.CENTER);
    }

    private JPanel kpi(String label, String value, String unit, Color accent) {
        JPanel card = Theme.card(new BorderLayout(0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));

        JLabel lbl = new JLabel(label);
        lbl.setFont(Theme.FONT_SMALL);
        lbl.setForeground(Theme.TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 26));
        val.setForeground(accent);

        JLabel u = new JLabel(unit);
        u.setFont(Theme.FONT_SMALL);
        u.setForeground(Theme.TEXT_SEC);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(val);

        card.add(lbl,    BorderLayout.NORTH);
        card.add(bottom, BorderLayout.CENTER);
        card.add(u,      BorderLayout.SOUTH);
        return card;
    }

    private JPanel featureGrid() {
        JPanel grid = new JPanel(new GridLayout(1, 3, 14, 0));
        grid.setBackground(Theme.BG_BASE);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        grid.add(feature("Mundos narrativos", "Cada amostra obrigatória é interpretada como uma proposta de adaptação: RPG, horror e aventura naval.", Theme.ACCENT));
        grid.add(feature("Comparação normalizada", "O comparador usa ocorrências por 10 mil palavras para equilibrar textos de tamanhos diferentes.", Theme.GREEN));
        grid.add(feature("Entrega acadêmica", "Mantém CSV, gráficos, README e as medições SerialCPU, ParallelCPU e ParallelGPU.", Theme.ORANGE));
        return grid;
    }

    private JPanel feature(String title, String desc, Color accent) {
        JPanel p = Theme.card(new BorderLayout(0, 6));
        p.setBorder(BorderFactory.createCompoundBorder(p.getBorder(), BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_H3);
        t.setForeground(accent);
        JLabel d = new JLabel("<html><body style='width:210px;color:#A8A8C8'>" + desc + "</body></html>");
        d.setFont(Theme.FONT_SMALL);
        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        return p;
    }

    private JPanel actionCard(String title, String desc, Color accent, String card) {
        JPanel p = new JPanel(new BorderLayout(0, 8)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(Theme.DIVIDER_LT);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                // subtle top accent line
                g2.setColor(accent);
                g2.fillRoundRect(14, 0, 40, 3, 3, 3);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(BorderFactory.createCompoundBorder(p.getBorder(),
                BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        p.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_H3);
        t.setForeground(accent);

        JLabel d = new JLabel("<html><body style='width:160px'>" + desc + "</body></html>");
        d.setFont(Theme.FONT_SMALL);
        d.setForeground(Theme.TEXT_SEC);

        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.CENTER);
        p.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) { win.showCard(card); }
        });
        return p;
    }

    private int countFiles(File dir, String ext) {
        if (!dir.exists()) return 0;
        File[] f = dir.listFiles((d,n) -> n.endsWith(ext));
        return f == null ? 0 : f.length;
    }
    private long countCsvLines(File f) {
        if (!f.exists()) return 0;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            return Math.max(0, br.lines().count() - 1);
        } catch (Exception e) { return 0; }
    }
}
