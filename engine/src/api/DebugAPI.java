package api;

import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;

import java.util.List;

public interface DebugAPI {
    DebugStateDTO init(ExecutionRequestDTO req);

    DebugStepDTO step();

    boolean isTerminated();

    void restore(DebugStateDTO snapshot);

    default void stop() {
    }

    default void resume() {
    }

    default DebugStateDTO resumeAndGetLastState() {
        resume();
        return null;
    }

    default void recordHistory(ExecutionRequestDTO request){}
}