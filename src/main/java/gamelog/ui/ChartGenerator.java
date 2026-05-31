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
 * ChartGenerator — 10 gráficos curados, organizados em 4 blocos
 * diretamente alinhados com os requisitos do enunciado.
 *
 * ══════════════════════════════════════════════════════════════════════
 *  BLOCO 1 — COMPARAÇÃO DIRETA ENTRE MÉTODOS (exigência central)
 *   1. chart_01_tempo_mediano        — tempo mediano: 1 barra por método,
 *                                      uma linha por texto. Mostra claramente
 *                                      quem é mais rápido em cada obra.
 *   2. chart_02_speedup_vs_serial    — speedup de cada método relativo ao
 *                                      SerialCPU. Quanto mais rápido que o
 *                                      baseline? Responde diretamente o enunciado.
 *
 *  BLOCO 2 — IMPACTO DO NÚMERO DE NÚCLEOS (exigência explícita)
 *   3. chart_03_threads_tempo        — tempo mediano do ParallelCPU de 1 a N
 *                                      threads: gráfico de linha, uma curva por texto.
 *   4. chart_04_threads_speedup      — speedup do ParallelCPU por nº de threads.
 *                                      Mostra onde o ganho para de crescer.
 *   5. chart_05_amdahl_efficiency    — eficiência paralela (speedup / threads).
 *                                      Revela overhead e lei de Amdahl.
 *
 *  BLOCO 3 — IMPACTO DO TAMANHO DO TEXTO (exigência explícita)
 *   6. chart_06_escala_tamanho       — eixo X = tamanho do texto (palavras),
 *                                      eixo Y = tempo mediano. Uma linha por método.
 *                                      Mostra escalabilidade.
 *   7. chart_07_tempo_normalizado    — tempo / 100 mil palavras. Remove o viés de
 *                                      tamanho: compara a "velocidade" real de cada
 *                                      método independente do volume.
 *
 *  BLOCO 4 — ESTABILIDADE E VARIABILIDADE (3 amostras, exigência explícita)
 *   8. chart_08_tres_execucoes       — tempo das 3 execuções individuais de cada
 *                                      método, por obra. Mostra reprodutibilidade.
 *   9. chart_09_media_desvio         — média ± desvio padrão das 3 execuções.
 *                                      Análise estatística pedida explicitamente.
 *  10. chart_10_gpu_vs_cpu           — GPU vs melhor CPU paralela vs Serial.
 *                                      Gráfico de destaque para a discussão GPU,
 *                                      que é o método mais complexo do trabalho.
 * ══════════════════════════════════════════════════════════════════════
 *
 * Regras de legibilidade aplicadas:
 *  – Máximo 6 séries por gráfico de barras agrupadas.
 *  – Gráficos de linha usados apenas para tendências (threads, tamanho).
 *  – Cada gráfico tem subtítulo que explica como interpretá-lo.
 *  – Cores consistentes: SerialCPU sempre azul, GPU sempre vermelho, etc.
 */
public class ChartGenerator {

    // ── Canvas ─────────────────────────────────────────────────────────────
    private static final int W = 1400;
    private static final int H = 820;

    // ── Design ────────────────────────────────────────────────────────────
    private static final Color BG      = new Color(0x12121E);
    private static final Color BG_PLOT = new Color(0x1A1A2E);
    private static final Color GRID    = new Color(255, 255, 255, 38);
    private static final Color GRID_AX = new Color(255, 255, 255, 90);
    private static final Color TEXT    = new Color(0xEEEEF8);
    private static final Color DIMMED  = new Color(0x9090B0);

    // ── Método → cor consistente ──────────────────────────────────────────
    private static final Color C_SERIAL  = new Color(0x6C8EF5);
    private static final Color C_SSTREAM = new Color(0x8AABFF);
    private static final Color C_CPU2    = new Color(0x4EC994);
    private static final Color C_CPU4    = new Color(0xF5A623);
    private static final Color C_CPU8    = new Color(0xF5734B);
    private static final Color C_CPUN    = new Color(0xE573C8);
    private static final Color C_FORK    = new Color(0xA8D8A8);
    private static final Color C_PSTREAM = new Color(0xEDC948);
    private static final Color C_VT      = new Color(0xB07BF5);
    private static final Color C_GPU     = new Color(0xFF6B6B);

    // ── Entrada pública ───────────────────────────────────────────────────

    public static void generateAll(List<BenchmarkResult> results, String outputDir)
            throws IOException {
        new File(outputDir).mkdirs();
        if (results == null || results.isEmpty())
            throw new IOException("Nenhum resultado para gerar gráficos.");

        // Bloco 1 — Comparação direta
        save(chart01_TempoMediano(results),     outputDir + "/chart_01_tempo_mediano.png");
        save(chart02_SpeedupVsSerial(results),  outputDir + "/chart_02_speedup_vs_serial.png");

        // Bloco 2 — Impacto de núcleos
        save(chart03_ThreadsTempo(results),     outputDir + "/chart_03_threads_tempo.png");
        save(chart04_ThreadsSpeedup(results),   outputDir + "/chart_04_threads_speedup.png");
        save(chart05_EficienciaParalela(results),outputDir + "/chart_05_amdahl_efficiency.png");

        // Bloco 3 — Impacto do tamanho
        save(chart06_EscalaTamanho(results),    outputDir + "/chart_06_escala_tamanho.png");
        save(chart07_TempoNormalizado(results),  outputDir + "/chart_07_tempo_normalizado.png");

        // Bloco 4 — Estabilidade
        save(chart08_TresExecucoes(results),    outputDir + "/chart_08_tres_execucoes.png");
        save(chart09_MediaDesvio(results),      outputDir + "/chart_09_media_desvio.png");
        save(chart10_GpuVsCpu(results),         outputDir + "/chart_10_gpu_vs_cpu.png");
        save(chart11_ChunkGranularity(results), outputDir + "/chart_11_chunks.png");
        save(chart12_ForkJoinThresholds(results), outputDir + "/chart_12_forkjoin_thresholds.png");
        save(chart13_GpuPreparationKernel(results), outputDir + "/chart_13_gpu_prep_kernel.png");

        System.out.println("\n  13 gráficos salvos em: " + new File(outputDir).getAbsolutePath());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BLOCO 1 — Comparação direta entre métodos
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 1. Tempo mediano de cada método por texto.
     * Usa métodos "representativos" (1 por família) para manter ≤ 6 séries.
     * Inclui SerialCPU como referência visual.
     */
    private static BufferedImage chart01_TempoMediano(List<BenchmarkResult> r) {
        List<String> methods = repMethods(r);
        return barChart(
            "1. Tempo mediano de execução por método",
            "Menor valor = melhor desempenho.  SerialCPU é o baseline de comparação.",
            "Texto / Obra literária", "Tempo mediano (ms)",
            buildBarData(r, orderedFiles(r), methods),
            methods, methodPalette(methods));
    }

    /**
     * 2. Speedup de cada método relativo ao SerialCPU.
     * Speedup = tempo_serial / tempo_método. Valor > 1 = mais rápido que serial.
     * Responde diretamente: "quanto cada método ganhou?"
     */
    private static BufferedImage chart02_SpeedupVsSerial(List<BenchmarkResult> r) {
        List<String> methods = repMethods(r);
        List<String> files   = orderedFiles(r);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            double serial = Math.max(0.0001, medianTime(r, f, "SerialCPU"));
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) {
                double t = medianTime(r, f, m);
                row.put(m, t > 0 ? serial / t : 0.0);
            }
            data.put(shortName(f), row);
        }

