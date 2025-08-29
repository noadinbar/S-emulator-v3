package screens;

import api.DisplayAPI;
import exceptions.ProgramNotLoadedException;
import execution.HistoryDTO;
import format.HistoryFormatter;

public class HistoryAction {
    private final DisplayAPI api;

    public HistoryAction(DisplayAPI api) { this.api = api; }

    public void run() {
        try {
            HistoryDTO dto = api.getHistory();
            System.out.print(HistoryFormatter.format(dto));
        }
        catch(ProgramNotLoadedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}
