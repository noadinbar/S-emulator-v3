package api;

import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;

public interface DebugAPI {
    DebugStateDTO init(ExecutionRequestDTO req);
    DebugStepDTO step();
    boolean isTerminated();
    void restore(DebugStateDTO snapshot);
}
