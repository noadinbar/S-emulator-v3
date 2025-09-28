package structure.function;

import structure.instruction.Instruction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class FunctionImpl implements Function {
    private final String name;
    private String userString;
    private final List<Instruction> instructions;

    public FunctionImpl(String name, String userString) {
        this.name = name;
        this.userString = userString;
        instructions = new ArrayList<>();
    }

    @Override
    public String getName() { return name; }

    @Override
    public String getUserString() { return userString; }

    @Override
    public void addInstruction(Instruction instruction) {
        if (instruction == null) return;
        instructions.add(instruction);
    }

    @Override
    public List<Instruction> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FunctionImpl)) return false;
        FunctionImpl that = (FunctionImpl) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
