package utils;

import structure.Program;
import structure.Instruction;
import structure.Argument;
import structure.InstructionType;
import xmlmodel.SProgram;
import xmlmodel.SInstruction;
import xmlmodel.SInstructionArgument;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class XMLToStructure {

    private static final List<String> errors = new ArrayList<>();

    public static List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public static void clearErrors() {
        errors.clear();
    }

    public static Program loadProgramFromFile(String filePath) {
        clearErrors();

        File file = new File(filePath);

        // בדיקות קובץ
        if (!file.exists() || !file.isFile()) {
            errors.add("File not found: " + filePath);
            return null;
        }
        if (!filePath.toLowerCase().endsWith(".xml")) {
            errors.add("Invalid file type. Expected .xml: " + filePath);
            return null;
        }

        try {
            JAXBContext context = JAXBContext.newInstance(SProgram.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SProgram sProgram = (SProgram) unmarshaller.unmarshal(file);

            Program program = toProgram(sProgram);
            if (!errors.isEmpty()) {
                return null;
            }
            return program;

        } catch (JAXBException e) {
            errors.add("Failed to parse XML: " + e.getMessage());
            return null;
        }
    }

    private static Program toProgram(SProgram sProgram) {
        if (sProgram == null) {
            errors.add("SProgram object is null");
            return null;
        }
        if (sProgram.getName() == null || sProgram.getName().trim().isEmpty()) {
            errors.add("Program name is missing");
        }
        if (sProgram.getSInstructions() == null || sProgram.getSInstructions().getSInstruction().isEmpty()) {
            errors.add("Program contains no instructions");
        }

        List<Instruction> instructions = new ArrayList<>();
        if (sProgram.getSInstructions() != null) {
            for (SInstruction sInstr : sProgram.getSInstructions().getSInstruction()) {
                Instruction instr = toInstruction(sInstr);
                if (instr != null) {
                    instructions.add(instr);
                }
            }
        }

        if (!errors.isEmpty()) {
            return null;
        }
        return new Program(sProgram.getName(), instructions);
    }

    private static Instruction toInstruction(SInstruction sInstruction) {
        if (sInstruction == null) {
            errors.add("SInstruction object is null");
            return null;
        }
        if (sInstruction.getSCommand() == null || sInstruction.getSCommand().trim().isEmpty()) {
            errors.add("Instruction command is missing");
        }
        if (sInstruction.getSCycles() < 0) {
            errors.add("Instruction cycles cannot be negative for command: " + sInstruction.getSCommand());
        }

        InstructionType type = InstructionType.fromString(sInstruction.getType());
        if (type == null) {
            errors.add("Invalid or missing instruction type for label '" + sInstruction.getSLabel() + "'");
        }

        List<Argument> arguments = null;
        if (sInstruction.getSInstructionArguments() != null) {
            arguments = new ArrayList<>();
            for (SInstructionArgument sArg : sInstruction.getSInstructionArguments().getSInstructionArgument()) {
                Argument arg = toArgument(sArg);
                if (arg != null) {
                    arguments.add(arg);
                }
            }
        }

        return new Instruction(
                sInstruction.getSLabel(),
                sInstruction.getSCommand(),
                sInstruction.getSCycles(),
                type,
                arguments
        );
    }

    private static Argument toArgument(SInstructionArgument sArg) {
        if (sArg == null) {
            errors.add("SInstructionArgument object is null");
            return null;
        }
        if (sArg.getName() == null || sArg.getName().trim().isEmpty()) {
            errors.add("Argument name is missing");
        }
        if (sArg.getValue() == null) {
            errors.add("Argument value is missing for argument: " + sArg.getName());
        }
        return new Argument(sArg.getName(), sArg.getValue());
    }
}
