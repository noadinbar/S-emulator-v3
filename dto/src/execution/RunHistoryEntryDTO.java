package execution;

import java.util.List;

public class RunHistoryEntryDTO {
    private final int runNumber;
    private final int degree;
    private final List<Long> inputs;
    private final long yValue;
    private final int cycles;

    public RunHistoryEntryDTO(int runNumber, int degree, List<Long> inputs, long yValue, int cycles) {
        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs = inputs;
        this.yValue = yValue;
        this.cycles = cycles;
    }

    public int getRunNumber() { return runNumber; }
    public int getDegree() { return degree; }
    public List<Long> getInputs() { return inputs; }
    public long getYValue() { return yValue; }
    public int getCycles() { return cycles; }
}
