package display;

import types.LabelDTO;
import types.VarRefDTO;

import java.util.List;

public final class ExpandDTO {
    private final String programName;
    private final List<VarRefDTO> inputsInUse;
    private final List<LabelDTO> labelsInUse;
    private final List<ExpandedInstructionDTO> instructions;
    private final int maxDegree;

    public ExpandDTO(String programName,
                     List<VarRefDTO> inputsInUse,
                     List<LabelDTO> labelsInUse,
                     List<ExpandedInstructionDTO> instructions, int maxDegree) {
        this.programName = programName;
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
        this.maxDegree = maxDegree;
    }

    public String getProgramName() { return programName; }
    public List<VarRefDTO> getInputsInUse() { return inputsInUse; }
    public List<LabelDTO> getLabelsInUse() { return labelsInUse; }
    public List<ExpandedInstructionDTO> getInstructions() { return instructions; }
    public int getMaxDegree() { return maxDegree; }
}
