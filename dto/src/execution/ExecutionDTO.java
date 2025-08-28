package execution;

import display.Command2DTO;
import java.util.List;

public class ExecutionDTO {
    private final long yValue;
    private final long totalCycles;
    private final List<VarValueDTO> finals;
    private final Command2DTO executedProgram;

    public ExecutionDTO(long yValue, long totalCycles, List<VarValueDTO> finals, Command2DTO executedProgram) {
        this.yValue = yValue;
        this.totalCycles = totalCycles;
        this.finals = finals;
        this.executedProgram = executedProgram;
    }

    public long getyValue() { return yValue; }
    public long getTotalCycles() { return totalCycles; }
    public List<VarValueDTO> getFinals() { return finals; }
    public Command2DTO getExecutedProgram() { return executedProgram; }
}
