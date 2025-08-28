package api;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

public interface ExecutionAPI {
    int getMaxDegree();
    ExecutionDTO execute(ExecutionRequestDTO request);
}
