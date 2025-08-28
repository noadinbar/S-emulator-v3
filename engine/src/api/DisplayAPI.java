package api;

import display.Command2DTO;
import display.Command3DTO;
import execution.HistoryDTO;

/** חוזה צר לפקודה 2: מחזיר DTO עמוק אחד */
public interface DisplayAPI {
    Command2DTO getCommand2();
    Command3DTO expand(int degree);
    ExecutionAPI execution();
    ExecutionAPI executionForDegree(int degree);
    HistoryDTO getHistory();
}
