package utils;

import java.io.Serializable;
import java.util.List;

public final class RunHistory implements Serializable {
    private final int runNumber;
    private final int degree;
    private final List<Long> inputs;
    private final long yValue;
    private final int cycles;
    private static final long serialVersionUID = 1L;

    public RunHistory(int runNumber, int degree, List<Long> inputs, long yValue, int cycles) {
        this.runNumber = runNumber;
        this.degree = degree;
        this.inputs  = List.copyOf(inputs);
        this.yValue  = yValue;
        this.cycles  = cycles;
    }

    public int getRunNumber() { return runNumber; }
    public int getDegree()    { return degree; }
    public List<Long> getInputs() { return inputs; }
    public long getYValue()   { return yValue; }
    public int getCycles()    { return cycles; }

    public String formatForDisplay() {
        return String.format("#%d | degree=%d | inputs=%s | y=%d | cycles=%d",
                runNumber, degree, inputs, yValue, cycles);
    }
}