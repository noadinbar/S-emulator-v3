package client.responses;

import execution.debug.DebugStateDTO;

public final class DebugResults {
    private DebugResults() {}
    public static record Init(String debugId, DebugStateDTO state) {}
    public static record Stop(boolean stoppedAll, boolean stopped, String debugId, int count) {}
    public static record Terminated(boolean terminated) {}
}
