package execution;

import display.DisplayDTO;
import java.util.List;

public class ExecutionDTO {
    private final long yValue;
    private final long totalCycles;
    private final List<VarValueDTO> finals;
    private final DisplayDTO executedProgram;

    public ExecutionDTO(long yValue, long totalCycles, List<VarValueDTO> finals, DisplayDTO executedProgram) {
        this.yValue = yValue;
        this.totalCycles = totalCycles;
        this.finals = finals;
        this.executedProgram = executedProgram;
    }

    public long getyValue() { return yValue; }
    public long getTotalCycles() { return totalCycles; }
    public List<VarValueDTO> getFinals() { return finals; }
    public DisplayDTO getExecutedProgram() { return executedProgram; }
}
