package client.responses.runtime;

// No need to import DebugStateDTO here anymore.

import execution.debug.DebugStepDTO;

public final class DebugResults {
    private DebugResults() {}
    public record Stop(boolean stopped, String debugId) {}
    public record Terminated(boolean terminated, int creditsCurrent) {}
    public record History(boolean ok, int runNumber) {}
    public record StepResult( DebugStepDTO step, int creditsCurrent, int creditsUsed, boolean terminated) {}
    public record InitResult(boolean accepted, String debugId, int retryMs, boolean locked, int creditsCurrent) {}

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
