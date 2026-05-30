package gamelog.ui;

import gamelog.benchmark.BenchmarkResult;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generates benchmark charts using pure Java2D — no external charting library required.
 *
 * The charts intentionally cover the main requirements of the assignment:
 *  - comparison between SerialCPU, ParallelCPU and ParallelGPU;
 *  - different input texts / narrative worlds;
 *  - three timed samples per method;
 *  - different CPU thread counts;
 *  - CSV-based statistical analysis.
 */
public class ChartGenerator {

    private static final int W = 1000;
    private static final int H = 580;

    private static final Color BG = new Color(0x1A1A2E);
    private static final Color GRID = new Color(255, 255, 255, 55);
    private static final Color TEXT = new Color(0xF5F7FA);
    private static final Color TEXT_DIM = new Color(0xB8C1D1);

    private static final Color[] PALETTE = {
            new Color(0x4E79A7), new Color(0xF28E2B), new Color(0xE15759),
            new Color(0x76B7B2), new Color(0x59A14F), new Color(0xEDC948),
            new Color(0xB07AA1), new Color(0xFF9DA7), new Color(0x9C755F)
    };

    public static void generateAll(List<BenchmarkResult> results, String outputDir) throws IOException {
        new File(outputDir).mkdirs();
        if (results == null || results.isEmpty()) {
            throw new IOException("Nenhum resultado encontrado para gerar gráficos.");
        }

        saveChart(chartMedianTime(results),          outputDir + "/chart_median_time.png");
        saveChart(chartThreadImpact(results),        outputDir + "/chart_thread_impact.png");
        saveChart(chartSpeedup(results),             outputDir + "/chart_speedup.png");
        saveChart(chartThroughput(results),          outputDir + "/chart_throughput.png");

        saveChart(chartRunVariation(results),        outputDir + "/chart_run_variation.png");
        saveChart(chartMeanStdDev(results),          outputDir + "/chart_mean_stddev.png");
        saveChart(chartSizeImpact(results),          outputDir + "/chart_size_impact.png");
        saveChart(chartNormalized100k(results),      outputDir + "/chart_normalized_100k.png");
        saveChart(chartCpuSpeedupThreads(results),   outputDir + "/chart_cpu_speedup_threads.png");
        saveChart(chartParallelEfficiency(results),  outputDir + "/chart_parallel_efficiency.png");
        saveChart(chartCpuVsGpu(results),            outputDir + "/chart_cpu_vs_gpu.png");
        saveChart(chartRankingByWorld(results),      outputDir + "/chart_ranking_world.png");
        saveChart(chartWordDensity(results),         outputDir + "/chart_word_density.png");

        saveChart(chartBestByFamily(results),        outputDir + "/chart_best_by_family.png");
        saveChart(chartCpuStrategyBranch(results),   outputDir + "/chart_cpu_strategy_branch.png");
        saveChart(chartVirtualThreadsBranch(results),outputDir + "/chart_virtual_threads_branch.png");
        saveChart(chartGpuBranch(results),           outputDir + "/chart_gpu_branch.png");

        System.out.println("\n  Charts saved to: " + new File(outputDir).getAbsolutePath());
    }

