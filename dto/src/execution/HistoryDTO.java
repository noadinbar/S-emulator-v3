package execution;

import java.util.List;

public class HistoryDTO {
    private final String programName;
    private final int totalRuns;
    private final List<RunHistoryEntryDTO> entries;

    public HistoryDTO(String programName, List<RunHistoryEntryDTO> entries) {
        this.programName = programName;
        this.entries = entries;
        this.totalRuns = (entries == null) ? 0 : entries.size();
    }

    public String getProgramName() { return programName; }
    public int getTotalRuns() { return totalRuns; }
    public List<RunHistoryEntryDTO> getEntries() { return entries; }
}
