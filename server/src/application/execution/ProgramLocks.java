package application.execution;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

public final class ProgramLocks {
    private static final ConcurrentHashMap<String, ReadWriteLock> LOCKS = new ConcurrentHashMap<>();
    private ProgramLocks() {}
    public static ReadWriteLock lockFor(String programName) {
        return LOCKS.computeIfAbsent(programName, k -> new ReentrantReadWriteLock());
    }
}