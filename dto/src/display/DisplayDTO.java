package display;

import types.LabelDTO;
import types.VarRefDTO;

import java.util.List;
import java.util.Objects;

public class DisplayDTO {
    private final String programName;
    private final List<VarRefDTO> inputsInUse;
    private final List<LabelDTO> labelsInUse;
    private final List<InstructionDTO> instructions;
    private final List<FunctionDTO> functions;

    public DisplayDTO(String programName,
                      List<VarRefDTO> inputsInUse,
                      List<LabelDTO> labelsInUse,
                      List<InstructionDTO> instructions) {
        this.programName = Objects.requireNonNull(programName, "programName");
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
        this.functions = List.of();
    }

    public DisplayDTO(String programName,
                      List<VarRefDTO> inputsInUse,
                      List<LabelDTO> labelsInUse,
                      List<InstructionDTO> instructions,
                      List<FunctionDTO> functions) {
        this.programName = Objects.requireNonNull(programName, "programName");
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
        this.functions = (functions == null) ? List.of() : List.copyOf(functions);
    }

    public String getProgramName() { return programName; }
    public List<VarRefDTO> getInputsInUse() { return inputsInUse; }
    public List<LabelDTO> getLabelsInUse() { return labelsInUse; }
    public List<InstructionDTO> getInstructions() { return instructions; }
    public List<FunctionDTO> getFunctions() { return functions; }
    public int numberOfInstructions() { return instructions.size(); }
}