        return barChart(
            "2. Speedup em relação ao SerialCPU",
            "Speedup = tempo_serial ÷ tempo_método.  Valor 2× = duas vezes mais rápido que serial.",
            "Texto / Obra literária", "Speedup (×)",
            data, methods, methodPalette(methods));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BLOCO 2 — Impacto do número de núcleos (ParallelCPU)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 3. Tempo do ParallelCPU por número de threads — linha por texto.
     * Mostra se mais threads sempre reduzem o tempo.
     */
    private static BufferedImage chart03_ThreadsTempo(List<BenchmarkResult> r) {
        Map<String, List<PointXY>> series = new LinkedHashMap<>();
        List<Color> colors = List.of(C_CPU4, C_VT, C_GPU, C_FORK);
        int ci = 0;
        for (String f : orderedFiles(r)) {
            List<PointXY> pts = new ArrayList<>();
            // Inclui SerialCPU como ponto base (threads=1) para contexto
            double serialT = medianTime(r, f, "SerialCPU");
            if (serialT > 0) pts.add(new PointXY(1, serialT, "Serial\n(ref.)"));

            r.stream().filter(x -> x.file.equals(f) && isThreadOnlyParallelCPU(x.method))
                .map(x -> x.threads).distinct().sorted()
                .forEach(t -> pts.add(new PointXY(t,
                    medianTimeByThreads(r, f, t), t + "t")));

            if (pts.size() > 1) {
                series.put(shortName(f), pts);
            }
        }
        return lineChart(
            "3. Impacto do número de threads — tempo de execução",
            "Linha mais baixa = mais rápido.  Ponto 1 = SerialCPU como referência.",
            "Número de threads", "Tempo mediano (ms)",
            series, true, false, List.of(C_CPU4, C_VT, C_GPU));
    }

    /**
     * 4. Speedup do ParallelCPU por número de threads.
     * Uma linha por texto. Linha pontilhada = speedup ideal (linear).
     * Mostra onde o ganho começa a saturar.
     */
    private static BufferedImage chart04_ThreadsSpeedup(List<BenchmarkResult> r) {
        Map<String, List<PointXY>> series = new LinkedHashMap<>();
        for (String f : orderedFiles(r)) {
            double serial = Math.max(0.0001, medianTime(r, f, "SerialCPU"));
            List<PointXY> pts = r.stream()
                .filter(x -> x.file.equals(f) && isThreadOnlyParallelCPU(x.method))
                .map(x -> x.threads).distinct().sorted()
                .map(t -> new PointXY(t,
                    serial / Math.max(0.0001, medianTimeByThreads(r, f, t)), t + "t"))
                .collect(Collectors.toList());
            if (!pts.isEmpty()) series.put(shortName(f), pts);
        }

        // Linha ideal baseada no max de threads disponível
        int maxT = series.values().stream().flatMap(List::stream)
            .mapToInt(p -> (int) p.x).max().orElse(8);
        List<PointXY> ideal = new ArrayList<>();
        Set<Integer> threadCounts = series.values().stream().flatMap(List::stream)
            .map(p -> (int) p.x).collect(Collectors.toCollection(TreeSet::new));
        for (int t : threadCounts) ideal.add(new PointXY(t, t, t + "t"));
        series.put("Ideal (linear)", ideal);

        return lineChart(
            "4. Speedup do ParallelCPU por número de threads",
            "Speedup ideal = crescimento linear com threads.  Desvio = overhead de sincronização.",
            "Número de threads", "Speedup (×) vs SerialCPU",
            series, true, true, List.of(C_CPU4, C_VT, C_GPU, new Color(255,255,80,160)));
    }

