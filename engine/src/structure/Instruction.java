package structure;

import java.util.List;

public class Instruction {
    private String label;
    private String command;
    private int cycles;
    private InstructionType type;
    private List<Argument> arguments;

    public Instruction() {}

    public Instruction(String label, String command, int cycles, InstructionType type, List<Argument> arguments) {
        this.label = label;
        this.command = command;
        this.cycles = cycles;
        this.type = type;
        this.arguments = arguments;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public InstructionType getType() {
        return type;
    }

    public void setType(InstructionType type) {
        this.type = type;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    @Override
    public String toString() {
        return "Instruction{" +
                "label='" + label + '\'' +
                ", command='" + command + '\'' +
                ", cycles=" + cycles +
                ", type=" + type +
                ", arguments=" + arguments +
                '}';
    }
}
