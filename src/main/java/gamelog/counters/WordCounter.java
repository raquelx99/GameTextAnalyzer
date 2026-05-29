package gamelog.counters;

/**
 * Common interface for all word-counting strategies.
 */
public interface WordCounter {

    /**
     * Counts occurrences of {@code word} in the given text lines.
     *
     * @param lines array of lines read from the log file
     * @param word  event/word to count
     * @return number of occurrences found
     */
    long count(String[] lines, String word);

    /** Human-readable name for this counter (used in reports). */
    String getName();
}
