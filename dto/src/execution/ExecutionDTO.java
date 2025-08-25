package execution;

import display.Command2DTO;
import java.util.List;

/** פלט הרצה סופי: y, כל המשתנים בסיום, cycles, ותצוגת התוכנית שבוצעה בפועל */
public class ExecutionDTO {
    private final long yValue;
    private final long totalCycles;
    private final List<VarValueDTO> finals; // y, כל x בסדר עולה, ואז כל z בסדר עולה
    private final Command2DTO executedProgram; // להצגה לפי כללי פקודות 2/3

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
