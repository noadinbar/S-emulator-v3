package structure;

import java.util.List;

public class Program {
    private String name;
    private List<Instruction> instructions;

    public Program() {}

    public Program(String name, List<Instruction> instructions) {
        this.name = name;
        this.instructions = instructions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public void setInstructions(List<Instruction> instructions) {
        this.instructions = instructions;
    }

    @Override
    public String toString() {
        return "Program{" +
                "name='" + name + '\'' +
                ", instructions=" + instructions +
                '}';
    }
}
