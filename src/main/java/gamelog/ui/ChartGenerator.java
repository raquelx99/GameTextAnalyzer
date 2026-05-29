package gamelog.ui;

import gamelog.benchmark.BenchmarkResult;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates benchmark charts using pure Java2D / Swing — no external charting
 * library required.
 *
 * Four charts are produced:
 *   1. Median execution time per method (grouped by log file)
 *   2. Thread-count impact on ParallelCPU
 *   3. Speedup per method and file
 *   4. Events-per-millisecond throughput
 */
public class ChartGenerator {

    private static final int W = 900;
    private static final int H = 520;

    private static final Color[] PALETTE = {
        new Color(0x4E79A7), new Color(0xF28E2B), new Color(0xE15759),
        new Color(0x76B7B2), new Color(0x59A14F), new Color(0xEDC948)
    };

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Generates all four charts and saves them to {@code outputDir}.
     *
     * @param results   flat list of all benchmark results
     * @param outputDir directory where PNG files will be saved
     */
    public static void generateAll(List<BenchmarkResult> results, String outputDir)
            throws IOException {
        new File(outputDir).mkdirs();

        saveChart(chartMedianTime(results),       outputDir + "/chart_median_time.png");
        saveChart(chartThreadImpact(results),     outputDir + "/chart_thread_impact.png");
        saveChart(chartSpeedup(results),          outputDir + "/chart_speedup.png");
        saveChart(chartThroughput(results),       outputDir + "/chart_throughput.png");

        System.out.println("\n  Charts saved to: " + new File(outputDir).getAbsolutePath());
    }

    // ── Chart 1 — Median time per method ────────────────────────────────────

