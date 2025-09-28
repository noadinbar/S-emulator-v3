package structure.program;

import structure.function.Function;
import structure.instruction.Instruction;

import java.util.List;

public interface Program {

    String getName();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();

    void validate();
    int calculateMaxDegree();

    List<Function> getFunctions();
    Function getFunction(String name);
    void addFunction(Function function);
}
