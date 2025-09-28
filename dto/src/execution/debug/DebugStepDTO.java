package execution.debug;

import java.util.List;

/**
 * DTO representing a single step in the debugging process of a program execution.
 * It contains information about the executed line, cycles delta, new state after execution,
 * and any deltas that occurred during this step.
 */
public final class DebugStepDTO {
    private final DebugStateDTO newState;

    public DebugStepDTO(int executedLine, long cyclesDelta, DebugStateDTO newState, List<DebugStepDTO> deltas) {
        this.newState = newState;
        List<DebugStepDTO> deltas1 = (deltas == null) ? List.of() : List.copyOf(deltas);
    }
    public DebugStateDTO getNewState() { return newState; }
}