    /**
     * 5. Eficiência paralela = speedup / threads.
     * Valor ideal = 1,0 (100 % dos núcleos aproveitados).
     * Demonstra a Lei de Amdahl: eficiência cai com mais threads.
     */
    private static BufferedImage chart05_EficienciaParalela(List<BenchmarkResult> r) {
        Map<String, List<PointXY>> series = new LinkedHashMap<>();
        for (String f : orderedFiles(r)) {
            double serial = Math.max(0.0001, medianTime(r, f, "SerialCPU"));
            List<PointXY> pts = r.stream()
                .filter(x -> x.file.equals(f) && isThreadOnlyParallelCPU(x.method))
                .map(x -> x.threads).distinct().sorted()
                .map(t -> {
                    double spd = serial / Math.max(0.0001, medianTimeByThreads(r, f, t));
                    return new PointXY(t, spd / Math.max(1, t), t + "t");
                })
                .collect(Collectors.toList());
            if (!pts.isEmpty()) series.put(shortName(f), pts);
        }
        return lineChart(
            "5. Eficiência paralela (Lei de Amdahl)",
            "Eficiência = speedup ÷ threads.  Valor ideal = 1,0.  Queda indica overhead ou porção serial.",
            "Número de threads", "Eficiência paralela",
            series, true, false, List.of(C_CPU4, C_VT, C_GPU));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BLOCO 3 — Impacto do tamanho do texto
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 6. Escala: eixo X = palavras no texto, eixo Y = tempo mediano.
     * Mostra como cada método cresce com o volume de dados.
     * Métodos mais paralelos devem crescer mais devagar.
     */
    private static BufferedImage chart06_EscalaTamanho(List<BenchmarkResult> r) {
        List<String> methods = repMethods(r);
        Map<String, List<PointXY>> series = new LinkedHashMap<>();
        for (String m : methods) {
            List<PointXY> pts = orderedFiles(r).stream()
                .map(f -> new PointXY(totalWords(r, f), medianTime(r, f, m), shortName(f)))
                .filter(p -> p.y > 0)
                .collect(Collectors.toList());
            if (!pts.isEmpty()) series.put(m, pts);
        }
        return lineChart(
            "6. Escalabilidade — tempo pelo tamanho do texto",
            "Inclinação menor = escala melhor com volume.  Métodos paralelos devem ser mais planos.",
            "Tamanho do texto (palavras)", "Tempo mediano (ms)",
            series, false, false, methodPalette(methods));
    }

    /**
     * 7. Tempo normalizado por 100 mil palavras.
     * Remove o viés de tamanho: obras maiores naturalmente demoram mais.
     * Mostra a "velocidade real" de cada método independente do volume.
     */
    private static BufferedImage chart07_TempoNormalizado(List<BenchmarkResult> r) {
        List<String> methods = repMethods(r);
        List<String> files   = orderedFiles(r);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            long total = Math.max(1, totalWords(r, f));
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods)
                row.put(m, medianTime(r, f, m) / total * 100_000.0);
            data.put(shortName(f) + "\n(" + String.format("%,d", total) + " pal.)", row);
        }

        return barChart(
            "7. Tempo normalizado por 100 mil palavras",
            "Remove viés de tamanho.  Compara velocidade real entre textos de volumes diferentes.",
            "Texto (total de palavras)", "ms por 100 mil palavras",
            data, methods, methodPalette(methods));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  BLOCO 4 — Estabilidade e variabilidade das 3 execuções
    // ══════════════════════════════════════════════════════════════════════

    /**
     * 8. Tempo das 3 execuções individuais — uma barra por run, por método.
     * Permite verificar se os resultados são estáveis e reproduzíveis.
     * Usa apenas o maior texto para evitar sobrecarga visual.
     */
    private static BufferedImage chart08_TresExecucoes(List<BenchmarkResult> r) {
        // Usa o maior texto (maior nº de palavras) para maximizar diferença entre métodos
        String biggestFile = orderedFiles(r).stream()
            .max(Comparator.comparingLong(f -> totalWords(r, f)))
            .orElse(orderedFiles(r).get(0));

        List<String> methods  = repMethods(r);
        List<String> runLabels = List.of("Run 1", "Run 2", "Run 3");

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String m : methods) {
            Map<String, Double> row = new LinkedHashMap<>();
            List<BenchmarkResult> methodResults = r.stream()
                .filter(x -> x.file.equals(biggestFile) && x.method.equals(m))
                .sorted(Comparator.comparingInt(x -> x.run))
                .collect(Collectors.toList());
            for (int i = 0; i < Math.min(3, methodResults.size()); i++)
                row.put(runLabels.get(i), methodResults.get(i).timeMs);
            data.put(shortMethodLabel(m), row);
        }

        List<Color> runColors = List.of(
            new Color(0x6C8EF5), new Color(0x4EC994), new Color(0xF5A623));

