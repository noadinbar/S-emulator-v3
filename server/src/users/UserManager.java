// users/UserManager.java
package users;

import users.UserTableRow;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all users as a map: userName -> row model.
 * No DTO/transport logic here. Thread-safe via ConcurrentHashMap and
 * per-row synchronization inside UserTableRow.
 */
public class UserManager {

    private final Map<String, UserTableRow> users = new ConcurrentHashMap<>();

    /** Add user if absent and return the row (existing or newly created). */
    public UserTableRow addIfAbsent(String name) {
        return users.computeIfAbsent(name, UserTableRow::new);
    }

    /** Returns true if a user exists. */
    public boolean exists(String name) {
        return users.containsKey(name);
    }

    /** Get the row for a user (or null if not present). */
    public UserTableRow get(String name) {
        return users.get(name);
    }

    /** A weakly-consistent live view of all rows (safe to iterate). */
    public Collection<UserTableRow> values() {
        return users.values();
    }

    public void remove(String name) {
        if (name == null) return;
        String key = name.trim();
        users.remove(key);
    }

    // --- Aggregation helpers to be called from application events ---

    /** Called when a main program is uploaded by this user. */
    public void onMainProgramUploaded(String name) {
        addIfAbsent(name).incMainPrograms();
    }

    /** Called when a helper function is uploaded by this user. */
    public void onFunctionUploaded(String name) {
        addIfAbsent(name).incFunctions();
    }

    /** Called when a run/debug is executed; updates runs and credits atomically per row. */
    public void onRunExecuted(String name, int creditsSpent) {
        addIfAbsent(name).recordRun(creditsSpent);
    }

    /** Adjust user's current/used credits (positive=top-up, negative=spend). */
    public void adjustCredits(String name, int delta) {
        addIfAbsent(name).adjustCredits(delta);
    }

    /** Clear all users (admin/testing only). */
    public void clear() {
        users.clear();
    }
}
