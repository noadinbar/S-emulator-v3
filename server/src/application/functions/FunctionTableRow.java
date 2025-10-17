package application.functions;

public class FunctionTableRow {

    private final String name;
    private final String programName;
    private final String uploader;
    private final int    baseInstrCount;
    private final int    maxDegree;

    public FunctionTableRow(String name,
                            String programName,
                            String uploader,
                            int baseInstrCount,
                            int maxDegree) {
        this.name = name;
        this.programName = programName;
        this.uploader = uploader;
        this.baseInstrCount = baseInstrCount;
        this.maxDegree = maxDegree;
    }

    public String getName()           { return name; }
    public String getProgramName()    { return programName; }
    public String getUploader()       { return uploader; }
    public int    getBaseInstrCount() { return baseInstrCount; }
    public int    getMaxDegree()      { return maxDegree; }
}
