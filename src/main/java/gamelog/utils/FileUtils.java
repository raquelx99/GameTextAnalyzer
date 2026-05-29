package gamelog.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for reading text files.
 *
 * The assignment samples are full literary texts, not one-event-per-line logs.
 * For this reason the official benchmark uses tokenization: each word becomes
 * one item in the returned array. The same tokenizer also works for the optional
 * synthetic gameplay logs because PLAYER_MOVE remains a single token.
 */
public class FileUtils {

    /**
     * Reads a text file and returns normalized word tokens.
     *
     * Normalization applied:
     *  - lowercase;
     *  - punctuation becomes spaces;
     *  - underscores are preserved for synthetic gameplay events;
     *  - accents are removed to make searches easier in Portuguese/Spanish text.
     */
    public static String[] readLines(String path) throws IOException {
        return readTokens(path);
    }

    public static String[] readTokens(String path) throws IOException {
        String text = Files.readString(Path.of(path), StandardCharsets.UTF_8);
        return tokenize(text);
    }

    public static String[] tokenize(String text) {
        if (text == null || text.isBlank()) return new String[0];

        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9_]+", " ")
                .trim();

        if (normalized.isEmpty()) return new String[0];
        return normalized.split("\\s+");
    }

    /**
     * Streams tokens with lower memory overhead. Currently not used by counters,
     * but useful for future improvements.
     */
    public static List<String> readTokenList(String path) throws IOException {
        String[] tokens = readTokens(path);
        List<String> list = new ArrayList<>(tokens.length);
        java.util.Collections.addAll(list, tokens);
        return list;
    }

    /** Normalizes a query word with the same rules used for file tokens. */
    public static String normalizeQuery(String query) {
        String[] tokens = tokenize(query == null ? "" : query);
        return tokens.length == 0 ? "" : tokens[0];
    }
}
