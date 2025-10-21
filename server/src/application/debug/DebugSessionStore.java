package application.debug;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/** In-memory registry for per-user debug sessions. */
public final class DebugSessionStore {

    public enum State { READY, RUNNING, PAUSED, DONE, ERROR }

    private static final Map<String, DebugSession> SESSIONS = new ConcurrentHashMap<>();

    private DebugSessionStore() {}

    public static DebugSession create(String userId, String kind, String name, int degree, List<Long> inputs) {
        String id = UUID.randomUUID().toString();
        DebugSession s = new DebugSession(id, userId, kind, name, degree, inputs);
        SESSIONS.put(id, s);
        return s;
    }

    public static DebugSession get(String id) { return SESSIONS.get(id); }

    public static void remove(String id) { SESSIONS.remove(id); }
}
