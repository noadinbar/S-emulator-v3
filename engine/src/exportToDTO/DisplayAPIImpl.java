package exportToDTO;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.DisplayDTO;
import api.DebugAPI;

import display.ExpandDTO;
import exceptions.InvalidDegreeException;
import exceptions.StatePersistenceException;
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
import java.util.*;


public class DisplayAPIImpl implements DisplayAPI {
    private Program program;
    private Map<String, DisplayAPI> functionToDisplayAPI;

    public DisplayAPIImpl(Program program) { this.program = program; }

    @Override
    public DisplayDTO getDisplay() { return DisplayMapper.toCommand2(program); }

    @Override
    public ExpandDTO expand(int degree) {
        int max = program.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException(
                    "Degree must be between 0 and " + max
            );
        }
        return ExpandMapper.toCommand3(program,degree, max);
    }

    @Override
    public ExecutionAPI execution() {
        ((ProgramImpl) program).setCurrentRunDegree(0);
        return new ExecutionAPIImpl(((ProgramImpl) program), ((ProgramImpl) program));
    }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        if (degree == 0) {
            return execution();
        }
        int max = program.calculateMaxDegree();
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
            // expanded=original degree 0
            return new DebugAPIImpl(((ProgramImpl) program), ((ProgramImpl) program), 0);
        }
        int max = program.calculateMaxDegree();
        if (degree < 0 || degree > max) {
            throw new InvalidDegreeException("Degree must be between 0 and " + max);
        }
        ExpandResult res = ProgramExpander.expandTo(program, degree);
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

    public List<String> listFunctionUserStrings() {
        List<String> result = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        List<Function> funcs = program.getFunctions();
        for (Function f : funcs) {
            if (f == null) {
                continue;
            }
            String us = f.getUserString();
            if (us == null || us.isBlank()) {
                continue;
            }
            if (seen.add(us)) {
                result.add(us);
            }
        }

        return result;
    }

    /**
     * Returns the Function object with the given userString, or null if not found.
     */
    public Function findFunctionByUserString(String userString) {
        if (userString == null || userString.isBlank()) {
            return null;
        }

        List<Function> funcs = program.getFunctions();
        for (Function f : funcs) {
            if (f == null) {
                continue;
            }
            String us = f.getUserString();
            if (us != null && us.equals(userString)) {
                return f;
            }
        }

        return null;
    }

    /**
     * Attaches all functions from the given list into this program
     * if they are not already present (by userString).
     * This mutates only this program. The server does not call program.addFunction() directly.
     */
    public void attachFunctions(List<Function> externalFunctions) {
        if (externalFunctions == null || externalFunctions.isEmpty()) {
            return;
        }

        // collect existing userStrings so we do not add duplicates
        Set<String> existing = new LinkedHashSet<>(listFunctionUserStrings());

        for (Function f : externalFunctions) {
            if (f == null) {
                continue;
            }
            String us = f.getUserString();
            if (us == null || us.isBlank()) {
                continue;
            }
            if (!existing.contains(us)) {
                // add to program and mark as existing
                program.addFunction(f);
                existing.add(us);
            }
        }
    }
}
