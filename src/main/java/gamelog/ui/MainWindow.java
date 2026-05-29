package gamelog.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class MainWindow extends JFrame {

    // Keep legacy constants so other panels compile without changes
    static final Color BG_DARK    = Theme.BG_BASE;
    static final Color BG_PANEL   = Theme.BG_SURFACE;
    static final Color BG_CARD    = Theme.BG_CARD;
    static final Color ACCENT     = Theme.ACCENT;
    static final Color ACCENT2    = Theme.ORANGE;
    static final Color ACCENT3    = Theme.GREEN;
    static final Color TEXT_MAIN  = Theme.TEXT_PRI;
    static final Color TEXT_DIM   = Theme.TEXT_SEC;
    static final Color BORDER_COL = Theme.DIVIDER;

    static final String DATA_DIR    = "data";
    static final String SAMPLE_DIR  = DATA_DIR + "/samples";
    static final String RESULTS_DIR = "results";
    static final String CHARTS_DIR  = "charts";
    static final String CSV_PATH    = RESULTS_DIR + "/resultados.csv";

    private CardLayout cardLayout;
    private JPanel     cardPanel;
    private final java.util.Map<String, JButton> navButtons = new java.util.LinkedHashMap<>();
    private String currentCard = "home";

    public MainWindow() {
        super("GameText Analyzer");
        Theme.apply();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 760);
        setMinimumSize(new Dimension(960, 640));
        setLocationRelativeTo(null);
        setBackground(Theme.BG_BASE);
        buildLayout();
        setVisible(true);
    }

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG_BASE);
        root.add(buildSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(Theme.BG_BASE);
        cardPanel.add(new HomePanel(this),      "home");
        cardPanel.add(new BenchmarkPanel(this), "benchmark");
        cardPanel.add(new LogAnalysisPanel(this), "analysis");
        cardPanel.add(new WordComparisonPanel(this), "compare");
        cardPanel.add(new ChartsPanel(this),    "charts");
        cardPanel.add(new SamplesPanel(this),   "generate");

        root.add(cardPanel, BorderLayout.CENTER);
        setContentPane(root);
    }

    private JPanel buildSidebar() {
        JPanel side = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.DIVIDER);
                g2.drawLine(getWidth()-1, 0, getWidth()-1, getHeight());
                g2.dispose();
            }
        };
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(Theme.BG_SURFACE);
        side.setPreferredSize(new Dimension(220, 0));

        // ── Logo ─────────────────────────────────────────────────────────
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Theme.DIVIDER);
                g2.drawLine(0, getHeight()-1, getWidth(), getHeight()-1);
                g2.dispose();
            }
        };
        logo.setBackground(Theme.BG_SURFACE);
        logo.setMaximumSize(new Dimension(220, 76));
        logo.setPreferredSize(new Dimension(220, 76));
        logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));

        JLabel gameIcon = new JLabel("◈  GameText");
        gameIcon.setFont(new Font("SansSerif", Font.BOLD, 17));
        gameIcon.setForeground(Theme.TEXT_PRI);
        gameIcon.setAlignmentX(LEFT_ALIGNMENT);
        gameIcon.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));

        JLabel sub = new JLabel("  Analyzer  v2.0");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.ACCENT);
        sub.setAlignmentX(LEFT_ALIGNMENT);
        sub.setBorder(BorderFactory.createEmptyBorder(3, 20, 0, 0));

        logo.add(Box.createVerticalStrut(20));
        logo.add(gameIcon);
        logo.add(sub);
        logo.add(Box.createVerticalGlue());

        side.add(logo);
        side.add(Box.createVerticalStrut(8));

        // ── Nav section: Main ─────────────────────────────────────────────
        side.add(navSection("PRINCIPAL"));
        side.add(navItem("home",       "⌂",  "Dashboard"));
        side.add(navItem("benchmark",  "▷",  "Benchmark"));
        side.add(navItem("analysis",   "◎",  "Explorar Textos"));
        side.add(navItem("compare",    "◇",  "Comparador"));

        side.add(Box.createVerticalStrut(4));
        side.add(navSection("DADOS"));
        side.add(navItem("charts",     "⬟",  "Gráficos"));
        side.add(navItem("generate",   "▣",  "Amostras Oficiais"));

        side.add(Box.createVerticalGlue());

        // ── Footer ────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Theme.BG_SURFACE);
        footer.setMaximumSize(new Dimension(220, 44));
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.DIVIDER),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)));

        JLabel javaVer = new JLabel("Java " + System.getProperty("java.version").split("\\.")[0]
                + "  ·  Serial · CPU · GPU");
        javaVer.setFont(Theme.FONT_SMALL);
        javaVer.setForeground(Theme.TEXT_MUTED);
        footer.add(javaVer, BorderLayout.CENTER);
        side.add(footer);

        setNavActive("home");
        return side;
    }

    private JLabel navSection(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 20, 4, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(220, 28));
        return lbl;
    }

    private JButton navItem(String card, String icon, String label) {
        JButton btn = new JButton() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean active = card.equals(currentCard);
                if (active) {
                    // Accent left bar
                    g2.setColor(Theme.ACCENT);
                    g2.fillRoundRect(0, 6, 3, getHeight()-12, 3, 3);
                    // Subtle highlight fill
                    g2.setColor(new Color(Theme.ACCENT.getRed(), Theme.ACCENT.getGreen(),
                            Theme.ACCENT.getBlue(), 18));
                    g2.fillRoundRect(6, 2, getWidth()-8, getHeight()-4, 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(Theme.BG_HOVER);
                    g2.fillRoundRect(6, 2, getWidth()-8, getHeight()-4, 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
            @Override public String getText() { return "  " + icon + "  " + label; }
        };
        btn.setFont(new Font("SansSerif", card.equals(currentCard) ? Font.BOLD : Font.PLAIN, 13));
        btn.setForeground(card.equals(currentCard) ? Theme.TEXT_PRI : Theme.TEXT_SEC);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(220, 38));
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showCard(card));
        navButtons.put(card, btn);
        return btn;
    }

    void showCard(String name) {
        currentCard = name;
        cardLayout.show(cardPanel, name);
        setNavActive(name);
    }

    private void setNavActive(String card) {
        navButtons.forEach((key, btn) -> {
            boolean active = key.equals(card);
            btn.setForeground(active ? Theme.TEXT_PRI : Theme.TEXT_SEC);
            btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 13));
            btn.repaint();
        });
    }

    // ── Legacy factory shims (other panels use these) ─────────────────────

    static JLabel sectionTitle(String text) { return Theme.h2(text); }
    static JLabel subLabel(String text)     { return Theme.muted(text); }
    static JButton primaryButton(String t)  { return Theme.primaryBtn(t); }
    static JButton secondaryButton(String t){ return Theme.ghostBtn(t); }
    static JPanel card(LayoutManager lm)    {
        JPanel p = Theme.card(lm);
        p.setBorder(BorderFactory.createCompoundBorder(p.getBorder(),
                BorderFactory.createEmptyBorder(14,14,14,14)));
        return p;
    }
    static JTextArea logArea()              { return Theme.terminal(); }

    public static void launch() {
        SwingUtilities.invokeLater(MainWindow::new);
    }
}
