// File: ui/src/screens/ExpandAction.java
package screens;

import api.DisplayAPI;
import api.ExecutionAPI;
import display.Command3DTO;
import display.ExpandedInstructionDTO;
import exceptions.ProgramNotLoadedException;
import format.InstructionFormatter;

import java.util.Locale;
import java.util.Scanner;

public class ExpandAction {
    private final DisplayAPI api;

    public ExpandAction(DisplayAPI api) { this.api = api; }

    public void run() {
        try {
            ExecutionAPI exec = api.execution();
            int maxDegree = exec.getMaxDegree();
            System.out.println(String.format(Locale.US, "Max degree available: %d", maxDegree));

            System.out.print("Enter expansion degree (0 = AS IS): ");
            Scanner sc = new Scanner(System.in);
            int degree = 0;
            try {
                degree = Integer.parseInt(sc.nextLine().trim());
            } catch (Exception ignore) {
            }
            if (degree < 0) degree = 0;
            if (degree > maxDegree) degree = maxDegree;

            Command3DTO dto = api.expand(degree);

            System.out.println();
            System.out.println("Program: " + dto.getProgramName());
            System.out.println("Inputs in use: " + InstructionFormatter.joinInputs(dto.getInputsInUse()));
            System.out.println("Labels in use: " + InstructionFormatter.joinLabels(dto.getLabelsInUse()));
            System.out.println();

            for (ExpandedInstructionDTO row : dto.getInstructions()) {
                System.out.println(InstructionFormatter.formatExpanded(row));
            }
            System.out.println();
        }
        catch(ProgramNotLoadedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

}
