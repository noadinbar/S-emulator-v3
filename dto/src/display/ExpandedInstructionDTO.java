// dto/src/display/ExpandedInstructionDTO.java
package display;

import java.util.List;

public final class ExpandedInstructionDTO {
    private final InstructionDTO instruction;          // השורה הסופית
    private final List<InstructionDTO> createdByChain; // שרשרת (נשתמש בהורה המיידי; אפשר להרחיב)

    public ExpandedInstructionDTO(InstructionDTO instruction, List<InstructionDTO> createdByChain) {
        this.instruction = instruction;
        this.createdByChain = List.copyOf(createdByChain);
    }
    public InstructionDTO getInstruction() { return instruction; }
    public List<InstructionDTO> getCreatedByChain() { return createdByChain; }
}
