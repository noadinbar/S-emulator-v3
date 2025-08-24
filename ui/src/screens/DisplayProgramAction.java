package screens;

import api.DisplayAPI;                // מה-engine
import display.Command2DTO;
import display.InstructionDTO;
import format.InstructionFormatter;  // מה-ui/format

public class DisplayProgramAction {
    private final DisplayAPI api;

    public DisplayProgramAction(DisplayAPI api) {
        this.api = api;
    }

    /** מריץ את פקודה 2: מציג תוכנית בפורמט הנדרש */
    public void run() {
        if (api == null) {
            System.out.println("No program loaded.");
            return;
        }

        Command2DTO dto = api.getCommand2();

        // כותרות (String.format ב-UI בלבד)
        System.out.println(String.format("Program: %s", dto.getProgramName()));
        System.out.println(String.format("Inputs in use: %s",
                InstructionFormatter.joinInputs(dto.getInputsInUse())));
        System.out.println(String.format("Labels in use: %s",
                InstructionFormatter.joinLabels(dto.getLabelsInUse())));
        System.out.println();

        // כל הוראה – שורה אחת בפורמט:
        // # <number> (B|S) [LABEL] <command> (cycles)
        for (InstructionDTO ins : dto.getInstructions()) {
            System.out.println(InstructionFormatter.formatDisplay(ins));
        }
    }
}
