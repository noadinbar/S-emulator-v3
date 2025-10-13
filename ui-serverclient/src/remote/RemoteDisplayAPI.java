package remote;

import api.DebugAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
import client.requests.History;
import client.responses.ExpandResponder;
import client.responses.FunctionsResponder;
import client.responses.HistoryResponder;
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
        if (display == null && name != null) {
            try {
                display = FunctionsResponder.program(name);
            } catch (Exception e) {
                throw new RuntimeException("Loading function program failed: " + e.getMessage(), e);
            }
        }
        return display;
    }

    @Override
    public ExpandDTO expand(int degree) {
        try {
            return (name == null || name.isBlank())
                    ? ExpandResponder.execute(degree)
                    : ExpandResponder.execute(name, degree);
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
                out.put(k, new RemoteDisplayAPI(null, k));
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("Functions list failed: " + e.getMessage(), e);
        }
    }

    @Override
    public ExecutionAPI execution() {
        return new RemoteExecutionAPI(name);
    }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        return new RemoteExecutionAPI(name);
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

    @Override public void saveState(Path path) { }
    @Override public DisplayAPI loadState(Path path) { return this; }
    @Override
    public DebugAPI debugForDegree(int degree) {
        return new RemoteDebugAPI(name);
    }

}
