package display;

import types.LabelDTO;

public class InstructionDTO {
    private final int number;                 // 1-based לפי סדר ה-XML
    private final InstrKindDTO kind;          // BASIC/SYNTHETIC (ב-UI יודפס 'B'/'S')
    private final LabelDTO label;             // null אם אין תווית מוצמדת לשורה
    private final InstructionBodyDTO body;    // payload עמוק (לא מחזיר Instruction של ה-engine)
    private final int cycles;                 // מחזורי ריצה

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