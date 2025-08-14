package structure.program;

import structure.instruction.Instruction;

import java.util.ArrayList;
import java.util.List;

public class ProgramImpl implements Program {

    private final String name;
    private final List<Instruction> instructions;

    public ProgramImpl(String name) {
        this.name = name;
        instructions = new ArrayList<>();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    @Override
    public List<Instruction> getInstructions() {
        return instructions;
    }

    //need to implement
    @Override
    public boolean validate() {
        return false;
    }

    //need to implement
    @Override
    public int calculateMaxDegree() {
        // traverse all commands and find maximum degree
        return 0;
    }

    //need to implement
    @Override
    public int calculateCycles() {
        // traverse all commands and calculate cycles
        return 0;
    }
}
