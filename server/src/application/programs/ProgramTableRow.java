package application.programs;

public class ProgramTableRow {
    private final String name;
    private final String uploader;
    private final int baseInstrCount;
    private final int maxDegree;
    private int runCount;
    private double avgCredits;

    public ProgramTableRow(String name, String uploader, int baseInstrCount, int maxDegree) {
        this.name = name;
        this.uploader = uploader;
        this.baseInstrCount = baseInstrCount;
        this.maxDegree = maxDegree;
        this.runCount = 0;
        this.avgCredits = 0.0;
    }

    public synchronized void onExecuted(double creditsCost) {
        if (creditsCost < 0) creditsCost = 0;
        avgCredits = (avgCredits * runCount + creditsCost) / (runCount + 1);
        runCount++;
    }

    public String getName() { return name; }
    public String getUploader() { return uploader; }
    public int getBaseInstrCount() { return baseInstrCount; }
    public int getMaxDegree() { return maxDegree; }
    public int getRunCount() { return runCount; }
    public double getAvgCredits() { return avgCredits; }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public void setAvgCredits(double avgCredits) {
        this.avgCredits = avgCredits;
    }
}
