package structure.program;

import structure.instruction.Instruction;
import utils.ParseResult;

import java.util.List;

public interface Program {

    String getName();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();

    ParseResult validate();
    int calculateMaxDegree();
    int calculateCycles();

}
