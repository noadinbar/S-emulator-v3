package application.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HistoryManager {
    private final Map<String, List<HistoryTableRow>> historyByUser;

    public HistoryManager() {
        this.historyByUser = new ConcurrentHashMap<>();
    }

    public synchronized HistoryTableRow addRunRecord(
            String username,
            String targetType,
            String targetName,
            String architectureType,
            int degree,
            long finalY,
            long cyclesCount,
            List<Long> inputs,
            List<String> outputsSnapshot,
            String runMode
    ) {
        List<HistoryTableRow> list = historyByUser.get(username);
        if (list == null) {
            list = new ArrayList<>();
            historyByUser.put(username, list);
        }

        int runNumber = list.size() + 1;

        HistoryTableRow row = new HistoryTableRow(
                runNumber,
                targetType,
                targetName,
                architectureType,
                degree,
                finalY,
                cyclesCount,
                inputs,
                outputsSnapshot,
                runMode,
                username
        );

        list.add(row);
        return row;
    }

    public synchronized List<HistoryTableRow> getUserHistoryRows(String username) {
        List<HistoryTableRow> list = historyByUser.get(username);
        if (list == null) {
            return Collections.emptyList();
        }
        return List.copyOf(list);
    }
}
