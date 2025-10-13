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

    private final String userString; // null => תוכנית ראשית; אחרת userString של פונקציה
    private String debugId;          // מתקבל מ-init

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
        ensureId();
        // TODO: כשנוסיף /api/debug/terminate או שדה 'terminated' בתגובה של step, נעדכן.
        return false;
    }

    private void ensureId() {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalStateException("Debug session is not initialized (missing debugId). Call init() first.");
        }
    }
}
