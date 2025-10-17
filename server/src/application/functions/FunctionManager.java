package application.functions;

import display.DisplayDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class FunctionManager {

    private final ConcurrentMap<String, DisplayDTO> displays = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, FunctionTableRow> rows = new ConcurrentHashMap<>();

    public void put(String userString, DisplayDTO dto) {
        if (userString == null || dto == null) return;
        String key = userString.trim();
        if (!key.isEmpty()) {
            displays.put(key, dto);
        }
    }

    public DisplayDTO get(String userString) {
        if (userString == null) return null;
        return displays.get(userString.trim());
    }

    public boolean exists(String userString) {
        return userString != null && displays.containsKey(userString.trim());
    }

    /** Remove a function completely (from both maps). */
    public void remove(String userString) {
        if (userString == null) return;
        String key = userString.trim();
        displays.remove(key);
        rows.remove(key);
    }

    /** List all user-strings (sorted). */
    public List<String> list() {
        ArrayList<String> out = new ArrayList<>(displays.keySet());
        Collections.sort(out);
        return out;
    }

    public void putRecord(FunctionTableRow rec) {
        if (rec == null || rec.getName() == null) return;
        String key = rec.getName().trim();
        if (!key.isEmpty()) {
            rows.put(key, rec);
        }
    }

    public FunctionTableRow getRecord(String userString) {
        if (userString == null) return null;
        return rows.get(userString.trim());
    }

    public List<FunctionTableRow> listRecords() {
        return new ArrayList<>(rows.values());
    }

    public void clear() {
        displays.clear();
        rows.clear();
    }
}
