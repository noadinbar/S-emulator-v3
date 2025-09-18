// dto/src/display/Command3DTO.java
package display;

import types.LabelDTO;
import types.VarRefDTO;

import java.util.List;

//command3dto
public final class ExpandDTO {
    private final String programName;
    private final List<VarRefDTO> inputsInUse;
    private final List<LabelDTO> labelsInUse;
    private final List<ExpandedInstructionDTO> instructions;
    private final List<FunctionDTO> functions;

    public ExpandDTO(String programName,
                     List<VarRefDTO> inputsInUse,
                     List<LabelDTO> labelsInUse,
                     List<ExpandedInstructionDTO> instructions) {
        this.programName = programName;
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
        this.functions=List.of();
    }

    public ExpandDTO(String programName,
                     List<VarRefDTO> inputsInUse,
                     List<LabelDTO> labelsInUse,
                     List<ExpandedInstructionDTO> instructions,
                     List<FunctionDTO> functions) {
        this.programName = programName;
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
        this.functions = (functions == null) ? List.of() : List.copyOf(functions);
    }

    public String getProgramName() { return programName; }
    public List<VarRefDTO> getInputsInUse() { return inputsInUse; }
    public List<LabelDTO> getLabelsInUse() { return labelsInUse; }
    public List<ExpandedInstructionDTO> getInstructions() { return instructions; }
    public List<FunctionDTO> getFunctions() { return functions; }
}
