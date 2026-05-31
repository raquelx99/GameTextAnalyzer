package gamelog;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Configuração central das amostras de texto oficiais exigidas pelo trabalho.
 *
 * O benchmark oficial usa essas amostras literárias como "mundos narrativos"
 * para a análise de adaptação de jogos.
 */
public final class SampleConfig {

    public static final String DATA_DIR = "data";
    public static final String SAMPLE_DIR = DATA_DIR + "/samples";

    public final String filePath;
    public final String title;
    public final String gameWorld;
    public final String defaultWord;
    public final String[] suggestedWords;

    private SampleConfig(String fileName, String title, String gameWorld,
                         String defaultWord, String... suggestedWords) {
        this.filePath = SAMPLE_DIR + "/" + fileName;
        this.title = title;
        this.gameWorld = gameWorld;
        this.defaultWord = normalize(defaultWord);
        this.suggestedWords = Arrays.stream(suggestedWords)
                .map(SampleConfig::normalize)
                .toArray(String[]::new);
    }

    public static final SampleConfig DON_QUIXOTE = new SampleConfig(
            "DonQuixote-388208.txt",
            "Don Quixote",
            "RPG de Cavalaria e Aventura",
            "quijote",
            "quijote", "sancho", "caballero", "aventura", "castillo", "dulcinea"
    );

    public static final SampleConfig DRACULA = new SampleConfig(
            "Dracula-165307.txt",
            "Dracula",
            "Survival Horror Gotico",
            "blood",
            "dracula", "vampire", "blood", "night", "fear", "castle", "helsing"
    );

    public static final SampleConfig MOBY_DICK = new SampleConfig(
            "MobyDick-217452.txt",
            "Moby Dick",
            "Aventura Naval / Boss Hunt",
            "whale",
            "whale", "sea", "ship", "captain", "ahab", "harpoon", "moby"
    );

    public static final List<SampleConfig> OFFICIAL_SAMPLES = List.of(
            DON_QUIXOTE,
            DRACULA,
            MOBY_DICK
    );

    public static String normalize(String word) {
        return word == null ? "" : word.trim().toLowerCase();
    }

    public static void ensureSampleDirectory() {
        new File(SAMPLE_DIR).mkdirs();
    }

    public static String[][] officialBenchmarkEntries() {
        String[][] entries = new String[OFFICIAL_SAMPLES.size()][2];
        for (int i = 0; i < OFFICIAL_SAMPLES.size(); i++) {
            SampleConfig sample = OFFICIAL_SAMPLES.get(i);
            entries[i][0] = sample.filePath;
            entries[i][1] = sample.defaultWord;
        }
        return entries;
    }

    public static String suggestedWordsText() {
        StringBuilder sb = new StringBuilder();
        for (SampleConfig sample : OFFICIAL_SAMPLES) {
            sb.append(sample.title).append(" (").append(sample.gameWorld).append("): ");
            sb.append(String.join(", ", sample.suggestedWords)).append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static SampleConfig findByFileName(String name) {
        if (name == null) return null;
        for (SampleConfig sample : OFFICIAL_SAMPLES) {
            if (new File(sample.filePath).getName().equalsIgnoreCase(new File(name).getName())) {
                return sample;
            }
        }
        return null;
    }

    private SampleConfig() {
        throw new AssertionError("Utility class");
    }
}
