package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.DisplayDTO;
import api.DebugAPI;

import display.ExpandDTO;
import exceptions.InvalidDegreeException;
import exceptions.StatePersistenceException;
import execution.HistoryDTO;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.function.Function;
import structure.instruction.Instruction;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;


public class DisplayAPIImpl implements DisplayAPI {
    private Program program;
    private Map<String, DisplayAPI> functionToDisplayAPI;

    public DisplayAPIImpl(Program program) { this.program = program; }

    @Override
    public DisplayDTO getCommand2() { return DisplayMapper.toCommand2(program); }

    @Override
    public ExpandDTO expand(int degree) {
        int max = program.calculateMaxDegree();
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
            this.program = (ProgramImpl) in.readObject();
            return this;
        } catch (Exception e) {
            throw new StatePersistenceException("Failed to load state from: " + path, e);
        }
    }

    @Override
    public DebugAPI debugForDegree(int degree) {
        if (degree == 0) {
            ((ProgramImpl) program).setCurrentRunDegree(0);
            // expanded=original כשה-degree 0
            return new DebugAPIImpl(((ProgramImpl) program), ((ProgramImpl) program), 0);
        }
        int max = program.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException("Degree must be between 0 and " + max);
        }
        var res = ProgramExpander.expandTo(program, degree);
        Program expanded = res.getExpandedProgram();
        return new DebugAPIImpl(((ProgramImpl) expanded), ((ProgramImpl) program), degree);
    }

    @Override
    public Map<String, DisplayAPI> functionDisplaysByUserString() {
        if (functionToDisplayAPI != null) return functionToDisplayAPI;

        Map<String, DisplayAPI> map = new LinkedHashMap<>();
        if (program != null && program.getFunctions() != null) {
            for (Function function : program.getFunctions()) {
                if (function == null) continue;
                String userString = function.getUserString();
                ProgramImpl fnProgram = new ProgramImpl(" F: " + userString);
                for (Instruction ins : function.getInstructions()) {
                    fnProgram.addInstruction(ins);
                }
                for (Function fn : program.getFunctions()) {
                        fnProgram.addFunction(fn);
                }
                map.put(userString, new DisplayAPIImpl(fnProgram));
            }
        }
        functionToDisplayAPI = Collections.unmodifiableMap(map);
        return functionToDisplayAPI;
    }
}