    private static BufferedImage chartMedianTime(List<BenchmarkResult> results) {
        // Group: file → method → median time
        List<String> files   = orderedFiles(results);
        List<String> methods = orderedMethods(results);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                row.put(m, medianTime(results, f, m));
            }
            data.put(shortName(f), row);
        }

        return doubleBarChart("Tempo Médio de Execução por Método", "Amostra / Mundo Narrativo",
                        "Tempo (ms)", data, methods);
    }

    // ── Chart 2 — Thread impact on ParallelCPU ──────────────────────────────

    private static BufferedImage chartThreadImpact(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> cpuMethods = results.stream()
                .map(r -> r.method)
                .filter(m -> m.startsWith("ParallelCPU") || m.equals("SerialCPU"))
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : cpuMethods) {
                row.put(m, medianTime(results, f, m));
            }
            data.put(shortName(f), row);
        }

        return doubleBarChart("Impacto do Número de Threads (CPU)", "Amostra / Mundo Narrativo",
                        "Tempo (ms)", data, cpuMethods);
    }

    // ── Chart 3 — Speedup ───────────────────────────────────────────────────

    private static BufferedImage chartSpeedup(List<BenchmarkResult> results) {
        List<String> files   = orderedFiles(results);
        List<String> methods = orderedMethods(results);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            double serialMs = Math.max(0.0001, medianTime(results, f, "SerialCPU"));
            for (String m : methods) {
                double t = medianTime(results, f, m);
                row.put(m, t > 0 ? serialMs / t : 0.0);
            }
            data.put(shortName(f), row);
        }

        return doubleBarChart("Speedup vs. SerialCPU", "Amostra / Mundo Narrativo",
                              "Speedup (vezes)", data, methods);
    }

    // ── Chart 4 — Throughput ────────────────────────────────────────────────

    private static BufferedImage chartThroughput(List<BenchmarkResult> results) {
        List<String> files   = orderedFiles(results);
        List<String> methods = orderedMethods(results);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            long totalWords = results.stream()
                    .filter(r -> r.file.equals(f))
                    .mapToLong(r -> r.totalWords).findFirst().orElse(1);
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                double t = Math.max(0.0001, medianTime(results, f, m));
                row.put(m, totalWords / t);
            }
            data.put(shortName(f), row);
        }

        return doubleBarChart("Throughput — Palavras por Milissegundo", "Amostra / Mundo Narrativo",
                              "Palavras/ms", data, methods);
    }

    // ── Generic grouped bar-chart helpers ───────────────────────────────────

    private static BufferedImage doubleBarChart(String title, String xLabel, String yLabel,
                                                Map<String, Map<String, Double>> data,
                                                List<String> series) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(0x1A1A2E));
        g.fillRect(0, 0, W, H);

        int marginL = 80, marginR = 30, marginT = 60, marginB = 110;
        int plotW   = W - marginL - marginR;
        int plotH   = H - marginT - marginB;

        // Title
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (W - fm.stringWidth(title)) / 2, 36);

        // Find max value
        double maxVal = data.values().stream()
                .flatMap(m -> m.values().stream())
                .mapToDouble(Double::doubleValue).max().orElse(1);
        maxVal *= 1.15;

        // Y-axis grid
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(new Color(255, 255, 255, 60));
        int yTicks = 5;
        for (int i = 0; i <= yTicks; i++) {
            int y = marginT + plotH - (int) ((double) i / yTicks * plotH);
            g.drawLine(marginL, y, marginL + plotW, y);
            g.setColor(new Color(200, 200, 200));
            String lbl = String.format("%.0f", maxVal * i / yTicks);
            g.drawString(lbl, marginL - fm.stringWidth(lbl) - 4, y + 4);
            g.setColor(new Color(255, 255, 255, 60));
        }

        // Bars
        List<String> groups = new ArrayList<>(data.keySet());
        int nGroups  = groups.size();
        int nSeries  = series.size();
        int groupW   = plotW / Math.max(nGroups, 1);
        int barW     = Math.max(4, (groupW - 10) / Math.max(nSeries, 1));

        for (int gi = 0; gi < nGroups; gi++) {
            String group = groups.get(gi);
            Map<String, Double> row = data.getOrDefault(group, Collections.emptyMap());
            int groupX = marginL + gi * groupW + 5;

            for (int si = 0; si < nSeries; si++) {
                String ser = series.get(si);
                double val = row.getOrDefault(ser, 0.0);
                int barH  = (int) (val / maxVal * plotH);
                int x     = groupX + si * barW;
                int y     = marginT + plotH - barH;

                Color c = PALETTE[si % PALETTE.length];
                g.setColor(c);
                g.fillRoundRect(x, y, barW - 2, barH, 4, 4);
                g.setColor(c.darker());
                g.drawRoundRect(x, y, barW - 2, barH, 4, 4);

                // Value label on bar
                if (barH > 18) {
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    String v = val >= 1000 ? String.format("%.0f", val) : String.format("%.1f", val);
                    int vw = g.getFontMetrics().stringWidth(v);
                    if (vw < barW - 2) g.drawString(v, x + (barW - 2 - vw) / 2, y + 12);
                }
            }

            // Group label
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            int cx = groupX + (nSeries * barW) / 2;
            g.drawString(group, cx - g.getFontMetrics().stringWidth(group) / 2,
                         marginT + plotH + 18);
        }

        // Axes
        g.setColor(new Color(200, 200, 200));
        g.drawLine(marginL, marginT, marginL, marginT + plotH);
        g.drawLine(marginL, marginT + plotH, marginL + plotW, marginT + plotH);

        // Axis labels
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString(xLabel, marginL + plotW / 2 - g.getFontMetrics().stringWidth(xLabel) / 2,
                     H - marginB + 36);

        // Y-label (rotated)
        Graphics2D gr = (Graphics2D) g.create();
        gr.rotate(-Math.PI / 2, 0, 0);
        gr.setFont(new Font("SansSerif", Font.BOLD, 12));
        gr.setColor(Color.WHITE);
        gr.drawString(yLabel, -(marginT + plotH / 2 + gr.getFontMetrics().stringWidth(yLabel) / 2), 16);
        gr.dispose();

        // Legend
        int legendX = marginL;
        int legendY = H - marginB + 50;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int si = 0; si < series.size(); si++) {
            Color c = PALETTE[si % PALETTE.length];
            g.setColor(c);
            g.fillRect(legendX, legendY - 10, 14, 12);
            g.setColor(Color.WHITE);
            g.drawString(series.get(si), legendX + 18, legendY);
            legendX += g.getFontMetrics().stringWidth(series.get(si)) + 36;
            if (legendX > W - 100) { legendX = marginL; legendY += 20; }
        }

        g.dispose();
        return img;
    }

    // ── Utilities ────────────────────────────────────────────────────────────

    private static double medianTime(List<BenchmarkResult> results, String file, String method) {
        List<Double> times = results.stream()
                .filter(r -> r.file.equals(file) && r.method.equals(method))
                .map(r -> r.timeMs)
                .sorted()
                .collect(Collectors.toList());
        if (times.isEmpty()) return 0.0;
        int n = times.size();
        return n % 2 == 1 ? times.get(n / 2) : (times.get(n / 2 - 1) + times.get(n / 2)) / 2.0;
    }

    private static List<String> orderedFiles(List<BenchmarkResult> results) {
        // Tutorial → Exploration → Combat → Bossfight order
        List<String> order = List.of("tutorial", "explor", "combat", "boss");
        return results.stream()
                .map(r -> r.file)
                .distinct()
                .sorted(Comparator.comparingInt(f -> {
                    String lc = f.toLowerCase();
                    for (int i = 0; i < order.size(); i++) if (lc.contains(order.get(i))) return i;
                    return 99;
                }))
                .collect(Collectors.toList());
    }

    private static List<String> orderedMethods(List<BenchmarkResult> results) {
        return results.stream()
                .map(r -> r.method)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    private static String shortName(String fileName) {
        return fileName.replace("_log.txt", "").replace(".txt", "");
    }

    private static void saveChart(BufferedImage img, String path) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", f);
        System.out.printf("  Chart -> %s%n", f.getAbsolutePath());
    }
}
