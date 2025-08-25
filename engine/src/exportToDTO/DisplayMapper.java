package exportToDTO;

import display.Command2DTO;
import display.InstrKindDTO;
import display.InstrOpDTO;
import display.InstructionBodyDTO;
import display.InstructionDTO;
import types.LabelDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import structure.instruction.Instruction;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NoOpInstruction;
import structure.instruction.synthetic.AssignmentInstruction;
import structure.instruction.synthetic.ConstantAssignmentInstruction;
import structure.instruction.synthetic.GoToInstruction;
import structure.instruction.synthetic.JumpEqualConstantInstruction;
import structure.instruction.synthetic.JumpEqualVariableInstruction;
import structure.instruction.synthetic.JumpZeroInstruction;
import structure.instruction.synthetic.ZeroVariableInstruction;
import structure.label.Label;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableType;

/** Mapper ל-DTO של פקודה 2 – עם EMPTY כתווית אובייקטיבית (לא null), ו-switch-case במקום if/else. */
class DisplayMapper {

    // ---------- ROOT ----------
    static Command2DTO toCommand2(Program model) {
        String programName = model.getName();
        List<Instruction> instructions = model.getInstructions();

        List<VarRefDTO> inputsInUse = computeInputsInUse(instructions);
        List<LabelDTO> labelsInUse  = computeLabelsInUse(instructions);

        List<InstructionDTO> dtoList = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            dtoList.add(toInstructionDTO(i + 1, instructions.get(i)));
        }
        return new Command2DTO(programName, inputsInUse, labelsInUse, dtoList);
    }

    // ---------- Instruction ----------
    private static InstructionDTO toInstructionDTO(int number, Instruction ins) {
        InstrKindDTO kind;
        switch (ins.kind()) {
            case 'B': kind = InstrKindDTO.BASIC; break;
            default:  kind = InstrKindDTO.SYNTHETIC;
        }
        LabelDTO lineLabel = labelDTO(ins.getMyLabel()); // תמיד LabelDTO: "EMPTY"/"EXIT"/"L#"
        InstructionBodyDTO body = toBody(ins);
        int cycles = ins.cycles();
        return new InstructionDTO(number, kind, lineLabel, body, cycles);
    }

    /** switch על שם ה־InstructionType כפי שמוחזר מ־ins.getName() */
    private static InstructionBodyDTO toBody(Instruction ins) {
        switch (ins.getName()) {
            // בסיסיות
            case "INCREASE": {
                IncreaseInstruction i = (IncreaseInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.INCREASE,
                        toVarRef(i.getVariable()), null, null, null, null, 0L, null);
            }
            case "DECREASE": {
                DecreaseInstruction d = (DecreaseInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.DECREASE,
                        toVarRef(d.getVariable()), null, null, null, null, 0L, null);
            }
            case "NEUTRAL": {
                NoOpInstruction n = (NoOpInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.NEUTRAL,
                        toVarRef(n.getVariable()), // ← חשוב! שולחים את המשתנה
                        null, null, null, null, 0L, null);
            }
            // השמות
            case "ASSIGNMENT": {
                AssignmentInstruction a = (AssignmentInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.ASSIGNMENT,
                        null,
                        toVarRef(a.getVariable()),     // dest
                        toVarRef(a.getToAssign()),     // source
                        null, null, 0L, null);
            }
            case "CONSTANT_ASSIGNMENT": {
                ConstantAssignmentInstruction c = (ConstantAssignmentInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.CONSTANT_ASSIGNMENT,
                        null,
                        toVarRef(c.getVariable()),     // dest
                        null, null, null,
                        c.getConstant(),               // constant
                        null);
            }
            case "ZERO_VARIABLE": {
                ZeroVariableInstruction z = (ZeroVariableInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.ZERO_VARIABLE,
                        null,
                        toVarRef(z.getVariable()),     // dest <- 0
                        null, null, null, 0L, null);
            }

            // קפיצות
            case "JUMP_NOT_ZERO": {
                JumpNotZeroInstruction j = (JumpNotZeroInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.JUMP_NOT_ZERO,
                        toVarRef(j.getVariable()),
                        null, null, null, null, 0L,
                        labelDTO(j.getTargetLabel())); // jumpTo
            }
            case "JUMP_ZERO": {
                JumpZeroInstruction j = (JumpZeroInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.JUMP_ZERO,
                        toVarRef(j.getVariable()),
                        null, null, null, null, 0L,
                        labelDTO(j.getTargetLabel()));
            }
            case "JUMP_EQUAL_CONSTANT": {
                JumpEqualConstantInstruction j = (JumpEqualConstantInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.JUMP_EQUAL_CONSTANT,
                        toVarRef(j.getVariable()),
                        null, null, null, null,
                        j.getConstant(),
                        labelDTO(j.getTargetLabel()));
            }
            case "JUMP_EQUAL_VARIABLE": {
                JumpEqualVariableInstruction j = (JumpEqualVariableInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.JUMP_EQUAL_VARIABLE,
                        null, null, null,
                        toVarRef(j.getVariable()),      // compare
                        toVarRef(j.getToCompare()),     // compareWith
                        0L,
                        labelDTO(j.getTargetLabel()));
            }
            case "GOTO_LABEL": {
                GoToInstruction g = (GoToInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.GOTO_LABEL,
                        null, null, null, null, null, 0L,
                        labelDTO(g.getTarget()));
            }
            default:
                throw new IllegalStateException("Unknown instruction name: " + ins.getName());
        }
    }

    // ---------- inputsInUse (switch במקום instanceof) ----------
    private static List<VarRefDTO> computeInputsInUse(List<Instruction> all) {
        Set<Integer> seen = new HashSet<>();
        List<VarRefDTO> out = new ArrayList<>();

        for (Instruction ins : all) {
            // תמיד נבדוק את המשתנה הראשי, אם הוא INPUT
            Variable v = ins.getVariable();
            if (v != null && v.getType() == VariableType.INPUT) {
                int idx = parseVarIndex(v.getRepresentation());
                if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
            }

            // תוספות לפי סוג ההוראה
            switch (ins.getName()) {
                case "ASSIGNMENT": {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    Variable s = a.getToAssign();
                    if (s != null && s.getType() == VariableType.INPUT) {
                        int idx = parseVarIndex(s.getRepresentation());
                        if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
                    }
                    break;
                }
                case "JUMP_EQUAL_VARIABLE": {
                    JumpEqualVariableInstruction j = (JumpEqualVariableInstruction) ins;
                    Variable r = j.getToCompare();
                    if (r != null && r.getType() == VariableType.INPUT) {
                        int idx = parseVarIndex(r.getRepresentation());
                        if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
                    }
                    break;
                }
                // יתר הסוגים לא מוסיפים עוד קלטים
                default: break;
            }
        }

        out.sort(Comparator.comparingInt(VarRefDTO::getIndex));
        return out;
    }

    private static int parseVarIndex(String rep) {
        if (rep == null || rep.isEmpty()) return 0;
        char c = rep.charAt(0);
        if ((c == 'x' || c == 'z') && rep.length() > 1) {
            try { return Integer.parseInt(rep.substring(1)); } catch (Exception ignore) {}
        }
        return 0; // y או כל מקרה אחר
    }

    // ---------- labelsInUse (switch במקום instanceof) ----------
    private static List<LabelDTO> computeLabelsInUse(List<Instruction> all) {
        Set<Integer> regular = new HashSet<>();
        boolean hasExit = false;

        for (Instruction ins : all) {
            // תווית על שורת ההוראה
            hasExit |= addLabel(regular, ins.getMyLabel());

            // יעדים לפי סוג ההוראה
            switch (ins.getName()) {
                case "JUMP_NOT_ZERO":
                    hasExit |= addLabel(regular, ((JumpNotZeroInstruction) ins).getTargetLabel());
                    break;
                case "JUMP_ZERO":
                    hasExit |= addLabel(regular, ((JumpZeroInstruction) ins).getTargetLabel());
                    break;
                case "JUMP_EQUAL_CONSTANT":
                    hasExit |= addLabel(regular, ((JumpEqualConstantInstruction) ins).getTargetLabel());
                    break;
                case "JUMP_EQUAL_VARIABLE":
                    hasExit |= addLabel(regular, ((JumpEqualVariableInstruction) ins).getTargetLabel());
                    break;
                case "GOTO_LABEL":
                    hasExit |= addLabel(regular, ((GoToInstruction) ins).getTarget());
                    break;
                default: break; // לשאר אין יעד
            }
        }

        List<Integer> sorted = new ArrayList<>(regular);
        sorted.sort(Comparator.naturalOrder());

        List<LabelDTO> out = new ArrayList<>();
        for (int n : sorted) out.add(new LabelDTO("L" + n, false));
        if (hasExit) out.add(new LabelDTO("EXIT", true));
        return out;
    }

    /**
     * מוסיף תווית לרשימת ה-L# (אם רגילה), או מסמן EXIT.
     * מחזיר true אם זו EXIT; מתעלם מ-EMPTY.
     */
    private static boolean addLabel(Set<Integer> regular, Label lbl) {
        if (lbl == null) return false;
        String rep = lbl.getLabelRepresentation();
        if (rep == null || rep.isEmpty()) return false; // זה מקרה נדיר, בכל זאת נחשב EMPTY
        switch (rep) {
            case "EMPTY":
                return false; // לא נכנס לרשימת השימושים
            case "EXIT":
                return true;
            default:
                if (rep.charAt(0) == 'L') {
                    try {
                        int num = Integer.parseInt(rep.substring(1));
                        regular.add(num);
                    } catch (Exception ignore) {}
                }
                return false;
        }
    }

    // ---------- Var/Label DTO ----------
    private static VarRefDTO toVarRef(Variable v) {
        if (v == null) return null;
        VarOptionsDTO space = switch (v.getType()) {
            case INPUT -> VarOptionsDTO.x;
            case RESULT -> VarOptionsDTO.y;
            case WORK  -> VarOptionsDTO.z;
        };
        int idx = parseVarIndex(v.getRepresentation());
        return new VarRefDTO(space, idx);
    }

    /**
     * תמיד מחזיר LabelDTO:
     * - EMPTY → name="EMPTY", isExit=false
     * - EXIT  → name="EXIT",  isExit=true
     * - L#    → name="L#",    isExit=false
     */
    private static LabelDTO labelDTO(Label l) {
        if (l == null) return new LabelDTO("EMPTY", false);
        String rep = l.getLabelRepresentation();
        if (rep == null || rep.isEmpty()) return new LabelDTO("EMPTY", false);
        switch (rep) {
            case "EMPTY": return new LabelDTO("EMPTY", false);
            case "EXIT":  return new LabelDTO("EXIT", true);
            default:      return new LabelDTO(rep, false);
        }
    }
}
