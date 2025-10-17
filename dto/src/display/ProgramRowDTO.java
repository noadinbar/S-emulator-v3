package display;

public class ProgramRowDTO {
    private String name;
    private String uploader;
    private int baseInstrCount;
    private int maxDegree;
    private int numRuns;
    private double avgCredits;

    public ProgramRowDTO() { } // for Gson

    public ProgramRowDTO(String name, String uploader, int baseInstrCount, int maxDegree, int numRuns, double avgCredits) {
        this.name = name;
        this.uploader = uploader;
        this.baseInstrCount = baseInstrCount;
        this.maxDegree = maxDegree;
        this.numRuns = numRuns;
        this.avgCredits = avgCredits;
    }

    public String getName() { return name; }
    public String getUploader() { return uploader; }
    public int getBaseInstrCount() { return baseInstrCount; }
    public int getMaxDegree() { return maxDegree; }
    public int getNumRuns() { return numRuns; }
    public double getAvgCredits() { return avgCredits; }
}
