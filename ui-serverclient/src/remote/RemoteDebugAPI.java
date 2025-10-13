package remote;

import api.DebugAPI;
import client.requests.Debug;
import client.responses.DebugResponder;
import client.responses.DebugResults;
import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import okhttp3.Request;

public class RemoteDebugAPI implements DebugAPI {

    private final String userString;
    private String debugId;
    private volatile boolean terminated = false;

    public RemoteDebugAPI() { this(null); }
    public RemoteDebugAPI(String functionUserString) { this.userString = functionUserString; }

    @Override
    public DebugStateDTO init(ExecutionRequestDTO request) {
        try {
            Request httpReq = Debug.init(request, userString);
            DebugResults.Init res = DebugResponder.init(httpReq);
            this.debugId = res.debugId();
            return res.state();
        } catch (Exception e) {
            throw new RuntimeException("Debug init failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DebugStepDTO step() {
        ensureId();
        try {
            Request httpReq = Debug.step(debugId);
            return DebugResponder.step(httpReq);
        } catch (Exception e) {
            throw new RuntimeException("Debug step failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void restore(DebugStateDTO to) {
        throw new UnsupportedOperationException("Step Back is disabled (no /api/debug/restore endpoint).");
    }


    @Override
    public boolean isTerminated() {
        if (terminated) return true;
        if (debugId == null || debugId.isBlank()) return true;
        try {
            Request r = Debug.terminated(debugId);                   // <<< שם חדש
            DebugResults.Terminated s = DebugResponder.terminated(r); // <<< שם חדש
            this.terminated = s.terminated();
        } catch (Exception ignored) {}
        return this.terminated;
    }


    @Override
    public void stop() {
        if (debugId == null || debugId.isBlank()) return;
        try {
            Request req = Debug.stop(debugId);
            DebugResults.Stop res = DebugResponder.stop(req);
            if (res != null && res.debugId() != null && !res.debugId().isBlank()) {
                this.debugId = res.debugId();
            }
        } catch (Exception ignore) {
        }
    }

    @Override
    public DebugStateDTO resumeAndGetLastState() {
        ensureId();
        try {
            var res = DebugResponder.resume(Debug.resume(debugId));
            this.terminated = res.terminated();
            return res.lastState(); // מגיע מהסרבלט שלך
        } catch (Exception e) {
            throw new RuntimeException("Remote debug resume failed", e);
        }
    }

    private void ensureId() {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalStateException("Debug session is not initialized (missing debugId). Call init() first.");
        }
    }
}
