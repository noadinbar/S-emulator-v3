package client.responses;

import execution.debug.DebugStateDTO;

public final class DebugResults {
    private DebugResults() {}
    public record Init(String debugId, DebugStateDTO state) {}
    public record Stop(boolean stopped, String debugId) {}
    public record Terminated(boolean terminated) {}
    public record Resume(boolean terminated, int steps, DebugStateDTO lastState, String debugId) {}
    public record History(boolean ok, int runNumber) {}
}
