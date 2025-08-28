package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.Command2DTO;

import display.Command3DTO;
import execution.HistoryDTO;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.program.Program;

public class DisplayAPIImpl implements DisplayAPI {
    private final Program program;
    public DisplayAPIImpl(Program program) { this.program = program; }

    @Override
    public Command2DTO getCommand2() { return DisplayMapper.toCommand2(program); }

    @Override
    public Command3DTO expand(int degree) {               // ← חדש
        return ExpandMapper.toCommand3(program, Math.max(0, degree));
    }

    @Override
    public api.ExecutionAPI execution() {
        return new ExecutionAPIImpl(program);
    }

    @Override
    public HistoryDTO getHistory() { return HistoryMapper.toHistory(program); }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        if (degree == 0) {
            return execution(); // בסיסי
        }
        // ההרחבה נעשית בתוך ה-engine, ה-UI לא רואה Program
        ExpandResult res = ProgramExpander.expandTo(program, degree);
        Program expanded = res.getExpandedProgram();
        return new ExecutionAPIImpl(expanded);
    }
}
