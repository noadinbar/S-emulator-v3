package application.debug;

import execution.debug.DebugStateDTO;
import execution.debug.DebugStepDTO;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/** A single debug session state. */
public final class DebugSession {
    public final String id;
    public final String userId;        // optional for future use
    public final String targetKind;    // "PROGRAM" / "FUNCTION"
    public final String targetName;    // function name if any
    public final int degree;
    public final List<Long> inputs;

    public final ReentrantLock lock = new ReentrantLock();
    public volatile DebugSessionStore.State state = DebugSessionStore.State.READY;
    public volatile String errorMsg = null;
    public volatile long updatedAt = System.currentTimeMillis();

    public DebugStateDTO lastSnapshot = null;
    public final Deque<DebugStepDTO> history = new ArrayDeque<>();

    public DebugSession(String id, String userId, String targetKind, String targetName, int degree, List<Long> inputs) {
        this.id = id;
        this.userId = userId;
        this.targetKind = targetKind;
        this.targetName = targetName;
        this.degree = degree;
        this.inputs = inputs;
    }
}