package utils;

import java.util.List;

public final class RunHistory {
    private final int runNumber;      // מספר ריצה (מתחיל מ-1)
    private final int degree;         // דרגת הריצה (כפי שהוגדרה לפני ההרצה)
    private final List<Long> inputs;  // הקלטים לפי הסדר: x1, x2, ...
    private final long yValue;        // ערך y בסיום
    private final int cycles;         // סה"כ cycles שנצרכו

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