package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.Command2DTO;

import display.Command3DTO;
import exceptions.InvalidDegreeException;
import execution.HistoryDTO;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.program.Program;
import structure.program.ProgramImpl;


public class DisplayAPIImpl implements DisplayAPI {
    private final Program program;

    public DisplayAPIImpl(Program program) { this.program = program; }

    @Override
    public Command2DTO getCommand2() { return DisplayMapper.toCommand2(program); }

    @Override
    public Command3DTO expand(int degree) {
        int max = ((ProgramImpl) program).calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException(
                    "Degree must be between 0 and " + max
            );
        }
        return ExpandMapper.toCommand3(program,degree);
    }

    @Override
    public api.ExecutionAPI execution() {
        ((ProgramImpl) program).setCurrentRunDegree(0);
        return new ExecutionAPIImpl(((ProgramImpl) program), ((ProgramImpl) program));
    }

    @Override
    public HistoryDTO getHistory() { return HistoryMapper.toHistory(program); }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        if (degree == 0) {
            return execution();
        }
        int max = ((ProgramImpl) program).calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException(
                    "Degree must be between 0 and " + max
            );
        }
        ((ProgramImpl) program).setCurrentRunDegree(degree);

        ExpandResult res = ProgramExpander.expandTo(program, degree);
        Program expanded = res.getExpandedProgram();
        return new ExecutionAPIImpl(((ProgramImpl) expanded), ((ProgramImpl) program));
    }
}
