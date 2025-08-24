package display;

import types.LabelDTO;
import types.VarRefDTO;

import java.util.List;
import java.util.Objects;

public class Command2DTO {
    private final String programName;
    private final List<VarRefDTO> inputsInUse;   // x.. לפי סדר הופעה
    private final List<LabelDTO> labelsInUse;    // L..; ואם קיימת – EXIT בסוף
    private final List<InstructionDTO> instructions;

    public Command2DTO(String programName,
                       List<VarRefDTO> inputsInUse,
                       List<LabelDTO> labelsInUse,
                       List<InstructionDTO> instructions) {
        this.programName = Objects.requireNonNull(programName, "programName");
        this.inputsInUse = List.copyOf(inputsInUse);
        this.labelsInUse = List.copyOf(labelsInUse);
        this.instructions = List.copyOf(instructions);
    }

    public String getProgramName() { return programName; }
    public List<VarRefDTO> getInputsInUse() { return inputsInUse; }
    public List<LabelDTO> getLabelsInUse() { return labelsInUse; }
    public List<InstructionDTO> getInstructions() { return instructions; }
}
