package api;

import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;

public interface DebugAPI {
    // -------- Existing legacy signatures (unchanged) --------
    DebugStateDTO init(ExecutionRequestDTO req);
    DebugStepDTO step();
    boolean isTerminated();
    void restore(DebugStateDTO snapshot);

    default void stop() {}
    default void resume() {}

    default DebugStateDTO resumeAndGetLastState() {
        resume();
        return null;
    }

    default void recordHistory(ExecutionRequestDTO request){}

    // -------- New async-friendly defaults (optional to override) --------

    /** Async submit for init: returns 202/429 semantics (accepted/busy). */
    default Submit submitInit(ExecutionRequestDTO req) {
        throw new UnsupportedOperationException("submitInit is not supported by this DebugAPI");
    }

    /** Async submit for resume: returns 202/429/409 semantics. */
    default Submit submitResume() {
        throw new UnsupportedOperationException("submitResume is not supported by this DebugAPI");
    }

    /** Read-only snapshot (GET /api/debug/state). May return null if no snapshot yet or unknown id. */
    default DebugStateDTO state() {
        throw new UnsupportedOperationException("state() is not supported by this DebugAPI");
    }

    // -------- Small internal DTO to avoid new external types --------
    final class Submit {
        private final boolean accepted;
        private final String debugId;
        private final int retryMs;
        private final boolean locked;

        public Submit(boolean accepted, String debugId, int retryMs, boolean locked) {
            this.accepted = accepted;
            this.debugId = debugId;
            this.retryMs = Math.max(0, retryMs);
            this.locked = locked;
        }

        public boolean accepted() { return accepted; }
        public String  debugId()  { return debugId; }
        public int     retryMs()  { return retryMs; }
        // Factory helpers
        public static Submit accepted(String id) { return new Submit(true, id, 0, false); }
        public static Submit busy(int retryMs)   { return new Submit(false, null, retryMs, false); }
        public static Submit locked()            { return new Submit(false, null, 0, true); }
    }
}
