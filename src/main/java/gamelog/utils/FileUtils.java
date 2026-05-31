package gamelog.utils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

/**
 * Métodos utilitários para leitura de arquivos de texto.
 *
 * As amostras do trabalho são textos literários completos, não logs com um evento por linha.
 * Por isso o benchmark oficial usa tokenização: cada palavra se torna
 * um item no array retornado. O mesmo tokenizador também funciona para os logs
 * sintéticos de gameplay opcionais, pois PLAYER_MOVE permanece como um único token.
 */
public class FileUtils {

    /**
     * Lê um arquivo de texto e retorna tokens de palavras normalizados.
     *
     * Normalização aplicada:
     *  - letras minúsculas;
     *  - pontuação é convertida em espaços;
     *  - underscores são preservados para eventos sintéticos de gameplay;
     *  - acentos são removidos para facilitar buscas em texto português/espanhol.
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
     * Fluxo de tokens com menor overhead de memória. Atualmente não é usado pelos contadores,
     * mas útil para melhorias futuras.
     */
    public static List<String> readTokenList(String path) throws IOException {
        String[] tokens = readTokens(path);
        List<String> list = new ArrayList<>(tokens.length);
        java.util.Collections.addAll(list, tokens);
        return list;
    }

    /** Normaliza uma palavra de query com as mesmas regras usadas para tokens de arquivo. */
    public static String normalizeQuery(String query) {
        String[] tokens = tokenize(query == null ? "" : query);
        return tokens.length == 0 ? "" : tokens[0];
    }
}
