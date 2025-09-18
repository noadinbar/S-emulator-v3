package structure.program;

import structure.function.Function;
import structure.instruction.Instruction;
import utils.ParseResult;

import java.util.List;

public interface Program {

    String getName();
    void addInstruction(Instruction instruction);
    List<Instruction> getInstructions();

    ParseResult validate();
    int calculateMaxDegree();

    List<Function> getFunctions();
    Function getFunction(String name);
    void addFunction(Function function);
}
