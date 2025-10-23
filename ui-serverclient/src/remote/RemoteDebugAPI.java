package remote;

import api.DebugAPI;
import client.requests.runtime.Debug;
import client.responses.runtime.DebugResponder;
import client.responses.runtime.DebugResults;
import execution.ExecutionRequestDTO;
import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;
import okhttp3.Request;

public class RemoteDebugAPI implements DebugAPI {

    private final String userString;     // optional function user-string
    private String debugId;              // assigned on accepted init
    private volatile boolean terminated; // cached flag from /terminated

    public RemoteDebugAPI() { this(null); }
    public RemoteDebugAPI(String functionUserString) { this.userString = functionUserString; }

    // ---------------- Legacy signatures (kept to match DebugAPI) ----------------

    /** Legacy init now returns null immediately (init is async). Controllers should call submitInit() then state(). */
    @Override
    public DebugStateDTO init(ExecutionRequestDTO req) {
        submitInit(req); // fire-and-return; debugId will be set if accepted
        return null;     // no blocking here; state() should be polled by controllers
    }

    /** Single step remains synchronous; updates the local 'terminated' flag best-effort. */
    @Override
    public DebugStepDTO step() {
        ensureId();
        try {
            Request httpReq = Debug.step(debugId);
            DebugStepDTO dto = DebugResponder.step(httpReq);

            // Refresh local terminated flag (best-effort; does not advance the session)
            try {
                Request termReq = Debug.terminated(debugId);
                var t = DebugResponder.terminated(termReq);
                this.terminated = t.terminated();
            } catch (Exception ignore) {}

            return dto;
        } catch (Exception e) {
            throw new RuntimeException("Debug step failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isTerminated() {
        if (terminated) return true;
        if (debugId == null || debugId.isBlank()) return true;
        try {
            Request r = Debug.terminated(debugId);
            var s = DebugResponder.terminated(r);
            this.terminated = s.terminated();
        } catch (Exception ignored) {}
        return this.terminated;
    }

    @Override
    public void restore(DebugStateDTO to) {
        throw new UnsupportedOperationException("Step Back is disabled (no /api/debug/restore endpoint).");
    }

    @Override
    public void stop() {
        if (debugId == null || debugId.isBlank()) return;
        try {
            Request req = Debug.stop(debugId);
            DebugResponder.stop(req);
        } catch (Exception ignore) { }
    }

    /** Legacy resume is void in the interface — keep it void.
     *  Controllers should move to submitResume() + polling state()/terminated().
     */
    @Override
    public void resume() {
        ensureId();
        try {
            Request req = Debug.resume(debugId);
            // We intentionally ignore the Submit result here to keep the legacy signature.
            DebugResponder.resume(req);
        } catch (Exception e) {
            throw new RuntimeException("Remote debug resume failed: " + e.getMessage(), e);
        }
    }

    @Override
    public void recordHistory(ExecutionRequestDTO request) {
        try {
            Request httpReq = Debug.history(request);
            DebugResults.History res = DebugResponder.history(httpReq);
            if (!res.ok()) throw new RuntimeException("recordHistory failed");
        } catch (Exception e) {
            throw new RuntimeException("recordHistory failed: " + e.getMessage(), e);
        }
    }

    // ---------------- New async-friendly overrides ----------------

    @Override
    public Submit submitInit(ExecutionRequestDTO request) {
        try {
            Request httpReq = Debug.init(request, userString);
            DebugResults.Submit r = DebugResponder.init(httpReq);

            if (r.accepted() && r.debugId() != null && !r.debugId().isBlank()) {
                this.debugId = r.debugId();
                this.terminated = false;
            }

            if (r.accepted()) return Submit.accepted(r.debugId());
            if (r.locked())   return Submit.locked();     // not expected on init, but safe
            return Submit.busy(r.retryMs());

        } catch (Exception e) {
            throw new RuntimeException("Debug submitInit failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Submit submitResume() {
        ensureId();
        try {
            Request req = Debug.resume(debugId);
            DebugResults.Submit r = DebugResponder.resume(req);

            if (r.accepted()) return Submit.accepted(debugId);
            if (r.locked())   return Submit.locked();
            return Submit.busy(r.retryMs());

        } catch (Exception e) {
            throw new RuntimeException("Debug submitResume failed: " + e.getMessage(), e);
        }
    }

    @Override
    public DebugStateDTO state() {
        ensureId();
        try {
            return DebugResponder.state(Debug.state(debugId));
        } catch (Exception e) {
            throw new RuntimeException("Debug state fetch failed: " + e.getMessage(), e);
        }
    }

    // ---------------- helpers ----------------

    private void ensureId() {
        if (debugId == null || debugId.isBlank()) {
            throw new IllegalStateException("Debug session is not initialized (missing debugId). Call init()/submitInit() first.");
        }
    }
}
