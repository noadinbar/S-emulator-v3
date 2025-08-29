package screens;

import api.DisplayAPI;
import api.ExecutionAPI;

import display.Command3DTO;
import display.ExpandedInstructionDTO;

import exceptions.InvalidDegreeException;
import exceptions.ProgramNotLoadedException;

import format.InstructionFormatter;
import format.ExecutionFormatter;

import java.util.Scanner;

public class ExpandAction {

    private final DisplayAPI api;

    public ExpandAction(DisplayAPI api) {
        this.api = api;
    }

    public void run() {
        try {
            Scanner sc = new Scanner(System.in);
            ExecutionAPI exec = api.execution();
            int maxDeg = exec.getMaxDegree();
            System.out.println(ExecutionFormatter.formatMaxDegree(maxDeg));

            Command3DTO dto;

            while (true) {
                System.out.print("Enter degree to expand (0 for no expansion): ");
                int degree = parseIntOr(sc.nextLine());
                System.out.println(ExecutionFormatter.confirmDegree(degree));
                try {
                    dto = api.expand(degree);
                    break;
                } catch (InvalidDegreeException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }

            System.out.println();
            System.out.printf("Program: %s%n", dto.getProgramName());
            System.out.printf("Inputs in use: %s%n", InstructionFormatter.joinInputs(dto.getInputsInUse()));
            System.out.printf("Labels in use: %s%n", InstructionFormatter.joinLabels(dto.getLabelsInUse()));
            System.out.println();

            for (ExpandedInstructionDTO row : dto.getInstructions()) {
                System.out.printf("%s%n", InstructionFormatter.formatExpanded(row));
            }
            System.out.println();

        } catch (ProgramNotLoadedException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static int parseIntOr(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }
}
