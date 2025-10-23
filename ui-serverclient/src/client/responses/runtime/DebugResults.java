package client.responses.runtime;

// No need to import DebugStateDTO here anymore.

public final class DebugResults {
    private DebugResults() {}
    public record Stop(boolean stopped, String debugId) {}
    public record Terminated(boolean terminated) {}
    public record History(boolean ok, int runNumber) {}
    /** Generic async submit result for 202/429/409 */
    public record Submit(boolean accepted, String debugId, int retryMs, boolean locked) {}
    public static Submit accepted(String id) {
        return new Submit(true, id, 0, false);
    }
    public static Submit busy(int retryMs) {
        return new Submit(false, null, Math.max(0, retryMs), false);
    }
    public static Submit locked() {
        return new Submit(false, null, 0, true);
    }
}
