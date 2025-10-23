package display;

import types.LabelDTO;

public class InstructionDTO {
    private final int number;
    private final InstrKindDTO kind;
    private final LabelDTO label;
    private final InstructionBodyDTO body;
    private final int cycles;
    private final String generation;

    public InstructionDTO(int number,
                          InstrKindDTO kind,
                          LabelDTO label,
                          InstructionBodyDTO body,
                          int cycles,
                          String generation) {
        this.number = number;
        this.kind = kind;
        this.label = label;
        this.body = body;
        this.cycles = cycles;
        this.generation = generation;
    }

    public int getNumber() { return number; }
    public InstrKindDTO getKind() { return kind; }
    public LabelDTO getLabel() { return label; }
    public InstructionBodyDTO getBody() { return body; }
    public int getCycles() { return cycles; }
    public String getGeneration() { return generation; }
}