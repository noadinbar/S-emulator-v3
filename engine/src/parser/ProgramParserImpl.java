package parser;

import jakarta.xml.bind.*;
import xmlmodel.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class ProgramParserImpl implements ProgramParser {

    @Override
    public ParseResult parseProgramFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return ParseResult.error("File not found: " + filePath);
        }
        if (!filePath.toLowerCase().endsWith(".xml")) {
            return ParseResult.error("Invalid file type. Must be .xml");
        }

        try {
            JAXBContext context = JAXBContext.newInstance(SProgram.class);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            SProgram sProgram = (SProgram) unmarshaller.unmarshal(file);

            String validationError = validateProgram(sProgram);
            if (validationError != null) {
                return ParseResult.error(validationError);
            }

            return ParseResult.success(sProgram);

        } catch (JAXBException e) {
            return ParseResult.error("Failed to parse XML: " + e.getMessage());
        }
    }

    @Override
    public String validateProgram(SProgram program) {
        Set<String> labels = new HashSet<>();
        for (SInstruction instr : program.getSInstructions().getSInstruction()) {
            if (instr.getSLabel() != null && !instr.getSLabel().isBlank()) {
                labels.add(instr.getSLabel());
            }
        }
        for (SInstruction instr : program.getSInstructions().getSInstruction()) {
            if (instr.getSInstructionArguments() != null &&
                    instr.getSInstructionArguments().getSInstructionArgument() != null) {
                for (SInstructionArgument arg : instr.getSInstructionArguments().getSInstructionArgument()) {
                    if ((arg.getName().equalsIgnoreCase("gotoLabel") ||
                            arg.getName().equalsIgnoreCase("JNZLabel")) &&
                            !arg.getValue().equals("EXIT") &&
                            !labels.contains(arg.getValue())) {
                        return "Label '" + arg.getValue() + "' is referenced but not defined.";
                    }
                }
            }
        }
        return null;
    }
}
