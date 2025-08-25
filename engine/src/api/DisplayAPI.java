package api;

import display.Command2DTO;
import execution.HistoryDTO;

/** חוזה צר לפקודה 2: מחזיר DTO עמוק אחד */
public interface DisplayAPI {
    Command2DTO getCommand2();
    ExecutionAPI execution();
    HistoryDTO getHistory();
}
