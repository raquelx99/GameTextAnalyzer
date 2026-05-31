package gamelog.ui;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

/**
 * Sistema de design centralizado do GameText Analyzer.
 * Todas as cores, fontes, espaçamentos e fábricas de componentes ficam aqui.
 */
public final class Theme {

    private Theme() {}

    // ── Tokens de cor ────────────────────────────────────────────────────────
    public static final Color BG_BASE    = new Color(0x0F0F17);   // fundo mais escuro
    public static final Color BG_SURFACE = new Color(0x171724);   // painéis, sidebar
    public static final Color BG_CARD    = new Color(0x1E1E30);   // cards, inputs
    public static final Color BG_HOVER   = new Color(0x252538);   // estado hover
    public static final Color BG_ACTIVE  = new Color(0x2A2A42);   // estado selecionado

    public static final Color ACCENT     = new Color(0x6C8EF5);   // azul primário
    public static final Color ACCENT_DIM = new Color(0x4A6ACF);   // azul mais escuro
    public static final Color GREEN      = new Color(0x4EC994);   // sucesso
    public static final Color ORANGE     = new Color(0xF5A623);   // aviso/gerar
    public static final Color RED        = new Color(0xE5534B);   // erro/perigo
    public static final Color PURPLE     = new Color(0xB07BF5);   // GPU

    public static final Color TEXT_PRI   = new Color(0xEDEDF5);   // texto primário
    public static final Color TEXT_SEC   = new Color(0x9898B8);   // texto secundário
    public static final Color TEXT_MUTED = new Color(0x5A5A7A);   // texto opaco/desativado

    public static final Color DIVIDER    = new Color(0x252535);   // bordas/divisores
    public static final Color DIVIDER_LT = new Color(0x32324A);   // borda mais clara

    // ── Cores dos métodos (consistentes em todos os painéis) ─────────────────────
    public static Color methodColor(String method) {
        if (method == null) return TEXT_SEC;
        if (method.equals("SerialCPU"))                return new Color(0x6C8EF5);
        if (method.startsWith("ParallelCPU-2"))        return new Color(0x4EC994);
        if (method.startsWith("ParallelCPU-4"))        return new Color(0xF5A623);
        if (method.startsWith("ParallelCPU-8"))        return new Color(0xF5734B);
        if (method.startsWith("ParallelGPU"))          return new Color(0xB07BF5);
        return TEXT_SEC;
    }

    // ── Tipografia ───────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD,  22);
    public static final Font FONT_H2      = new Font("SansSerif", Font.BOLD,  16);
    public static final Font FONT_H3      = new Font("SansSerif", Font.BOLD,  13);
    public static final Font FONT_BODY    = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font(Font.MONOSPACED, Font.PLAIN, 12);
    public static final Font FONT_MONO_SM = new Font(Font.MONOSPACED, Font.PLAIN, 11);

    // ── Espaçamento ──────────────────────────────────────────────────────────
    public static final int PAD_SM  = 8;
    public static final int PAD_MD  = 16;
    public static final int PAD_LG  = 24;
    public static final int PAD_XL  = 32;

    // ── Inicialização do UIManager ──────────────────────────────────────────────────
    public static void apply() {
        UIManager.put("Panel.background",            BG_BASE);
        UIManager.put("Label.foreground",            TEXT_PRI);
        UIManager.put("Button.foreground",           Color.WHITE);
        UIManager.put("TextField.background",        BG_CARD);
        UIManager.put("TextField.foreground",        TEXT_PRI);
        UIManager.put("TextField.caretForeground",   ACCENT);
        UIManager.put("TextArea.background",         BG_CARD);
        UIManager.put("TextArea.foreground",         TEXT_PRI);
        UIManager.put("ComboBox.background",         BG_CARD);
        UIManager.put("ComboBox.foreground",         TEXT_PRI);
        UIManager.put("CheckBox.background",         BG_SURFACE);
        UIManager.put("CheckBox.foreground",         TEXT_PRI);
        UIManager.put("RadioButton.background",      BG_SURFACE);
        UIManager.put("RadioButton.foreground",      TEXT_PRI);
        UIManager.put("ProgressBar.background",      BG_SURFACE);
        UIManager.put("ProgressBar.foreground",      GREEN);
        UIManager.put("Spinner.background",          BG_CARD);
        UIManager.put("PopupMenu.background",        BG_SURFACE);
        UIManager.put("List.background",             BG_SURFACE);
        UIManager.put("List.foreground",             TEXT_PRI);
        UIManager.put("List.selectionBackground",    ACCENT);
        UIManager.put("SplitPane.background",        BG_BASE);
        UIManager.put("SplitPaneDivider.background", DIVIDER);
        UIManager.put("TabbedPane.background",       BG_BASE);
        UIManager.put("TabbedPane.foreground",       TEXT_SEC);
        UIManager.put("TabbedPane.selected",         BG_CARD);
        UIManager.put("TabbedPane.selectedForeground", TEXT_PRI);
        UIManager.put("Table.background",            BG_CARD);
        UIManager.put("Table.foreground",            TEXT_PRI);
        UIManager.put("Table.selectionBackground",   ACCENT_DIM);
        UIManager.put("Table.selectionForeground",   Color.WHITE);
        UIManager.put("Table.gridColor",             DIVIDER);
        UIManager.put("TableHeader.background",      BG_SURFACE);
        UIManager.put("TableHeader.foreground",      TEXT_SEC);
        UIManager.put("ScrollBar.width",             6);
        UIManager.put("ScrollBar.thumbColor",        new Color(0x3A3A5C));
    }

