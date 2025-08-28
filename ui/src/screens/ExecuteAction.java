package screens;

import api.DisplayAPI;
import api.ExecutionAPI;

import display.Command2DTO;
import display.Command3DTO;
import display.ExpandedInstructionDTO;
import display.InstructionDTO;

import execution.ExecutionDTO;
import execution.ExecutionRequestDTO;

import format.ExecutionFormatter;
import format.InstructionFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ExecuteAction {
    private final DisplayAPI displayAPI;
    private final ExecutionAPI execAPI;

    public ExecuteAction(DisplayAPI displayAPI) {
        this.displayAPI = displayAPI;
        this.execAPI = displayAPI != null ? displayAPI.execution() : null;
    }

    public void run() {
        if (displayAPI == null || execAPI == null) {
            System.out.println("No program loaded. Choose 'Load XML' first.");
            return;
        }

        Scanner sc = new Scanner(System.in);

        int maxDeg = execAPI.getMaxDegree();
        System.out.println(ExecutionFormatter.formatMaxDegree(maxDeg));
        System.out.print("Enter desired degree (0 for no expansion): ");
        int degree = parseIntOr(sc.nextLine(), 0);
        if (degree < 0) degree = 0;
        if (degree > maxDeg) degree = maxDeg;
        System.out.println(ExecutionFormatter.confirmDegree(degree));

        Command2DTO c2 = displayAPI.getCommand2();
        System.out.println(ExecutionFormatter.formatInputsInUse(c2.getInputsInUse()));
        System.out.print("Enter inputs (comma-separated), can be fewer/more: ");
        List<Long> inputs = parseCsvLongs(sc.nextLine());

        if (degree > 0) {
            Command3DTO dto = displayAPI.expand(degree);

            System.out.println();
            System.out.println(String.format("Program: %s", dto.getProgramName()));
            System.out.println(String.format("Inputs in use: %s",
                    InstructionFormatter.joinInputs(dto.getInputsInUse())));
            System.out.println(String.format("Labels in use: %s",
                    InstructionFormatter.joinLabels(dto.getLabelsInUse())));
            System.out.println();

            for (ExpandedInstructionDTO row : dto.getInstructions()) {
                System.out.println(String.format("%s", InstructionFormatter.formatExpanded(row)));
            }
            System.out.println();
        }

        ExecutionAPI runner = (degree == 0)
                ? execAPI
                : displayAPI.executionForDegree(degree);

        int degreeForExec = 0;
        ExecutionRequestDTO req = new ExecutionRequestDTO(degreeForExec, inputs);
        ExecutionDTO out = runner.execute(req);

        if (degree == 0) {
            if (out.getExecutedProgram() != null) {
                printExecutedProgram(out.getExecutedProgram());
            } else {
                printExecutedProgram(c2);
            }
        }
        // y
        System.out.println(ExecutionFormatter.formatY(out));
        if (out.getFinals() != null && !out.getFinals().isEmpty()) {
            System.out.println(ExecutionFormatter.formatAllVars(out.getFinals()));
        }
        System.out.println(ExecutionFormatter.formatCycles(out));
    }

    private static int parseIntOr(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    private static List<Long> parseCsvLongs(String s) {
        List<Long> list = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) return list;
        String[] parts = s.split(",");
        for (String p : parts) {
            String t = p.trim().toLowerCase(Locale.ROOT);
            if (t.isEmpty()) continue;
            try { list.add(Long.parseLong(t)); }
            catch (NumberFormatException ignore) { /* מתעלמים מערכים לא חוקיים */ }
        }
        return list;
    }

    private static void printExecutedProgram(Command2DTO dto) {
        System.out.println();
        System.out.println(String.format("Program: %s", dto.getProgramName()));
        System.out.println(String.format("Inputs in use: %s",
                InstructionFormatter.joinInputs(dto.getInputsInUse())));
        System.out.println(String.format("Labels in use: %s",
                InstructionFormatter.joinLabels(dto.getLabelsInUse())));
        System.out.println();

        for (InstructionDTO ins : dto.getInstructions()) {
            System.out.println(InstructionFormatter.formatDisplay(ins));
        }
        System.out.println();
    }
}