        return barChart(
            "8. Tempo das 3 execuções individuais — " + shortName(biggestFile),
            "Barras próximas = resultado estável e reproduzível.  Variação grande = instabilidade.",
            "Método", "Tempo de execução (ms)",
            data, runLabels, runColors);
    }

    /**
     * 9. Média ± desvio padrão das 3 execuções.
     * A barra de erro mostra a dispersão dos resultados.
     * Desvio pequeno em relação à média = método estável.
     */
    private static BufferedImage chart09_MediaDesvio(List<BenchmarkResult> r) {
        List<String> methods = repMethods(r);
        List<String> files   = orderedFiles(r);

        Map<String, Map<String, Double>> means  = new LinkedHashMap<>();
        Map<String, Map<String, Double>> errors = new LinkedHashMap<>();

        for (String f : files) {
            Map<String, Double> mRow = new LinkedHashMap<>();
            Map<String, Double> eRow = new LinkedHashMap<>();
            for (String m : methods) {
                List<Double> ts = times(r, f, m);
                mRow.put(m, avg(ts));
                eRow.put(m, stddev(ts));
            }
            means.put(shortName(f), mRow);
            errors.put(shortName(f), eRow);
        }

        return barChartWithError(
            "9. Média e desvio padrão das 3 execuções",
            "Barra de erro = ± 1 desvio padrão.  Erro pequeno vs. média = método estável.",
            "Texto / Obra literária", "Tempo médio (ms)",
            means, errors, methods, methodPalette(methods));
    }

    /**
     * 10. GPU vs melhor CPU paralela vs SerialCPU.
     * Apenas 3 séries — máxima clareza para a discussão da GPU.
     * Gráfico de destaque para o relatório.
     */
    private static BufferedImage chart10_GpuVsCpu(List<BenchmarkResult> r) {
        String gpuString = gpuLabelContaining(r, "String");
        String gpuHash = gpuLabelContaining(r, "Hash");
        List<String> series  = new ArrayList<>();
        series.add("SerialCPU");
        series.add("Melhor CPU paralela");
        if (gpuString != null) series.add(gpuString);
        if (gpuHash != null) series.add(gpuHash);
        List<Color>  palette = series.stream().map(ChartGenerator::colorForMethod).collect(Collectors.toList());
        List<String> files   = orderedFiles(r);

        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("SerialCPU",           medianTime(r, f, "SerialCPU"));
            row.put("Melhor CPU paralela", bestParallelMedian(r, f));
            if (gpuString != null) row.put(gpuString, medianTime(r, f, gpuString));
            if (gpuHash != null) row.put(gpuHash, medianTime(r, f, gpuHash));
            data.put(shortName(f), row);
        }

        return barChart(
            "10. SerialCPU vs melhor CPU paralela vs GPU/OpenCL",
            "Compara GPU por string e por hash.  Hash reduz trabalho textual, mas pode ter colisões teóricas.",
            "Texto / Obra literária", "Tempo mediano (ms)",
            data, series, palette);
    }

    /**
     * 11. Compara granularidades de chunks no ParallelCPU.
     */
    private static BufferedImage chart11_ChunkGranularity(List<BenchmarkResult> r) {
        List<String> methods = r.stream().map(x -> x.method)
                .filter(m -> m.contains("chunks") || m.contains("dynChunks"))
                .distinct().sorted(Comparator.comparingInt(ChartGenerator::methodOrder))
                .collect(Collectors.toList());
        if (methods.isEmpty()) methods = List.of("ParallelCPU-8t");
        return barChart(
                "11. Granularidade de chunks no ParallelCPU",
                "Compara chunks fixos e dinâmicos.  Menor tempo = melhor granularidade para o texto.",
                "Texto / Obra literária", "Tempo mediano (ms)",
                buildBarData(r, orderedFiles(r), methods), methods, methodPalette(methods));
    }

    /**
     * 12. Compara thresholds fixos e dinâmicos do ForkJoin.
     */
    private static BufferedImage chart12_ForkJoinThresholds(List<BenchmarkResult> r) {
        List<String> methods = r.stream().map(x -> x.method)
                .filter(m -> m.startsWith("ForkJoinCPU"))
                .distinct().sorted(Comparator.comparingInt(ChartGenerator::methodOrder))
                .collect(Collectors.toList());
        if (methods.isEmpty()) methods = List.of("ForkJoinCPU-dyn");
        return barChart(
                "12. Granularidade do ForkJoin — thresholds",
                "Threshold menor cria mais tarefas; threshold maior reduz overhead.  Menor tempo = melhor.",
                "Texto / Obra literária", "Tempo mediano (ms)",
                buildBarData(r, orderedFiles(r), methods), methods, methodPalette(methods));
    }

    /**
     * 13. Decomposição do tempo da GPU em preparação e kernel/leitura.
     */
    private static BufferedImage chart13_GpuPreparationKernel(List<BenchmarkResult> r) {
        String biggest = orderedFiles(r).stream()
                .max(Comparator.comparingLong(f -> totalWords(r, f)))
                .orElse(orderedFiles(r).isEmpty() ? "" : orderedFiles(r).get(0));
        List<String> gpuMethods = r.stream().map(x -> x.method)
                .filter(m -> m.startsWith("ParallelGPU"))
                .distinct().sorted(Comparator.comparingInt(ChartGenerator::methodOrder))
                .collect(Collectors.toList());
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String m : gpuMethods) {
            Map<String, Double> row = new LinkedHashMap<>();
            row.put("Preparação", medianMetric(r, biggest, m, "prep"));
            row.put("Kernel/leitura", medianMetric(r, biggest, m, "kernel"));
            row.put("Total", medianTime(r, biggest, m));
            data.put(shortMethodLabel(m), row);
        }
        if (data.isEmpty()) data.put("Sem GPU", Map.of("Preparação", 0.0, "Kernel/leitura", 0.0, "Total", 0.0));
        List<String> series = List.of("Preparação", "Kernel/leitura", "Total");
        return barChart(
                "13. GPU — preparação vs kernel/leitura",
                "Mostra quanto do tempo da GPU é overhead de dados e quanto é execução/leitura do resultado.",
                "Método GPU — " + shortName(biggest), "Tempo mediano (ms)",
                data, series, List.of(C_CPU4, C_PSTREAM, C_GPU));
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Primitivos de desenho
    // ══════════════════════════════════════════════════════════════════════

    private static BufferedImage barChart(String title, String subtitle,
            String xLabel, String yLabel,
            Map<String, Map<String, Double>> data,
            List<String> series, List<Color> palette) {
        return barChartWithError(title, subtitle, xLabel, yLabel, data, null, series, palette);
    }

    private static BufferedImage barChartWithError(String title, String subtitle,
            String xLabel, String yLabel,
            Map<String, Map<String, Double>> data,
            Map<String, Map<String, Double>> errData,
            List<String> series, List<Color> palette) {

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setup(img, title, subtitle);

        int mL = 115, mR = 36, mT = 100, mB = 190;
        int pW = W - mL - mR, pH = H - mT - mB;

        // Max Y with error bars considered
        double maxVal = data.values().stream()
            .flatMap(m -> m.values().stream()).mapToDouble(v -> v).max().orElse(1);
        if (errData != null) {
            for (Map.Entry<String, Map<String,Double>> fe : errData.entrySet()) {
                Map<String,Double> mMap = data.getOrDefault(fe.getKey(), Map.of());
                for (Map.Entry<String,Double> ee : fe.getValue().entrySet())
                    maxVal = Math.max(maxVal, mMap.getOrDefault(ee.getKey(), 0.0) + ee.getValue());
            }
        }
        maxVal = Math.max(1e-9, maxVal * 1.22);

        drawGrid(g, mL, mT, pW, pH, maxVal);

        List<String> groups = new ArrayList<>(data.keySet());
        int nG = groups.size(), nS = series.size();
        int groupW = pW / Math.max(nG, 1);
        int gap    = 5;
        int barW   = Math.max(12, Math.min(52, (groupW - 20 - gap * (nS - 1)) / Math.max(nS, 1)));
        int innerW = barW * nS + gap * (nS - 1);

        for (int gi = 0; gi < nG; gi++) {
            String grp = groups.get(gi);
            Map<String,Double> row  = data.getOrDefault(grp, Map.of());
            Map<String,Double> eRow = errData == null ? Map.of() : errData.getOrDefault(grp, Map.of());
            int gx = mL + gi * groupW + (groupW - innerW) / 2;

            for (int si = 0; si < nS; si++) {
                String m  = series.get(si);
                double val = row.getOrDefault(m, 0.0);
                int bH = (int) Math.round(val / maxVal * pH);
                int x  = gx + si * (barW + gap);
                int y  = mT + pH - bH;
                Color c = palette.get(si % palette.size());

                // Bar with rounded top
                g.setColor(c);
                g.fillRoundRect(x, y, barW, Math.max(2, bH), 6, 6);
                // Sharp bottom corners
                if (bH > 6) g.fillRect(x, y + 6, barW, Math.max(0, bH - 6));
                g.setColor(c.darker());
                g.drawRoundRect(x, y, barW, Math.max(2, bH), 6, 6);

                // Error bar
                if (errData != null && eRow.containsKey(m)) {
                    double err = eRow.get(m);
                    int eH = (int) Math.round(err / maxVal * pH);
                    int cx = x + barW / 2;
                    int et = Math.max(mT + 1, y - eH);
                    g.setColor(TEXT);
                    g.setStroke(new BasicStroke(2.2f));
                    g.drawLine(cx, et, cx, y);
                    g.drawLine(cx - 6, et, cx + 6, et);
                    g.drawLine(cx - 6, y,  cx + 6, y);
                    g.setStroke(new BasicStroke(1f));
                }

                // Value label above bar
                if (val > 0 && bH > 12) {
                    String lbl = compact(val);
                    g.setFont(new Font("SansSerif", Font.BOLD, 10));
                    FontMetrics fm = g.getFontMetrics();
                    int lw = fm.stringWidth(lbl);
                    g.setColor(TEXT);
                    if (lw + 4 <= barW) {
                        g.drawString(lbl, x + (barW - lw) / 2, y - 4);
                    } else {
                        Graphics2D gr2 = (Graphics2D) g.create();
                        gr2.setFont(new Font("SansSerif", Font.BOLD, 9));
                        gr2.setColor(DIMMED);
                        gr2.translate(x + barW / 2, y - 3);
                        gr2.rotate(-Math.PI / 2);
                        gr2.drawString(lbl, -lw, gr2.getFontMetrics().getAscent() / 2);
                        gr2.dispose();
                    }
                }
            }
            drawGroupLabel(g, grp, mL + gi * groupW + groupW / 2, mT + pH + 20, groupW - 6);
        }

        drawAxes(g, mL, mT, pW, pH, xLabel, yLabel, mB);
        drawLegend(g, series, palette, mL, H - 90);
        g.dispose();
        return img;
    }

    private static BufferedImage lineChart(String title, String subtitle,
            String xLabel, String yLabel,
            Map<String, List<PointXY>> series,
            boolean integerX, boolean idealLine,
            List<Color> palette) {

        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = setup(img, title, subtitle);

        int mL = 115, mR = 260, mT = 100, mB = 130;
        int pW = W - mL - mR, pH = H - mT - mB;

        double minX = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.x).min().orElse(0);
        double maxX = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.x).max().orElse(1);
        double maxY = series.values().stream().flatMap(List::stream).mapToDouble(p -> p.y).max().orElse(1);
        if (Math.abs(maxX - minX) < 1e-9) maxX = minX + 1;
        maxY = Math.max(1e-9, maxY * 1.18);

        drawGrid(g, mL, mT, pW, pH, maxY);

        // Ideal linear reference (for speedup chart)
        if (idealLine) {
            g.setColor(new Color(255, 220, 60, 70));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{10f, 6f}, 0f));
            int ix1 = mL, iy1 = mT + pH;
            int ix2 = mL + (int) Math.round((maxX - minX) / (maxX - minX) * pW);
            int iy2 = mT + pH - (int) Math.round(maxX / maxY * pH);
            g.drawLine(ix1, iy1, ix2, Math.max(mT, iy2));
            g.setStroke(new BasicStroke(1f));
        }

        // Efficiency = 1 reference
        if (yLabel.contains("ficiência") || yLabel.contains("Eficiência")) {
            int refY = mT + pH - (int) Math.round(1.0 / maxY * pH);
            if (refY > mT && refY < mT + pH) {
                g.setColor(new Color(255, 220, 60, 70));
                g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        10f, new float[]{8f, 5f}, 0f));
                g.drawLine(mL, refY, mL + pW, refY);
                g.setFont(new Font("SansSerif", Font.ITALIC, 11));
                g.setColor(new Color(255, 220, 60, 150));
                g.drawString("ideal = 1,0", mL + pW + 8, refY + 4);
                g.setStroke(new BasicStroke(1f));
            }
        }

        float[][] dashes = {null, {14f,6f}, {4f,6f}, {14f,6f,4f,6f}, {18f,6f}};

        // Pass 1: lines
        record EndLabel(String name, Color col, int x, int y, double val) {}
        List<EndLabel> ends = new ArrayList<>();

        int si = 0;
        for (Map.Entry<String, List<PointXY>> e : series.entrySet()) {
            if (e.getKey().equals("Ideal (linear)")) { si++; continue; }
            List<PointXY> pts = sorted(e.getValue());
            Color c = palette.get(si % palette.size());
            float[] dash = dashes[si % dashes.length];
            g.setColor(c);
            g.setStroke(dash == null
                ? new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                : new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f));

            int px = Integer.MIN_VALUE, py = Integer.MIN_VALUE;
            for (PointXY p : pts) {
                int x = mL + (int) Math.round((p.x - minX) / (maxX - minX) * pW);
                int y = mT + pH - (int) Math.round(p.y / maxY * pH);
                if (px != Integer.MIN_VALUE) g.drawLine(px, py, x, y);
                px = x; py = y;
            }
            if (!pts.isEmpty()) {
                PointXY last = pts.get(pts.size() - 1);
                int ex = mL + (int) Math.round((last.x - minX) / (maxX - minX) * pW);
                int ey = mT + pH - (int) Math.round(last.y / maxY * pH);
                ends.add(new EndLabel(e.getKey(), c, ex, ey, last.y));
            }
            si++;
        }

        // Ideal dashed line on top
        if (idealLine && series.containsKey("Ideal (linear)")) {
            List<PointXY> pts = sorted(series.get("Ideal (linear)"));
            g.setColor(new Color(255, 220, 60, 130));
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10f, new float[]{10f, 6f}, 0f));
            int px = Integer.MIN_VALUE, py = Integer.MIN_VALUE;
            for (PointXY p : pts) {
                int x = mL + (int) Math.round((p.x - minX) / (maxX - minX) * pW);
                int y = mT + pH - (int) Math.round(Math.min(p.y, maxY * 0.98) / maxY * pH);
                if (px != Integer.MIN_VALUE) g.drawLine(px, py, x, y);
                px = x; py = y;
            }
            g.setStroke(new BasicStroke(1f));
            g.setFont(new Font("SansSerif", Font.ITALIC, 12));
            g.setColor(new Color(255, 220, 60, 160));
            g.drawString("Ideal (linear)", mL + pW + 8, mT + 20);
        }

        // Pass 2: dots
        g.setStroke(new BasicStroke(1.5f));
        si = 0;
        for (Map.Entry<String, List<PointXY>> e : series.entrySet()) {
            if (e.getKey().equals("Ideal (linear)")) { si++; continue; }
            Color c = palette.get(si % palette.size());
            for (PointXY p : e.getValue()) {
                int x = mL + (int) Math.round((p.x - minX) / (maxX - minX) * pW);
                int y = mT + pH - (int) Math.round(p.y / maxY * pH);
                g.setColor(BG);   g.fillOval(x-7, y-7, 14, 14);
                g.setColor(c);    g.fillOval(x-5, y-5, 10, 10);
                g.setColor(Color.WHITE); g.fillOval(x-2, y-2, 4, 4);
            }
            si++;
        }

        // X-axis tick labels
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(DIMMED);
        if (integerX) {
            TreeSet<Integer> ticks = series.values().stream().flatMap(List::stream)
                .map(p -> (int) Math.round(p.x))
                .collect(Collectors.toCollection(TreeSet::new));
            for (int t : ticks) {
                int x = mL + (int) Math.round((t - minX) / (maxX - minX) * pW);
                String lbl = String.valueOf(t);
                g.drawString(lbl, x - g.getFontMetrics().stringWidth(lbl)/2, mT + pH + 22);
            }
        } else {
            // Use first series' labels (book names)
            final double fMinX = minX, fMaxX = maxX;
            final int fmL = mL, fmT = mT, fpW = pW, fpH = pH;
            series.values().stream().findFirst().ifPresent(pts ->
                pts.forEach(p -> {
                    int x = fmL + (int) Math.round((p.x - fMinX) / (fMaxX - fMinX) * fpW);
                    g.drawString(p.label, x - g.getFontMetrics().stringWidth(p.label)/2, fmT + fpH + 22);
                }));
        }

        // End-of-line labels with collision avoidance
        ends.sort(Comparator.comparingInt(EndLabel::y));
        int prevY = Integer.MIN_VALUE;
        for (EndLabel ep : ends) {
            int ly = Math.max(ep.y(), prevY == Integer.MIN_VALUE ? ep.y() : prevY + 20);
            ly = Math.min(ly, mT + pH - 4);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.setColor(ep.col());
            g.fillRoundRect(mL + pW + 14, ly - 11, 13, 12, 3, 3);
            g.setColor(TEXT);
            g.drawString(ep.name() + "  " + compact(ep.val()), mL + pW + 32, ly);
            prevY = ly;
        }

        drawAxes(g, mL, mT, pW, pH, xLabel, yLabel, mB);
        g.dispose();
        return img;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Utilitários de desenho
    // ══════════════════════════════════════════════════════════════════════

    private static Graphics2D setup(BufferedImage img, String title, String subtitle) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,         RenderingHints.VALUE_RENDER_QUALITY);

        g.setPaint(new GradientPaint(0, 0, BG, 0, H, BG_PLOT));
        g.fillRect(0, 0, W, H);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.setColor(TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(title, (W - fm.stringWidth(title)) / 2, 38);

        g.setFont(new Font("SansSerif", Font.ITALIC, 13));
        g.setColor(DIMMED);
        fm = g.getFontMetrics();
        g.drawString(subtitle, (W - fm.stringWidth(subtitle)) / 2, 60);

        g.setColor(new Color(255, 255, 255, 20));
        g.fillRect(40, 74, W - 80, 1);
        return g;
    }

    private static void drawGrid(Graphics2D g, int x, int y, int w, int h, double maxVal) {
        int ticks = 6;
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int i = 0; i <= ticks; i++) {
            int yy = y + h - (int) Math.round((double) i / ticks * h);
            g.setColor(i == 0 ? GRID_AX : GRID);
            g.setStroke(i == 0
                ? new BasicStroke(1.6f)
                : new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{8f,5f}, 0f));
            g.drawLine(x, yy, x + w, yy);
            g.setStroke(new BasicStroke(1f));
            g.setColor(DIMMED);
            String lbl = compact(maxVal * i / ticks);
            g.drawString(lbl, x - g.getFontMetrics().stringWidth(lbl) - 8, yy + 5);
        }
    }

    private static void drawAxes(Graphics2D g, int x, int y, int w, int h,
            String xLabel, String yLabel, int mB) {
        g.setColor(GRID_AX);
        g.setStroke(new BasicStroke(1.8f));
        g.drawLine(x, y, x, y + h);
        g.drawLine(x, y + h, x + w, y + h);
        g.setStroke(new BasicStroke(1f));

        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.setColor(TEXT);
        FontMetrics fm = g.getFontMetrics();
        g.drawString(xLabel, x + w/2 - fm.stringWidth(xLabel)/2, H - mB + 46);

        Graphics2D gr = (Graphics2D) g.create();
        gr.setFont(new Font("SansSerif", Font.BOLD, 14));
        gr.setColor(TEXT);
        gr.rotate(-Math.PI / 2);
        gr.drawString(yLabel, -(y + h/2 + gr.getFontMetrics().stringWidth(yLabel)/2), 24);
        gr.dispose();
    }

    private static void drawLegend(Graphics2D g, List<String> series, List<Color> palette,
            int startX, int y) {
        int lx = startX;
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        for (int i = 0; i < series.size(); i++) {
            Color c = palette.get(i % palette.size());
            String lbl = shortMethodLabel(series.get(i));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(c);
            g.fillRoundRect(lx, y - 12, 15, 13, 4, 4);
            g.setColor(TEXT);
            g.drawString(lbl, lx + 21, y);
            lx += fm.stringWidth(lbl) + 46;
            if (lx > W - 180) { lx = startX; y += 21; }
        }
    }

    private static void drawGroupLabel(Graphics2D g, String text, int cx, int y, int maxW) {
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.setColor(DIMMED);
        String[] parts = text.split("\n");
        for (int i = 0; i < parts.length; i++) {
            // Each part may need sub-wrapping
            String[] words = parts[i].split("[-_ ]+");
            List<String> lines = new ArrayList<>();
            String line = "";
            for (String w : words) {
                if (w.isBlank()) continue;
                String cand = line.isEmpty() ? w : line + " " + w;
                if (g.getFontMetrics().stringWidth(cand) > maxW && !line.isEmpty()) {
                    lines.add(line); line = w;
                } else line = cand;
            }
            if (!line.isEmpty()) lines.add(line);
            for (int j = 0; j < Math.min(3, lines.size()); j++) {
                String l = lines.get(j);
                g.drawString(l, cx - g.getFontMetrics().stringWidth(l)/2, y + (i * 2 + j) * 16);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Helpers de dados
    // ══════════════════════════════════════════════════════════════════════

    private static Map<String, Map<String, Double>> buildBarData(
            List<BenchmarkResult> r, List<String> files, List<String> methods) {
        Map<String, Map<String, Double>> data = new LinkedHashMap<>();
        for (String f : files) {
            Map<String, Double> row = new LinkedHashMap<>();
            for (String m : methods) row.put(m, medianTime(r, f, m));
            data.put(shortName(f), row);
        }
        return data;
    }

    /** Métodos representativos — 1 por família + melhor ParallelCPU. Máx 6. */
    private static List<String> repMethods(List<BenchmarkResult> r) {
        List<String> rep = new ArrayList<>();
        if (hasMethod(r, "SerialCPU"))      rep.add("SerialCPU");

        // Melhor ParallelCPU
        String bestCpu = r.stream()
            .filter(x -> isThreadOnlyParallelCPU(x.method))
            .map(x -> x.method).distinct()
            .min(Comparator.comparingDouble(m ->
                r.stream().filter(x -> x.method.equals(m)).mapToDouble(x -> x.timeMs).average().orElse(Double.MAX_VALUE)))
            .orElse(null);
        if (bestCpu != null) rep.add(bestCpu);

        // Melhor ForkJoin entre thresholds fixos e dinâmico
        String bestFork = r.stream()
            .filter(x -> x.method.startsWith("ForkJoinCPU"))
            .map(x -> x.method).distinct()
            .min(Comparator.comparingDouble(m ->
                r.stream().filter(x -> x.method.equals(m)).mapToDouble(x -> x.timeMs).average().orElse(Double.MAX_VALUE)))
            .orElse(null);
        if (bestFork != null) rep.add(bestFork);
        if (hasMethod(r, "ParallelStream")) rep.add("ParallelStream");

        // Melhor VirtualThreads
        String bestVT = r.stream()
            .filter(x -> x.method.startsWith("VirtualThreads"))
            .map(x -> x.method).distinct()
            .min(Comparator.comparingDouble(m ->
                r.stream().filter(x -> x.method.equals(m)).mapToDouble(x -> x.timeMs).average().orElse(Double.MAX_VALUE)))
            .orElse(null);
        if (bestVT != null) rep.add(bestVT);

        String gpuHash = gpuLabelContaining(r, "Hash");
        String gpuString = gpuLabelContaining(r, "String");
        if (gpuHash != null) rep.add(gpuHash);
        else if (gpuString != null) rep.add(gpuString);

        return rep;
    }

    private static double medianMetric(List<BenchmarkResult> r, String file, String method, String metric) {
        List<Double> values = r.stream()
                .filter(x -> x.file.equals(file) && x.method.equals(method))
                .map(x -> metric.equals("prep") ? x.preparationMs : x.kernelMs)
                .sorted().collect(Collectors.toList());
        return median(values);
    }

    private static double medianTime(List<BenchmarkResult> r, String file, String method) {
        return median(times(r, file, method));
    }

    private static double medianTimeByThreads(List<BenchmarkResult> r, String file, int threads) {
        return median(r.stream()
            .filter(x -> x.file.equals(file) && isThreadOnlyParallelCPU(x.method) && x.threads == threads)
            .map(x -> x.timeMs).collect(Collectors.toList()));
    }

    private static List<Double> times(List<BenchmarkResult> r, String file, String method) {
        return r.stream().filter(x -> x.file.equals(file) && x.method.equals(method))
            .map(x -> x.timeMs).sorted().collect(Collectors.toList());
    }

    private static double median(List<Double> v) {
        if (v == null || v.isEmpty()) return 0;
        List<Double> s = new ArrayList<>(v); Collections.sort(s);
        int n = s.size();
        return n % 2 == 1 ? s.get(n/2) : (s.get(n/2-1) + s.get(n/2)) / 2.0;
    }

    private static double avg(List<Double> v) {
        return v == null || v.isEmpty() ? 0 : v.stream().mapToDouble(d->d).average().orElse(0);
    }

    private static double stddev(List<Double> v) {
        if (v == null || v.size() < 2) return 0;
        double a = avg(v);
        return Math.sqrt(v.stream().mapToDouble(x -> (x-a)*(x-a)).sum() / (v.size()-1));
    }

    private static long totalWords(List<BenchmarkResult> r, String file) {
        return r.stream().filter(x -> x.file.equals(file)).mapToLong(x -> x.totalWords).findFirst().orElse(1);
    }

    private static double bestParallelMedian(List<BenchmarkResult> r, String file) {
        return r.stream()
            .filter(x -> x.file.equals(file) && (x.family.equals("PARALLEL_CPU") || x.family.equals("PARALLEL_STREAM")))
            .map(x -> x.method).distinct()
            .mapToDouble(m -> medianTime(r, file, m))
            .filter(v -> v > 0).min().orElse(0);
    }

    private static boolean hasMethod(List<BenchmarkResult> r, String m) {
        return r.stream().anyMatch(x -> x.method.equals(m));
    }

    private static String gpuLabel(List<BenchmarkResult> r) {
        return r.stream().map(x -> x.method).filter(m -> m.startsWith("ParallelGPU"))
            .findFirst().orElse("ParallelGPU");
    }

    private static String gpuLabelContaining(List<BenchmarkResult> r, String token) {
        return r.stream().map(x -> x.method)
            .filter(m -> m.startsWith("ParallelGPU") && m.contains(token))
            .findFirst().orElse(null);
    }

    private static List<String> orderedFiles(List<BenchmarkResult> r) {
        List<String> order = List.of("donquixote","dracula","mobydick","moby");
        return r.stream().map(x -> x.file).distinct()
            .sorted(Comparator.comparingInt(f -> {
                String n = f.toLowerCase().replaceAll("[^a-z]","");
                for (int i = 0; i < order.size(); i++) if (n.contains(order.get(i))) return i;
                return 99;
            })).collect(Collectors.toList());
    }

    private static List<PointXY> sorted(List<PointXY> pts) {
        return pts.stream().sorted(Comparator.comparingDouble(p -> p.x)).collect(Collectors.toList());
    }

    private static boolean isThreadOnlyParallelCPU(String method) {
        return method != null && method.matches("ParallelCPU-\\d+t");
    }

    private static int methodOrder(String m) {
        if (m.equals("SerialCPU"))           return 0;
        if (m.equals("SerialStream"))        return 1;
        if (m.startsWith("ParallelCPU"))     return 10 + extractInt(m,'-','t');
        if (m.startsWith("ForkJoinCPU"))     return 80 + (m.contains("dyn") ? 50 : extractTrailingInt(m));
        if (m.startsWith("ParallelStream"))  return 90;
        if (m.startsWith("VirtualThreads"))  return 200 + extractInt(m,'-','c');
        if (m.contains("GPU-String"))        return 10000;
        if (m.contains("GPU-Hash"))          return 10010;
        if (m.startsWith("ParallelGPU"))     return 10000;
        return 20000;
    }

    private static int extractTrailingInt(String s) {
        try {
            String digits = s.replaceAll(".*?(\\d+)$", "$1");
            return Integer.parseInt(digits);
        } catch (Exception ignored) {}
        return 999;
    }

    private static int extractInt(String s, char a, char b) {
        try { int i = s.indexOf(a), j = s.indexOf(b,i); if(i>=0&&j>i) return Integer.parseInt(s.substring(i+1,j)); }
        catch (Exception ignored) {}
        return 999;
    }

    private static Color colorForMethod(String m) {
        if (m == null)                       return DIMMED;
        if (m.equals("SerialCPU"))           return C_SERIAL;
        if (m.equals("SerialStream"))        return C_SSTREAM;
        if (m.contains("CPU-2"))             return C_CPU2;
        if (m.contains("CPU-4"))             return C_CPU4;
        if (m.contains("CPU-8"))             return C_CPU8;
        if (m.startsWith("ParallelCPU"))     return C_CPUN;
        if (m.startsWith("ForkJoinCPU"))     return C_FORK;
        if (m.startsWith("ParallelStream"))  return C_PSTREAM;
        if (m.startsWith("VirtualThreads"))  return C_VT;
        if (m.startsWith("ParallelGPU"))     return C_GPU;
        return DIMMED;
    }

    private static List<Color> methodPalette(List<String> methods) {
        return methods.stream().map(ChartGenerator::colorForMethod).collect(Collectors.toList());
    }

    private static String shortName(String f) {
        String n = f.replace(".txt","").toLowerCase();
        if (n.contains("donquixote")) return "Don Quixote";
        if (n.contains("dracula"))    return "Dracula";
        if (n.contains("mobydick"))   return "Moby Dick";
        return f.replace("_"," ").replace(".txt","");
    }

    private static String shortMethodLabel(String m) {
        if (m == null) return "-";
        return switch (m) {
            case "SerialCPU"       -> "Serial CPU";
            case "SerialStream"    -> "Serial Stream";
            case "ParallelStream"  -> "Parallel Stream";
            case "ParallelGPU"     -> "GPU/OpenCL";
            case "ParallelGPU-String" -> "GPU String";
            case "ParallelGPU-String-FallbackCPU" -> "GPU String FB";
            case "ParallelGPU-Hash" -> "GPU Hash";
            case "ParallelGPU-Hash-FallbackCPU" -> "GPU Hash FB";
            case "ParallelGPU-FallbackCPU" -> "GPU (fallback)";
            case "Melhor CPU paralela"     -> "Melhor CPU par.";
            case "Run 1"           -> "Run 1";
            case "Run 2"           -> "Run 2";
            case "Run 3"           -> "Run 3";
            default -> m.replace("ParallelCPU-","CPU ")
                        .replace("ForkJoinCPU-th", "ForkJoin th")
                        .replace("ForkJoinCPU-dyn", "ForkJoin dyn")
                        .replace("VirtualThreads-","VT ")
                        .replace("dynChunks", "dyn ch")
                        .replace("chunks"," ch").replace("t"," t");
        };
    }

    private static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) return String.format("%.1fM", v/1e6);
        if (a >= 10_000)    return String.format("%.0fK", v/1e3);
        if (a >= 1_000)     return String.format("%.1fK", v/1e3);
        if (a >= 100)       return String.format("%.0f", v);
        if (a >= 10)        return String.format("%.1f", v);
        return String.format("%.2f", v);
    }

    private static void save(BufferedImage img, String path) throws IOException {
        File f = new File(path);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", f);
        System.out.printf("  Saved -> %s%n", f.getName());
    }

    private record PointXY(double x, double y, String label) {}
}