    // ── Fábricas de componentes ──────────────────────────────────────────────

    /** Botão primário plano com cantos arredondados. */
    public static JButton primaryBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? ACCENT_DIM.darker() :
                           getModel().isRollover() ? ACCENT_DIM : ACCENT;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_H3);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(9, 20, 9, 20));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        return btn;
    }

    /** Botão ghost / secundário. */
    public static JButton ghostBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? BG_ACTIVE :
                           getModel().isRollover() ? BG_HOVER  : BG_CARD;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(DIVIDER_LT);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(FONT_H3);
        btn.setForeground(TEXT_PRI);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(false);
        return btn;
    }

    /** Botão de ação perigosa (tom vermelho). */
    public static JButton dangerBtn(String text) {
        JButton btn = primaryBtn(text);
        btn.setForeground(Color.WHITE);
        btn.addMouseListener(new MouseAdapter() {}); // aciona repaint
        // Sobrescreve a pintura usando a paleta vermelha
        btn.putClientProperty("dangerBtn", true);
        return btn;
    }

    /** Painel card estilizado com borda arredondada. */
    public static JPanel card() {
        return new JPanel() {
            { setOpaque(false); setLayout(new BorderLayout()); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(DIVIDER_LT);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 12, 12);
                g2.dispose();
            }
        };
    }

    public static JPanel card(LayoutManager lm) {
        JPanel p = card();
        p.setLayout(lm);
        return p;
    }

    /** Campo de input com borda arredondada. */
    public static JTextField inputField(String placeholder) {
        JTextField tf = new JTextField(placeholder) {
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? ACCENT : DIVIDER_LT);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
            }
        };
        tf.setBackground(BG_CARD);
        tf.setForeground(TEXT_PRI);
        tf.setCaretColor(ACCENT);
        tf.setFont(FONT_BODY);
        tf.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        tf.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return tf;
    }

    /** Combo box estilizado. */
    public static JComboBox<String> comboBox() {
        JComboBox<String> cb = new JComboBox<>();
        cb.setBackground(BG_CARD);
        cb.setForeground(TEXT_PRI);
        cb.setFont(FONT_BODY);
        cb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        return cb;
    }

    /** Rótulo pequeno colorido no estilo badge/pílula. */
    public static JLabel badge(String text, Color bg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bgA = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 30);
                g2.setColor(bgA);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 999, 999);
                g2.setColor(bg);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 999, 999);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(bg);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lbl.setOpaque(false);
        return lbl;
    }

    /** Rótulo de cabeçalho de seção. */
    public static JLabel h2(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H2);
        l.setForeground(TEXT_PRI);
        return l;
    }

    /** Rótulo de subseção. */
    public static JLabel h3(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_H3);
        l.setForeground(TEXT_PRI);
        return l;
    }

    /** Rótulo de corpo em tom opaco. */
    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_BODY);
        l.setForeground(TEXT_SEC);
        return l;
    }

    /** Divisor horizontal fino. */
    public static JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(DIVIDER);
        sep.setBackground(DIVIDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /** Área de texto no estilo terminal/log. */
    public static JTextArea terminal() {
        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setBackground(new Color(0x0C0C14));
        ta.setForeground(new Color(0x7EE8A2));
        ta.setFont(FONT_MONO);
        ta.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        ta.setLineWrap(true);
        ta.setWrapStyleWord(false);
        return ta;
    }

    /** Barra de progresso fina com cor de destaque. */
    public static JProgressBar progressBar() {
        JProgressBar pb = new JProgressBar() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 999, 999);
                if (isIndeterminate() || getValue() > 0) {
                    int w = isIndeterminate() ? getWidth() / 2
                            : (int)((double) getValue() / getMaximum() * getWidth());
                    g2.setColor(GREEN);
                    g2.fillRoundRect(0, 0, Math.max(w, getHeight()), getHeight(), 999, 999);
                }
                g2.dispose();
            }
        };
        pb.setBorderPainted(false);
        pb.setStringPainted(false);
        pb.setPreferredSize(new Dimension(0, 4));
        pb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        pb.setOpaque(false);
        return pb;
    }
}
