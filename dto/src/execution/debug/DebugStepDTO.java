package execution.debug;

import java.util.List;

/**
 * DTO representing a single step in the debugging process of a program execution.
 * It contains information about the executed line, cycles delta, new state after execution,
 * and any deltas that occurred during this step.
 */
public final class DebugStepDTO {
    private final int executedLine;
    private final long cyclesDelta;
    private final DebugStateDTO newState;
    private final List<DebugStepDTO> deltas;

    public DebugStepDTO(int executedLine, long cyclesDelta, DebugStateDTO newState, List<DebugStepDTO> deltas) {
        this.executedLine = executedLine;
        this.cyclesDelta = cyclesDelta;
        this.newState = newState;
        this.deltas = (deltas == null) ? List.of() : List.copyOf(deltas); // אימיוטבילי
    }

    public int getExecutedLine() { return executedLine; }
    public long getCyclesDelta() { return cyclesDelta; }
    public DebugStateDTO getNewState() { return newState; }
    public List<DebugStepDTO> getDeltas() { return deltas; }
}
