package gamelog.ui;

import gamelog.benchmark.BenchmarkResult;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * Scrollable table panel that displays benchmark results.
 */
public class ResultTablePanel extends JPanel {

    private static final String[] COLS = {
        "Arquivo", "Tipo", "Família", "Método", "Paral.", "Run",
        "Ocorrências", "Tempo (ms)", "Speedup", "Efic.", "Palavras/ms", "GPU real"
    };

    private final DefaultTableModel model;
    private final JTable table;

    public ResultTablePanel() {
        setLayout(new BorderLayout());
        setBackground(MainWindow.BG_CARD);

        model = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(MainWindow.BG_CARD);
        scroll.setBackground(MainWindow.BG_CARD);
        add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel("  Resultados aparecem após a execução do benchmark.");
        hint.setForeground(MainWindow.TEXT_DIM);
        hint.setFont(new Font("SansSerif", Font.PLAIN, 11));
        hint.setBorder(BorderFactory.createEmptyBorder(6, 4, 6, 0));
        add(hint, BorderLayout.SOUTH);
    }

    public void setResults(List<BenchmarkResult> results) {
        model.setRowCount(0);
        for (BenchmarkResult r : results) {
            model.addRow(new Object[]{
                r.file,
                r.logType,
                r.family,
                r.method,
                r.threads,
                r.run,
                String.format("%,d", r.occurrences),
                String.format(java.util.Locale.US, "%.4f", r.timeMs),
                String.format(java.util.Locale.US, "%.3f", r.speedup),
                String.format(java.util.Locale.US, "%.3f", r.efficiency),
                String.format(java.util.Locale.US, "%.0f", r.wordsPerMs),
                r.realGpu ? "sim" : "não"
            });
        }
    }

    private void styleTable() {
        table.setBackground(MainWindow.BG_CARD);
        table.setForeground(MainWindow.TEXT_MAIN);
        table.setGridColor(MainWindow.BORDER_COL);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(26);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setSelectionBackground(new Color(0x2A2A5A));
        table.setSelectionForeground(Color.WHITE);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setBackground(MainWindow.BG_PANEL);
        header.setForeground(MainWindow.TEXT_DIM);
        header.setFont(new Font("SansSerif", Font.BOLD, 11));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, MainWindow.BORDER_COL));

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                if (!sel) {
                    setBackground(row % 2 == 0 ? MainWindow.BG_CARD : new Color(0x14142A));
                    setForeground(MainWindow.TEXT_MAIN);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                return this;
            }
        });

        int[] widths = {150, 95, 120, 190, 55, 40, 100, 90, 70, 60, 100, 70};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }
}
