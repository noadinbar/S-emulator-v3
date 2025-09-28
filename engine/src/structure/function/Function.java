package structure.function;

import structure.instruction.Instruction;

import java.util.List;

public interface Function {
    String getName();
    String getUserString();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();
}
