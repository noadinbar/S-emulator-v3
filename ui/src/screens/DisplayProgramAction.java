package screens;

import api.DisplayAPI;
import display.Command2DTO;
import display.InstructionDTO;
import format.InstructionFormatter;

public class DisplayProgramAction {
    private final DisplayAPI api;

    public DisplayProgramAction(DisplayAPI api) {
        this.api = api;
    }

    public void run() {
        if (api == null) {
            System.out.println("No program loaded.");
            return;
        }

        Command2DTO dto = api.getCommand2();

        System.out.println(String.format("Program: %s", dto.getProgramName()));
        System.out.println(String.format("Inputs in use: %s",
                InstructionFormatter.joinInputs(dto.getInputsInUse())));
        System.out.println(String.format("Labels in use: %s",
                InstructionFormatter.joinLabels(dto.getLabelsInUse())));
        System.out.println();

        // # <number> (B|S) [LABEL] <command> (cycles)
        for (InstructionDTO ins : dto.getInstructions()) {
            System.out.println(InstructionFormatter.formatDisplay(ins));
        }
    }
}
