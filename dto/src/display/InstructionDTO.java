package display;

import types.LabelDTO;

public class InstructionDTO {
    private final int number;
    private final InstrKindDTO kind;
    private final LabelDTO label;
    private final InstructionBodyDTO body;
    private final int cycles;

    public InstructionDTO(int number,
                          InstrKindDTO kind,
                          LabelDTO label,
                          InstructionBodyDTO body,
                          int cycles) {
        this.number = number;
        this.kind = kind;
        this.label = label;
        this.body = body;
        this.cycles = cycles;
    }

    public int getNumber() { return number; }
    public InstrKindDTO getKind() { return kind; }
    public LabelDTO getLabel() { return label; }
    public InstructionBodyDTO getBody() { return body; }
    public int getCycles() { return cycles; }
}