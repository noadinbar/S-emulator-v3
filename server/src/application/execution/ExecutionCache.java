package application.execution;

import api.DisplayAPI;
import api.ExecutionAPI;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches the heavy, input-independent "executionForDegree" result
 * per (target display, function user-string, degree).
 *
 * We DO NOT cache outputs. Each execute(...) still computes fresh results from inputs.
 * This assumes ExecutionAPI is safe to reuse concurrently (it creates a fresh runner per call).
 */
public final class ExecutionCache {
    private ExecutionCache() {}

    private static final ConcurrentHashMap<String, ExecutionAPI> CACHE = new ConcurrentHashMap<>();

    /** Clear all cached entries (e.g., after repository changes). */
    public static void clearAll() {
        CACHE.clear();
    }

    /** Get cached ExecutionAPI or build and cache it atomically. */
    public static ExecutionAPI getOrCompute(
            DisplayAPI target,
            int degree,
            Supplier<ExecutionAPI> builder
    ) {
        final String key = buildKey(target, degree);
        return CACHE.computeIfAbsent(key, k -> builder.get());
    }

    private static String buildKey(DisplayAPI target, int degree) {
             int id = System.identityHashCode(target);
             return id + "|" + degree;
        }

}
