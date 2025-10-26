package remote;

import api.DebugAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
import client.requests.runtime.History;
import client.responses.info.FunctionsResponder;
import client.responses.runtime.ExpandResponder;
import client.responses.runtime.HistoryResponder;
import display.DisplayDTO;
import display.ExpandDTO;
import execution.HistoryDTO;
import okhttp3.Request;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RemoteDisplayAPI implements DisplayAPI {
    private DisplayDTO display;
    private final String name;

    public RemoteDisplayAPI(DisplayDTO display, String userString) {
        this.display = display;
        this.name = userString;
    }

    @Override
    public DisplayDTO getDisplay() {
        ensureDisplayLoaded();
        return display;
    }

    @Override
    public ExpandDTO expand(int degree) {
        // 'display' tells us the logical parent program name.
        ensureDisplayLoaded();
        String programLogicalName = (display != null) ? display.getProgramName() : null;

        // If this RemoteDisplayAPI represents the full program,
        // 'name' will equal the program's name.
        boolean isProgramView = false;
        if (programLogicalName != null && name != null && programLogicalName.equals(name)) {
            isProgramView = true;
        }

        try {
            if (isProgramView) {
                // Expand whole program
                return ExpandResponder.expandProgram(programLogicalName, degree);
            } else {
                // Expand specific function
                return ExpandResponder.expandFunction(name, degree);
            }
        } catch (Exception e) {
            throw new RuntimeException("Expand failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Map<String, DisplayAPI> functionDisplaysByUserString() {
        try {
            List<String> keys = FunctionsResponder.list();
            Map<String, DisplayAPI> out = new LinkedHashMap<>();
            for (String k : keys) {
                // each function gets its own RemoteDisplayAPI wrapper, keyed by its user-string
                out.put(k, new RemoteDisplayAPI(null, k));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Functions list failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecutionAPI execution() {
        ensureDisplayLoaded();

        String programName = (display != null) ? display.getProgramName() : null;
        if (programName == null) {
            programName = name;
        }

        // If this RemoteDisplayAPI is a whole program: name == programName.
        // Otherwise this RemoteDisplayAPI is a function.
        if (programName != null && programName.equals(name)) {
            return new RemoteExecutionAPI(programName);
        } else {
            return new RemoteExecutionAPI(programName, name);
        }
    }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        // degree does not affect which ExecutionAPI we build on the client
        ensureDisplayLoaded();

        String programName = (display != null) ? display.getProgramName() : null;
        if (programName == null) {
            programName = name;
        }

        if (programName != null && programName.equals(name)) {
            return new RemoteExecutionAPI(programName);
        } else {
            return new RemoteExecutionAPI(programName, name);
        }
    }

    @Override
    public HistoryDTO getHistory() {
        try {
            Request req = History.build(name);
            return HistoryResponder.get(req);
        } catch (Exception e) {
            throw new RuntimeException("History failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void saveState(Path path) { }

    @Override
    public DisplayAPI loadState(Path path) { return this; }

    @Override
    public DebugAPI debugForDegree(int degree) {
        ensureDisplayLoaded();

        String programName = (display != null) ? display.getProgramName() : null;
        // 'name' is the function user-string when this RemoteDisplayAPI wraps a function.
        // If this RemoteDisplayAPI represents the full program, 'name' will usually match programName.
        return new RemoteDebugAPI(programName, name);
    }

    /**
     * Make sure 'display' is populated.
     * For a program this loads the program's DisplayDTO.
     * For a function this loads the function's DisplayDTO (scoped version).
     */
    private void ensureDisplayLoaded() {
        if (display == null && name != null) {
            try {
                display = FunctionsResponder.program(name);
            } catch (Exception e) {
                throw new RuntimeException("Loading program failed: " + e.getMessage(), e);
            }
        }
    }
}
