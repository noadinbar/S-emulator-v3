package display;

/**
 * Shared DTO for the Functions table (server ↔ client via Gson).
 * Field names match TableColumn PropertyValueFactory keys exactly.
 *
 * Columns:
 * - name
 * - programName
 * - uploader
 * - baseInstrCount
 * - maxDegree
 */
public class FunctionRowDTO {
    private String name;
    private String programName;
    private String uploader;
    private int    baseInstrCount;
    private int    maxDegree;

    public FunctionRowDTO() { }

    public FunctionRowDTO(String name,
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
