package screens;

import api.DisplayAPI;
import execution.HistoryDTO;
import format.HistoryFormatter;

public class HistoryAction {
    private final DisplayAPI api;

    public HistoryAction(DisplayAPI api) { this.api = api; }

    /** מריץ את פקודה 5 – מציג היסטוריית ריצות */
    public void run() {
        HistoryDTO dto = api.getHistory();
        System.out.print(HistoryFormatter.format(dto));
    }
}
