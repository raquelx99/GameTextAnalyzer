package gamelog.counters;

/**
 * Contador serial de CPU - itera sobre cada token com um loop simples.
 * Baseline para os cálculos de speedup.
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

    @Override
    public StrategyFamily getFamily() {
        return StrategyFamily.SERIAL;
    }
}
