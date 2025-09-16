package api;

import display.Command2DTO;
import display.Command3DTO;
import execution.HistoryDTO;

import java.nio.file.Path;

public interface DisplayAPI {
    Command2DTO getCommand2();
    Command3DTO expand(int degree);
    ExecutionAPI execution();
    ExecutionAPI executionForDegree(int degree);
    HistoryDTO getHistory();
    void saveState(Path path);
    DisplayAPI loadState(Path path);
    DebugAPI debugForDegree(int degree);
}
