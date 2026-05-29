package gamelog.counters;

/**
 * Serial CPU counter — iterates over every line with a simple loop.
 * Baseline for speedup calculations.
 */
public class SerialCPUCounter implements WordCounter {

    @Override
    public long count(String[] lines, String word) {
        long count = 0;
        for (String line : lines) {
            if (line.equals(word)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String getName() {
        return "SerialCPU";
    }
}
