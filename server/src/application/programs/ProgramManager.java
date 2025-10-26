package application.programs;

import display.DisplayDTO;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProgramManager {
    private final ConcurrentMap<String, DisplayDTO> displays = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ProgramTableRow> rows = new ConcurrentHashMap<>();

    public void put(String name, DisplayDTO dto) {
        displays.put(name, dto);
    }

    public DisplayDTO get(String name) {
        if (name == null) return null;
        return displays.get(name.trim());
    }

    public List<String> list() {
        ArrayList<String> out = new ArrayList<>(displays.keySet());
        Collections.sort(out);
        return out;
    }

    public void remove(String name) {
        if (name == null) return;
        String key = name.trim();
        displays.remove(key);
        rows.remove(key);
    }

    public boolean exists(String name) {
        return name != null && displays.containsKey(name.trim());
    }

    public void putRecord(ProgramTableRow rec) {
        if (rec == null || rec.getName() == null) return;
        String key = rec.getName().trim();
        if (!key.isEmpty()) {
            rows.put(key, rec);
        }
    }

    public ProgramTableRow getRecord(String name) {
        if (name == null) return null;
        return rows.get(name.trim());
    }

    public List<ProgramTableRow> listRecords() {
        return new ArrayList<>(rows.values());
    }

    /** +1 to runCount for a given program, if it exists. */
    public void incRunCount(String name) {
        if (name == null) return;
        String key = name.trim();
        ProgramTableRow row = rows.get(key);
        if (row != null) {
            // Keep it simple; rows is concurrent, but we only mutate a single int.
            synchronized (row) {
                row.setRunCount(row.getRunCount() + 1);
            }
        }
    }

}
