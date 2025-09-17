package exportToDTO;

import api.DebugAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
import display.Command2DTO;

import display.Command3DTO;
import exceptions.InvalidDegreeException;
import exceptions.StatePersistenceException;
import execution.HistoryDTO;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class DisplayAPIImpl implements DisplayAPI {
    private Program program;


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
    public ExecutionAPI execution() {
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

    @Override
    public void saveState(Path path) {
        try {
            if (!path.toString().toLowerCase().endsWith(".ser")) {
                path = Path.of(path.toString() + ".ser");
            }
            ProgramImpl impl = (ProgramImpl) program;
            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(path))) {
                out.writeObject(impl);
            }
        } catch (Exception e) {
            throw new StatePersistenceException("Failed to save state to: " + path, e);
        }
    }

    @Override
    public DisplayAPI loadState(Path path) {
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(path))) {
            ProgramImpl loaded = (ProgramImpl) in.readObject();
            this.program = loaded;
            return this;
        } catch (Exception e) {
            throw new StatePersistenceException("Failed to load state from: " + path, e);
        }
    }

    @Override
    public api.DebugAPI debugForDegree(int degree) {
        if (degree == 0) {
            ((ProgramImpl) program).setCurrentRunDegree(0);
            // expanded=original כשה-degree 0
            return new DebugAPIImpl(((ProgramImpl) program), ((ProgramImpl) program), 0);
        }
        int max = ((ProgramImpl) program).calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException("Degree must be between 0 and " + max);
        }
        var res = ProgramExpander.expandTo(program, degree);
        Program expanded = res.getExpandedProgram();
        return new DebugAPIImpl(((ProgramImpl) expanded), ((ProgramImpl) program), degree);
    }

}
