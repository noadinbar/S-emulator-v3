package api;

import display.DisplayDTO;
import display.ExpandDTO;
import execution.HistoryDTO;

import java.nio.file.Path;

public interface DisplayAPI {
    DisplayDTO getCommand2();
    ExpandDTO expand(int degree);
    ExecutionAPI execution();
    ExecutionAPI executionForDegree(int degree);
    HistoryDTO getHistory();
    void saveState(Path path);
    DisplayAPI loadState(Path path);
    DebugAPI debugForDegree(int degree);
}
