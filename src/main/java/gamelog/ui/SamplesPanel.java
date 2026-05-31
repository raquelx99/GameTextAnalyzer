package gamelog.ui;

import gamelog.SampleConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Painel com as amostras oficiais do trabalho e sua interpretação gamificada.
 * Também permite ao usuário importar arquivos .txt personalizados para data/samples/.
 */
public class SamplesPanel extends JPanel {

    private final MainWindow win;
    private JPanel contentPanel;

    public SamplesPanel(MainWindow win) {
        this.win = win;
        setBackground(Theme.BG_BASE);
        setLayout(new BorderLayout());
        build();
    }

    private void build() {
        add(buildHeader(), BorderLayout.NORTH);

        contentPanel = new JPanel();
        contentPanel.setBackground(Theme.BG_BASE);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(26, 30, 26, 30));
        populateContent();

        JScrollPane scroll = new JScrollPane(contentPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BG_BASE);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BG_SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.DIVIDER),
                BorderFactory.createEmptyBorder(16, 28, 16, 28)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        left.setBackground(Theme.BG_SURFACE);
        left.add(MainWindow.sectionTitle("▣  Amostras Oficiais"));
        left.add(MainWindow.subLabel("Textos obrigatórios usados no benchmark oficial"));
        header.add(left, BorderLayout.WEST);

        JButton btnImport = Theme.primaryBtn("＋  Adicionar amostra");
        btnImport.addActionListener(e -> importSamples());
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setBackground(Theme.BG_SURFACE);
        btnRow.add(btnImport);
        header.add(btnRow, BorderLayout.EAST);

        return header;
    }

    // ── Content ───────────────────────────────────────────────────────────────

    private void populateContent() {
        contentPanel.removeAll();

        contentPanel.add(infoBox());
        contentPanel.add(Box.createVerticalStrut(18));

        JLabel samplesTitle = Theme.h3("Mundos narrativos analisados");
        samplesTitle.setAlignmentX(LEFT_ALIGNMENT);
        contentPanel.add(samplesTitle);
        contentPanel.add(Box.createVerticalStrut(12));

        for (SampleConfig sample : SampleConfig.OFFICIAL_SAMPLES) {
            contentPanel.add(sampleCard(sample));
            contentPanel.add(Box.createVerticalStrut(12));
        }

        List<File> extras = extraFiles();
        if (!extras.isEmpty()) {
            contentPanel.add(Box.createVerticalStrut(8));
            contentPanel.add(Theme.divider());
            contentPanel.add(Box.createVerticalStrut(18));

            JLabel extrasTitle = Theme.h3("Arquivos adicionais");
            extrasTitle.setAlignmentX(LEFT_ALIGNMENT);
            contentPanel.add(extrasTitle);
            contentPanel.add(Box.createVerticalStrut(4));
            JLabel extrasHint = Theme.muted("Arquivos importados disponíveis em data/samples/ para análise livre.");
            extrasHint.setAlignmentX(LEFT_ALIGNMENT);
            contentPanel.add(extrasHint);
            contentPanel.add(Box.createVerticalStrut(12));

            for (File f : extras) {
                contentPanel.add(extraFileCard(f));
                contentPanel.add(Box.createVerticalStrut(8));
            }
        }

        contentPanel.add(Box.createVerticalStrut(12));
        contentPanel.add(noteBox());
        contentPanel.add(Box.createVerticalGlue());

        contentPanel.revalidate();
        contentPanel.repaint();
    }

    // ── Import ────────────────────────────────────────────────────────────────

    private void importSamples() {
        JFileChooser chooser = new JFileChooser();
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("Arquivos de texto (.txt)", "txt"));
        chooser.setDialogTitle("Selecionar arquivo(s) de amostra");

        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;

        SampleConfig.ensureSampleDirectory();

        int copied = 0;
        List<String> errors = new ArrayList<>();
        for (File src : chooser.getSelectedFiles()) {
            File dest = new File(SampleConfig.SAMPLE_DIR + "/" + src.getName());
            try {
                Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException ex) {
                errors.add(src.getName() + ": " + ex.getMessage());
            }
        }

        if (copied > 0) {
            populateContent();
        }

        if (errors.isEmpty()) {
            String msg = copied == 1
                    ? "Arquivo adicionado com sucesso em data/samples/."
                    : copied + " arquivos adicionados com sucesso em data/samples/.";
            JOptionPane.showMessageDialog(this, msg, "Amostra adicionada",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            String msg = (copied > 0 ? copied + " arquivo(s) adicionado(s).\n" : "") +
                    "Erros:\n" + String.join("\n", errors);
            JOptionPane.showMessageDialog(this, msg, "Importação parcial",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    // ── Cards ─────────────────────────────────────────────────────────────────

    private JPanel infoBox() {
        JPanel box = Theme.card(new BorderLayout(0, 10));
        box.setBorder(BorderFactory.createCompoundBorder(box.getBorder(),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel title = Theme.h3("Sobre esta aba");
        JLabel desc = new JLabel(
            "<html><body style='font-family:SansSerif;font-size:12px;color:#A8A8C8;width:760px'>" +
            "Esta versão do projeto utiliza <b style='color:#EDEDF5'>as amostras obrigatórias</b> " +
            "como base oficial dos testes. Você também pode importar qualquer arquivo <b>.txt</b> " +
            "para análise livre nos painéis <i>Explorar Textos</i> e <i>Comparador</i>." +
            "</body></html>");
        box.add(title, BorderLayout.NORTH);
        box.add(desc, BorderLayout.CENTER);
        return box;
    }

    private JPanel sampleCard(SampleConfig sample) {
        JPanel card = Theme.card(new BorderLayout(16, 8));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 155));

        File f = new File(sample.filePath);
        boolean exists = f.exists();

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(sample.title);
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(Theme.TEXT_PRI);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel world = new JLabel(sample.gameWorld);
        world.setFont(new Font("SansSerif", Font.PLAIN, 13));
        world.setForeground(Theme.ACCENT);
        world.setAlignmentX(LEFT_ALIGNMENT);

        JLabel fileLabel = new JLabel("Arquivo: " + f.getName());
        fileLabel.setFont(Theme.FONT_SMALL);
        fileLabel.setForeground(Theme.TEXT_SEC);
        fileLabel.setAlignmentX(LEFT_ALIGNMENT);

        JLabel wordsLabel = new JLabel("Palavras sugeridas: " + String.join(", ", sample.suggestedWords));
        wordsLabel.setFont(Theme.FONT_SMALL);
        wordsLabel.setForeground(Theme.TEXT_SEC);
        wordsLabel.setAlignmentX(LEFT_ALIGNMENT);

        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(world);
        left.add(Box.createVerticalStrut(8));
        left.add(fileLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(wordsLabel);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setPreferredSize(new Dimension(150, 90));

        JLabel status = new JLabel(exists ? "✓ Encontrada" : "! Ausente");
        status.setFont(new Font("SansSerif", Font.BOLD, 13));
        status.setForeground(exists ? Theme.GREEN : Theme.ORANGE);
        status.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel defaultWordLbl = new JLabel("Busca padrão");
        defaultWordLbl.setFont(Theme.FONT_SMALL);
        defaultWordLbl.setForeground(Theme.TEXT_MUTED);
        defaultWordLbl.setAlignmentX(RIGHT_ALIGNMENT);

        JLabel word = new JLabel(sample.defaultWord);
        word.setFont(new Font("SansSerif", Font.BOLD, 17));
        word.setForeground(Theme.TEXT_PRI);
        word.setAlignmentX(RIGHT_ALIGNMENT);

        right.add(status);
        right.add(Box.createVerticalGlue());
        right.add(defaultWordLbl);
        right.add(Box.createVerticalStrut(4));
        right.add(word);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel extraFileCard(File f) {
        JPanel card = Theme.card(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        card.setAlignmentX(LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel name = new JLabel(f.getName());
        name.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
        name.setForeground(Theme.ACCENT);
        name.setAlignmentX(LEFT_ALIGNMENT);

        String size = f.length() > 1024 * 1024
                ? String.format("%.1f MB", f.length() / (1024.0 * 1024))
                : String.format("%.0f KB", f.length() / 1024.0);
        JLabel info = Theme.muted(f.getParent() + "  ·  " + size);
        info.setAlignmentX(LEFT_ALIGNMENT);

        left.add(name);
        left.add(Box.createVerticalStrut(3));
        left.add(info);

        JLabel badge = Theme.badge("personalizado", Theme.PURPLE);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(badge);

        card.add(left, BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel noteBox() {
        JPanel box = Theme.card(new BorderLayout(0, 6));
        box.setBorder(BorderFactory.createCompoundBorder(box.getBorder(),
                BorderFactory.createEmptyBorder(16, 18, 16, 18)));
        box.setAlignmentX(LEFT_ALIGNMENT);
        box.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel title = Theme.h3("Fluxo oficial do projeto");
        JLabel text = new JLabel(
            "<html><body style='font-family:SansSerif;font-size:12px;color:#A8A8C8;width:760px'>" +
            "Como a atividade exige as amostras fornecidas pelo professor, o projeto foi centralizado nesses textos. " +
            "O benchmark oficial, os gráficos e o README foram direcionados para os textos em <b>data/samples/</b>." +
            "</body></html>");
        box.add(title, BorderLayout.NORTH);
        box.add(text, BorderLayout.CENTER);
        return box;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<File> extraFiles() {
        File dir = new File(SampleConfig.SAMPLE_DIR);
        if (!dir.exists()) return List.of();
        File[] all = dir.listFiles((d, n) -> n.toLowerCase().endsWith(".txt"));
        if (all == null) return List.of();
        Set<String> officialNames = SampleConfig.OFFICIAL_SAMPLES.stream()
                .map(s -> new File(s.filePath).getName().toLowerCase())
                .collect(Collectors.toSet());
        List<File> extras = new ArrayList<>();
        for (File f : all) {
            if (!officialNames.contains(f.getName().toLowerCase())) extras.add(f);
        }
        extras.sort(java.util.Comparator.comparing(File::getName));
        return extras;
    }
}
