package screens;

import api.DisplayAPI;
import execution.HistoryDTO;
import format.HistoryFormatter;

public class HistoryAction {
    private final DisplayAPI api;

    public HistoryAction(DisplayAPI api) { this.api = api; }

    public void run() {
        HistoryDTO dto = api.getHistory();
        System.out.print(HistoryFormatter.format(dto));
    }
}