    // 1 — Median time per method and sample
    private static BufferedImage chartMedianTime(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) row.put(m, medianTime(results, f, m));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Tempo mediano por método", "Amostra / Mundo Narrativo", "Tempo (ms)", data, methods);
    }

    // 2 — Thread impact on ParallelCPU
    private static BufferedImage chartThreadImpact(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> cpuMethods = orderedCpuMethods(results, true);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : cpuMethods) row.put(m, medianTime(results, f, m));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Impacto do número de threads na CPU", "Amostra / Mundo Narrativo", "Tempo (ms)", data, cpuMethods);
    }

    // 3 — Speedup
    private static BufferedImage chartSpeedup(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            double serialMs = Math.max(0.0001, medianTime(results, f, "SerialCPU"));
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                double t = medianTime(results, f, m);
                row.put(m, t > 0 ? serialMs / t : 0.0);
            }
            data.put(shortName(f), row);
        }
        return groupedBarChart("Speedup em relação ao SerialCPU", "Amostra / Mundo Narrativo", "Speedup (vezes)", data, methods);
    }

    // 4 — Throughput
    private static BufferedImage chartThroughput(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            long totalWords = totalWords(results, f);
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                double t = Math.max(0.0001, medianTime(results, f, m));
                row.put(m, totalWords / t);
            }
            data.put(shortName(f), row);
        }
        return groupedBarChart("Throughput — palavras processadas por milissegundo", "Amostra / Mundo Narrativo", "Palavras/ms", data, methods);
    }

    // 5 — Run variation: average time per run and method across all samples
    private static BufferedImage chartRunVariation(List<BenchmarkResult> results) {
        List<String> methods = orderedMethods(results);
        List<String> runs = results.stream().map(r -> "Run " + r.run).distinct().sorted().collect(Collectors.toList());
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String runLabel : runs) {
            int run = Integer.parseInt(runLabel.replace("Run ", ""));
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                row.put(m, average(results.stream()
                        .filter(r -> r.run == run && r.method.equals(m))
                        .map(r -> r.timeMs).collect(Collectors.toList())));
            }
            data.put(runLabel, row);
        }
        return groupedBarChart("Variação entre as 3 execuções", "Execução", "Tempo médio (ms)", data, methods);
    }

    // 6 — Mean with standard deviation per method and sample
    private static BufferedImage chartMeanStdDev(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results);
        Map<String, Map<String, Double>> means = new LinkedHashMap<>();
        Map<String, Map<String, Double>> errors = new LinkedHashMap<>();

        for (String f : files) {
            Map<String, Double> meanRow = new LinkedHashMap<>();
            Map<String, Double> errRow = new LinkedHashMap<>();
            for (String m : methods) {
                List<Double> values = times(results, f, m);
                meanRow.put(m, average(values));
                errRow.put(m, stddev(values));
            }
            means.put(shortName(f), meanRow);
            errors.put(shortName(f), errRow);
        }
        return groupedBarChart("Tempo médio com desvio padrão", "Amostra / Mundo Narrativo", "Tempo (ms)", means, methods, errors);
    }

    // 7 — Size impact: x = total words, y = median time
    private static BufferedImage chartSizeImpact(List<BenchmarkResult> results) {
        List<String> methods = orderedMethods(results);
        Map<String, List<PointValue>> series = new LinkedHashMap<>();
        for (String m : methods) {
            List<PointValue> points = new ArrayList<>();
            for (String f : orderedFiles(results)) {
                points.add(new PointValue(totalWords(results, f), medianTime(results, f, m), shortName(f)));
            }
            series.put(m, points);
        }
        return lineChart("Impacto do tamanho do texto no tempo", "Total de palavras", "Tempo mediano (ms)", series, false);
    }

    // 8 — Normalized time per 100k words
    private static BufferedImage chartNormalized100k(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            long total = Math.max(1, totalWords(results, f));
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                row.put(m, medianTime(results, f, m) / total * 100_000.0);
            }
            data.put(shortName(f), row);
        }
        return groupedBarChart("Tempo normalizado por 100 mil palavras", "Amostra / Mundo Narrativo", "ms / 100 mil palavras", data, methods);
    }

    // 9 — CPU speedup by number of threads
    private static BufferedImage chartCpuSpeedupThreads(List<BenchmarkResult> results) {
        Map<String, List<PointValue>> series = new LinkedHashMap<>();
        for (String f : orderedFiles(results)) {
            double serial = Math.max(0.0001, medianTime(results, f, "SerialCPU"));
            List<PointValue> points = results.stream()
                    .filter(r -> r.file.equals(f) && r.method.startsWith("ParallelCPU"))
                    .map(r -> r.threads)
                    .distinct()
                    .sorted()
                    .map(t -> new PointValue(t, serial / Math.max(0.0001, medianTimeByThreads(results, f, t)), t + "t"))
                    .collect(Collectors.toList());
            series.put(shortName(f), points);
        }
        return lineChart("Speedup da CPU paralela por número de threads", "Threads", "Speedup (vezes)", series, true);
    }

    // 10 — Parallel efficiency by threads
    private static BufferedImage chartParallelEfficiency(List<BenchmarkResult> results) {
        Map<String, List<PointValue>> series = new LinkedHashMap<>();
        for (String f : orderedFiles(results)) {
            double serial = Math.max(0.0001, medianTime(results, f, "SerialCPU"));
            List<PointValue> points = results.stream()
                    .filter(r -> r.file.equals(f) && r.method.startsWith("ParallelCPU"))
                    .map(r -> r.threads)
                    .distinct()
                    .sorted()
                    .map(t -> {
                        double speedup = serial / Math.max(0.0001, medianTimeByThreads(results, f, t));
                        return new PointValue(t, speedup / Math.max(1, t), t + "t");
                    })
                    .collect(Collectors.toList());
            series.put(shortName(f), points);
        }
        return lineChart("Eficiência paralela da CPU", "Threads", "Eficiência = speedup / threads", series, true);
    }

    // 11 — Serial vs best ParallelCPU vs GPU
    private static BufferedImage chartCpuVsGpu(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> series = List.of("SerialCPU", "Melhor ParallelCPU", gpuLabel(results));
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("SerialCPU", medianTime(results, f, "SerialCPU"));
            row.put("Melhor ParallelCPU", bestCpuStrategyMedian(results, f));
            row.put(gpuLabel(results), medianTime(results, f, gpuLabel(results)));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Comparação: SerialCPU vs melhor CPU paralela vs GPU", "Amostra / Mundo Narrativo", "Tempo mediano (ms)", data, series);
    }

    // 12 — Ranking by narrative world: winner only
    private static BufferedImage chartRankingByWorld(List<BenchmarkResult> results) {
        List<RankingRow> rows = new ArrayList<>();
        for (String f : orderedFiles(results)) {
            String winner = null;
            double best = Double.MAX_VALUE;
            for (String m : orderedMethods(results)) {
                double t = medianTime(results, f, m);
                if (t > 0 && t < best) {
                    best = t;
                    winner = m;
                }
            }
            rows.add(new RankingRow(shortName(f), winner == null ? "-" : winner, best == Double.MAX_VALUE ? 0 : best));
        }
        return rankingChart("Ranking dos algoritmos por mundo narrativo", rows);
    }

    // 13 — Density of searched word per 10k words
    private static BufferedImage chartWordDensity(List<BenchmarkResult> results) {
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        List<String> series = List.of("densidade");
        for (String f : orderedFiles(results)) {
            BenchmarkResult r = results.stream().filter(x -> x.file.equals(f) && x.method.equals("SerialCPU")).findFirst()
                    .orElse(results.stream().filter(x -> x.file.equals(f)).findFirst().orElse(null));
            if (r == null) continue;
            double density = r.totalWords > 0 ? (r.occurrences / (double) r.totalWords) * 10_000.0 : 0;
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("densidade", density);
            data.put(shortName(f) + " (" + r.wordSearched + ")", row);
        }
        return groupedBarChart("Densidade da palavra-tema por obra", "Obra / Palavra", "Ocorrências a cada 10 mil palavras", data, series);
    }

    // 14 — Best strategy of each family per text
    private static BufferedImage chartBestByFamily(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> families = orderedFamilies(results);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String fam : families) {
                row.put(familyLabel(fam), bestMedianByFamily(results, f, fam));
            }
            data.put(shortName(f), row);
        }
        List<String> labels = families.stream().map(ChartGenerator::familyLabel).collect(Collectors.toList());
        return groupedBarChart("Melhor desempenho por família de estratégia", "Amostra / Mundo Narrativo", "Melhor tempo mediano (ms)", data, labels);
    }

    // 15 — CPU parallel branch: manual threads, fork/join and parallel stream
    private static BufferedImage chartCpuStrategyBranch(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results).stream()
                .filter(m -> m.startsWith("ParallelCPU") || m.startsWith("ForkJoin") || m.startsWith("ParallelStream"))
                .collect(Collectors.toList());
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) row.put(m, medianTime(results, f, m));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Vertente CPU paralela — estratégias", "Amostra / Mundo Narrativo", "Tempo mediano (ms)", data, methods);
    }

    // 16 — Virtual threads branch: compare chunk granularities
    private static BufferedImage chartVirtualThreadsBranch(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        List<String> methods = orderedMethods(results).stream()
                .filter(m -> m.startsWith("VirtualThreads"))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            Map<String, Map<String, Double>> empty = new LinkedHashMap<>();
            empty.put("Sem dados", Map.of("VirtualThreads", 0.0));
            return groupedBarChart("Vertente Virtual Threads", "Amostra", "Tempo mediano (ms)", empty, List.of("VirtualThreads"));
        }
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) row.put(m, medianTime(results, f, m));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Vertente Virtual Threads — granularidade", "Amostra / Mundo Narrativo", "Tempo mediano (ms)", data, methods);
    }

    // 17 — GPU branch compared with serial, best CPU and best virtual-thread strategy
    private static BufferedImage chartGpuBranch(List<BenchmarkResult> results) {
        List<String> files = orderedFiles(results);
        String gpu = gpuLabel(results);
        List<String> series = List.of("SerialCPU", "Melhor CPU paralela", "Melhor VirtualThreads", gpu);
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("SerialCPU", medianTime(results, f, "SerialCPU"));
            row.put("Melhor CPU paralela", bestCpuStrategyMedian(results, f));
            row.put("Melhor VirtualThreads", bestMedianByFamily(results, f, "VIRTUAL_THREADS"));
            row.put(gpu, medianTime(results, f, gpu));
            data.put(shortName(f), row);
        }
        return groupedBarChart("Vertente GPU/OpenCL vs melhores estratégias", "Amostra / Mundo Narrativo", "Tempo mediano (ms)", data, series);
    }

    // ── Drawing helpers ────────────────────────────────────────────────────

    private static BufferedImage groupedBarChart(String title, String xLabel, String yLabel,
                                                 Map<String, Map<String, Double>> data,
                                                 List<String> series) {
        return groupedBarChart(title, xLabel, yLabel, data, series, null);
    }

    private static BufferedImage groupedBarChart(String title, String xLabel, String yLabel,
                                                 Map<String, Map<String, Double>> data,
                                                 List<String> series,
                                                 Map<String, Map<String, Double>> errorData) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(img, title);

        int marginL = 95, marginR = 35, marginT = 70, marginB = 130;
        int plotW = W - marginL - marginR;
        int plotH = H - marginT - marginB;

        double maxVal = data.values().stream().flatMap(m -> m.entrySet().stream())
                .mapToDouble(e -> e.getValue() + (errorData == null ? 0.0 : errorData.getOrDefault(findGroup(data, e), Collections.emptyMap()).getOrDefault(e.getKey(), 0.0)))
                .max().orElse(1.0);
        maxVal = Math.max(1e-9, maxVal * 1.18);

        drawGrid(g, marginL, marginT, plotW, plotH, maxVal);

        List<String> groups = new ArrayList<>(data.keySet());
        int nGroups = Math.max(1, groups.size());
        int nSeries = Math.max(1, series.size());
        int groupW = plotW / nGroups;
        int barW = Math.max(3, Math.min(28, (groupW - 14) / nSeries));
        int groupInnerW = barW * nSeries;

        for (int gi = 0; gi < groups.size(); gi++) {
            String group = groups.get(gi);
            Map<String, Double> row = data.getOrDefault(group, Collections.emptyMap());
            Map<String, Double> errRow = errorData == null ? Collections.emptyMap() : errorData.getOrDefault(group, Collections.emptyMap());
            int groupStart = marginL + gi * groupW + (groupW - groupInnerW) / 2;

            for (int si = 0; si < series.size(); si++) {
                String ser = series.get(si);
                double val = row.getOrDefault(ser, 0.0);
                int barH = (int) Math.round(val / maxVal * plotH);
                int x = groupStart + si * barW;
                int y = marginT + plotH - barH;
                Color c = PALETTE[si % PALETTE.length];
                g.setColor(c);
                g.fillRoundRect(x, y, Math.max(1, barW - 3), barH, 5, 5);
                g.setColor(c.darker());
                g.drawRoundRect(x, y, Math.max(1, barW - 3), barH, 5, 5);

                if (errorData != null && errRow.containsKey(ser)) {
                    double err = errRow.get(ser);
                    int errH = (int) Math.round(err / maxVal * plotH);
                    int cx = x + Math.max(1, barW - 3) / 2;
                    int errTop = Math.max(marginT, y - errH);
                    g.setColor(TEXT);
                    g.drawLine(cx, errTop, cx, y);
                    g.drawLine(cx - 4, errTop, cx + 4, errTop);
                    g.drawLine(cx - 4, y, cx + 4, y);
                }

                if (barH > 22 && barW >= 18) {
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    String v = compact(val);
                    int vw = g.getFontMetrics().stringWidth(v);
                    if (vw < barW - 3) g.drawString(v, x + (barW - 3 - vw) / 2, y + 13);
                }
            }

            drawCenteredPossiblyMultiline(g, group, marginL + gi * groupW + groupW / 2,
                    marginT + plotH + 20, groupW - 6);
        }

        drawAxesAndLabels(g, marginL, marginT, plotW, plotH, xLabel, yLabel, marginB);
        drawLegend(g, series, marginL, H - 62);
        g.dispose();
        return img;
    }

    private static BufferedImage lineChart(String title, String xLabel, String yLabel,
                                           Map<String, List<PointValue>> series, boolean integerX) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(img, title);

        int marginL = 95, marginR = 45, marginT = 70, marginB = 120;
        int plotW = W - marginL - marginR;
        int plotH = H - marginT - marginB;

        double minX = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.x).min().orElse(0);
        double maxX = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.x).max().orElse(1);
        double maxY = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.y).max().orElse(1);
        if (Math.abs(maxX - minX) < 1e-9) maxX = minX + 1;
        maxY = Math.max(1e-9, maxY * 1.18);

        drawGrid(g, marginL, marginT, plotW, plotH, maxY);

        int si = 0;
        for (Map.Entry<String, List<PointValue>> entry : series.entrySet()) {
            List<PointValue> points = new ArrayList<>(entry.getValue());
            points.sort(Comparator.comparingDouble(p -> p.x));
            Color c = PALETTE[si % PALETTE.length];
            g.setColor(c);
            g.setStroke(new BasicStroke(2.2f));
            int lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
            for (PointValue p : points) {
                int x = marginL + (int) Math.round((p.x - minX) / (maxX - minX) * plotW);
                int y = marginT + plotH - (int) Math.round(p.y / maxY * plotH);
                if (lastX != Integer.MIN_VALUE) g.drawLine(lastX, lastY, x, y);
                g.fillOval(x - 5, y - 5, 10, 10);
                lastX = x; lastY = y;
            }
            si++;
        }

        // x tick labels
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(TEXT_DIM);
        if (integerX) {
            Set<Integer> ticks = series.values().stream().flatMap(List::stream).map(p -> (int) Math.round(p.x))
                    .collect(Collectors.toCollection(TreeSet::new));
            for (int t : ticks) {
                int x = marginL + (int) Math.round((t - minX) / (maxX - minX) * plotW);
                String lbl = String.valueOf(t);
                g.drawString(lbl, x - g.getFontMetrics().stringWidth(lbl) / 2, marginT + plotH + 18);
            }
        } else {
            for (List<PointValue> points : series.values()) {
                for (PointValue p : points) {
                    int x = marginL + (int) Math.round((p.x - minX) / (maxX - minX) * plotW);
                    g.drawString(p.label, x - g.getFontMetrics().stringWidth(p.label) / 2, marginT + plotH + 18);
                }
                break;
            }
        }

        drawAxesAndLabels(g, marginL, marginT, plotW, plotH, xLabel, yLabel, marginB);
        drawLegend(g, new ArrayList<>(series.keySet()), marginL, H - 62);
        g.dispose();
        return img;
    }

    private static BufferedImage rankingChart(String title, List<RankingRow> rows) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = begin(img, title);

        int x0 = 130, y0 = 110, barH = 52, gap = 32;
        double max = rows.stream().mapToDouble(r -> r.time).max().orElse(1) * 1.15;
        int plotW = W - x0 - 90;

        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        g.setColor(TEXT_DIM);
        g.drawString("Menor tempo mediano vence cada mundo narrativo", x0, 74);

        for (int i = 0; i < rows.size(); i++) {
            RankingRow r = rows.get(i);
            int y = y0 + i * (barH + gap);
            int w = (int) Math.round(r.time / max * plotW);
            Color c = PALETTE[i % PALETTE.length];
            g.setColor(TEXT_DIM);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString(r.world, 30, y + 30);
            g.setColor(c);
            g.fillRoundRect(x0, y, Math.max(2, w), barH, 12, 12);
            g.setColor(c.darker());
            g.drawRoundRect(x0, y, Math.max(2, w), barH, 12, 12);
            g.setColor(TEXT);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            String label = String.format(Locale.US, "1º %s — %.4f ms", r.method, r.time);
            g.drawString(label, x0 + 12, y + 31);
        }

        g.dispose();
        return img;
    }

    private static Graphics2D begin(BufferedImage img, String title) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(BG);
        g.fillRect(0, 0, W, H);
        g.setColor(TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (W - fm.stringWidth(title)) / 2, 38);
        return g;
    }

    private static void drawGrid(Graphics2D g, int x, int y, int w, int h, double maxVal) {
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        int ticks = 5;
        for (int i = 0; i <= ticks; i++) {
            int yy = y + h - (int) Math.round((double) i / ticks * h);
            g.setColor(GRID);
            g.drawLine(x, yy, x + w, yy);
            g.setColor(TEXT_DIM);
            String lbl = compact(maxVal * i / ticks);
            g.drawString(lbl, x - g.getFontMetrics().stringWidth(lbl) - 8, yy + 4);
        }
    }

    private static void drawAxesAndLabels(Graphics2D g, int x, int y, int w, int h, String xLabel, String yLabel, int marginB) {
        g.setColor(TEXT_DIM);
        g.drawLine(x, y, x, y + h);
        g.drawLine(x, y + h, x + w, y + h);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.setColor(TEXT);
        g.drawString(xLabel, x + w / 2 - g.getFontMetrics().stringWidth(xLabel) / 2, H - marginB + 42);
        Graphics2D gr = (Graphics2D) g.create();
        gr.rotate(-Math.PI / 2);
        gr.setFont(new Font("SansSerif", Font.BOLD, 12));
        gr.setColor(TEXT);
        gr.drawString(yLabel, -(y + h / 2 + gr.getFontMetrics().stringWidth(yLabel) / 2), 19);
        gr.dispose();
    }

    private static void drawLegend(Graphics2D g, List<String> series, int x, int y) {
        int legendX = x;
        int legendY = y;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (int si = 0; si < series.size(); si++) {
            String label = series.get(si);
            Color c = PALETTE[si % PALETTE.length];
            g.setColor(c);
            g.fillRoundRect(legendX, legendY - 11, 14, 12, 3, 3);
            g.setColor(TEXT);
            g.drawString(label, legendX + 19, legendY);
            legendX += g.getFontMetrics().stringWidth(label) + 42;
            if (legendX > W - 170) {
                legendX = x;
                legendY += 20;
            }
        }
    }

    private static void drawCenteredPossiblyMultiline(Graphics2D g, String text, int centerX, int y, int maxW) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.setColor(TEXT);
        String[] words = text.split("(?=[A-Z])|[-_ ]");
        String line = "";
        List<String> lines = new ArrayList<>();
        for (String word : words) {
            if (word == null || word.isBlank()) continue;
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (g.getFontMetrics().stringWidth(candidate) > maxW && !line.isEmpty()) {
                lines.add(line);
                line = word;
            } else {
                line = candidate;
            }
        }
        if (!line.isEmpty()) lines.add(line);
        if (lines.isEmpty()) lines.add(text);
        for (int i = 0; i < Math.min(3, lines.size()); i++) {
            String l = lines.get(i);
            g.drawString(l, centerX - g.getFontMetrics().stringWidth(l) / 2, y + i * 13);
        }
    }

    // ── Data helpers ───────────────────────────────────────────────────────

    private static double medianTime(List<BenchmarkResult> results, String file, String method) {
        return median(times(results, file, method));
    }

    private static double medianTimeByThreads(List<BenchmarkResult> results, String file, int threads) {
        return median(results.stream()
                .filter(r -> r.file.equals(file) && r.method.startsWith("ParallelCPU") && r.threads == threads)
                .map(r -> r.timeMs).sorted().collect(Collectors.toList()));
    }

    private static List<Double> times(List<BenchmarkResult> results, String file, String method) {
        return results.stream()
                .filter(r -> r.file.equals(file) && r.method.equals(method))
                .map(r -> r.timeMs).sorted().collect(Collectors.toList());
    }

    private static double median(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        int n = sorted.size();
        return n % 2 == 1 ? sorted.get(n / 2) : (sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0;
    }

    private static double average(List<Double> values) {
        if (values == null || values.isEmpty()) return 0.0;
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double stddev(List<Double> values) {
        if (values == null || values.size() < 2) return 0.0;
        double avg = average(values);
        double var = values.stream().mapToDouble(v -> (v - avg) * (v - avg)).sum() / (values.size() - 1);
        return Math.sqrt(var);
    }

    private static long totalWords(List<BenchmarkResult> results, String file) {
        return results.stream().filter(r -> r.file.equals(file)).mapToLong(r -> r.totalWords).findFirst().orElse(1);
    }

    private static double bestParallelCpuMedian(List<BenchmarkResult> results, String file) {
        return results.stream()
                .filter(r -> r.file.equals(file) && r.method.startsWith("ParallelCPU"))
                .map(r -> r.method).distinct()
                .mapToDouble(m -> medianTime(results, file, m))
                .filter(v -> v > 0)
                .min().orElse(0.0);
    }

    private static List<String> orderedFiles(List<BenchmarkResult> results) {
        List<String> order = List.of("donquixote", "dracula", "mobydick", "moby");
        return results.stream().map(r -> r.file).distinct()
                .sorted(Comparator.comparingInt(f -> {
                    String normalized = f.toLowerCase().replaceAll("[^a-z]", "");
                    for (int i = 0; i < order.size(); i++) if (normalized.contains(order.get(i))) return i;
                    return 99;
                }))
                .collect(Collectors.toList());
    }

    private static List<String> orderedMethods(List<BenchmarkResult> results) {
        return results.stream().map(r -> r.method).distinct()
                .sorted(Comparator.comparingInt(ChartGenerator::methodOrder))
                .collect(Collectors.toList());
    }

    private static List<String> orderedCpuMethods(List<BenchmarkResult> results, boolean includeSerial) {
        return results.stream().map(r -> r.method)
                .filter(m -> (includeSerial && m.equals("SerialCPU")) || m.startsWith("ParallelCPU"))
                .distinct().sorted(Comparator.comparingInt(ChartGenerator::methodOrder))
                .collect(Collectors.toList());
    }

    private static int methodOrder(String method) {
        if (method.equals("SerialCPU")) return 0;
        if (method.equals("SerialStream")) return 1;
        if (method.startsWith("ParallelCPU")) return 10 + extractThreadCount(method);
        if (method.startsWith("ForkJoin")) return 80;
        if (method.startsWith("ParallelStream")) return 90;
        if (method.startsWith("VirtualThreads")) return 200 + extractChunkCount(method);
        if (method.startsWith("ParallelGPU")) return 10_000;
        return 20_000;
    }

    private static int extractChunkCount(String method) {
        try {
            int dash = method.indexOf('-');
            int c = method.indexOf("chunks", dash);
            if (dash >= 0 && c > dash) return Integer.parseInt(method.substring(dash + 1, c));
        } catch (Exception ignored) {}
        return 999;
    }

    private static List<String> orderedFamilies(List<BenchmarkResult> results) {
        return results.stream().map(r -> r.family).distinct()
                .sorted(Comparator.comparingInt(ChartGenerator::familyOrder))
                .collect(Collectors.toList());
    }

    private static int familyOrder(String family) {
        return switch (family) {
            case "SERIAL" -> 0;
            case "PARALLEL_CPU" -> 10;
            case "PARALLEL_STREAM" -> 20;
            case "VIRTUAL_THREADS" -> 30;
            case "GPU_OPENCL" -> 40;
            default -> 999;
        };
    }

    private static String familyLabel(String family) {
        return switch (family) {
            case "SERIAL" -> "Serial";
            case "PARALLEL_CPU" -> "CPU paralela";
            case "PARALLEL_STREAM" -> "ParallelStream";
            case "VIRTUAL_THREADS" -> "Virtual Threads";
            case "GPU_OPENCL" -> "GPU/OpenCL";
            default -> family;
        };
    }

    private static double bestMedianByFamily(List<BenchmarkResult> results, String file, String family) {
        return results.stream()
                .filter(r -> r.file.equals(file) && r.family.equals(family))
                .map(r -> r.method).distinct()
                .mapToDouble(m -> medianTime(results, file, m))
                .filter(v -> v > 0)
                .min().orElse(0.0);
    }

    private static double bestCpuStrategyMedian(List<BenchmarkResult> results, String file) {
        return results.stream()
                .filter(r -> r.file.equals(file) && (r.family.equals("PARALLEL_CPU") || r.family.equals("PARALLEL_STREAM")))
                .map(r -> r.method).distinct()
                .mapToDouble(m -> medianTime(results, file, m))
                .filter(v -> v > 0)
                .min().orElse(0.0);
    }

    private static int extractThreadCount(String method) {
        try {
            int dash = method.indexOf('-');
            int t = method.indexOf('t', dash);
            if (dash >= 0 && t > dash) return Integer.parseInt(method.substring(dash + 1, t));
        } catch (Exception ignored) {}
        return 999;
    }

    private static String gpuLabel(List<BenchmarkResult> results) {
        return results.stream().map(r -> r.method).filter(m -> m.startsWith("ParallelGPU"))
                .findFirst().orElse("ParallelGPU");
    }

    private static String shortName(String fileName) {
        String n = fileName.replace(".txt", "");
        if (n.toLowerCase().contains("donquixote")) return "Don Quixote";
        if (n.toLowerCase().contains("dracula")) return "Dracula";
        if (n.toLowerCase().contains("mobydick")) return "Moby Dick";
        return n.replace("_", " ");
    }

    private static String compact(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000) return String.format(Locale.US, "%.1fM", value / 1_000_000.0);
        if (abs >= 10_000) return String.format(Locale.US, "%.0fK", value / 1_000.0);
        if (abs >= 1_000) return String.format(Locale.US, "%.1fK", value / 1_000.0);
        if (abs >= 100) return String.format(Locale.US, "%.0f", value);
        if (abs >= 10) return String.format(Locale.US, "%.1f", value);
        return String.format(Locale.US, "%.2f", value);
    }

    private static String findGroup(Map<String, Map<String, Double>> data, Map.Entry<String, Double> entry) {
        // This method is only a defensive helper for max-value calculation and is not used for drawing.
        for (Map.Entry<String, Map<String, Double>> e : data.entrySet()) {
            if (e.getValue().containsKey(entry.getKey()) && Objects.equals(e.getValue().get(entry.getKey()), entry.getValue())) {
                return e.getKey();
            }
        }
        return "";
    }

    private static void saveChart(BufferedImage img, String path) throws IOException {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null) parent.mkdirs();
        ImageIO.write(img, "PNG", f);
        System.out.printf("  Chart -> %s%n", f.getAbsolutePath());
    }

    private record PointValue(double x, double y, String label) {}
    private record RankingRow(String world, String method, double time) {}
}
