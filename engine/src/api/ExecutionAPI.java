package api;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

public interface ExecutionAPI {
    int getMaxDegree();
    ExecutionDTO execute(ExecutionRequestDTO request);
    execution.debug.DebugStateDTO debugInit(execution.ExecutionRequestDTO req);
    execution.debug.DebugStepDTO debugStep(execution.debug.DebugStateDTO state);
    execution.ExecutionDTO debugResume(execution.debug.DebugStateDTO state);

}
