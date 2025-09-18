package display;

import display.InstructionDTO;
import java.util.Collections;
import java.util.List;

public class FunctionDTO {
    private final String name;
    private final String userString;
    private final List<InstructionDTO> instructions;

    public FunctionDTO(String name, String userString, List<InstructionDTO> instructions) {
        this.name = name;
        this.userString = userString;
        this.instructions = (instructions == null)
                ? List.of()
                : List.copyOf(instructions);
    }

    public String getName() { return name; }
    public String getUserString() { return userString; }
    public List<InstructionDTO> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }
}
