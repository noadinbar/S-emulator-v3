package exportToDTO;

import display.*;
import structure.function.Function;
import structure.instruction.synthetic.*;
import types.LabelDTO;
import types.VarRefDTO;
import types.VarOptionsDTO;

import java.util.*;

import structure.instruction.Instruction;
import structure.instruction.basic.DecreaseInstruction;
import structure.instruction.basic.IncreaseInstruction;
import structure.instruction.basic.JumpNotZeroInstruction;
import structure.instruction.basic.NeutralInstruction;
import structure.label.Label;
import structure.program.Program;
import structure.variable.Variable;
import structure.variable.VariableType;

import static java.lang.Integer.parseInt;


class DisplayMapper {

    private static Map<String, String> nameToUserString = Collections.emptyMap();

    static DisplayDTO toCommand2(Program program) {
        String programName = program.getName();
        List<Instruction> instructions = program.getInstructions();

        Map<String, String> map = new HashMap<>();
        for (Function func : program.getFunctions()) {
            map.put(func.getName(), func.getUserString());
        }
        nameToUserString = map;

        List<VarRefDTO> inputsInUse = computeInputsInUse(instructions);
        List<LabelDTO> labelsInUse  = computeLabelsInUse(instructions);

        List<InstructionDTO> dtoList = new ArrayList<>();
        for (int i = 0; i < instructions.size(); i++) {
            dtoList.add(toInstructionDTO(i + 1, instructions.get(i)));
        }

        List<FunctionDTO> functionDTOs = new ArrayList<>();
        for (Function func : program.getFunctions()) {
            List<Instruction> fInstructions = func.getInstructions();
            List<InstructionDTO> fdtoList = new ArrayList<>();
            for (int i = 0; i < fInstructions.size(); i++) {
                fdtoList.add(toInstructionDTO(i + 1, fInstructions.get(i)));
            }
            functionDTOs.add(new FunctionDTO(func.getName(), func.getUserString(), fdtoList));
        }


        return new DisplayDTO(programName, inputsInUse, labelsInUse, dtoList,functionDTOs);
    }

    private static InstructionDTO toInstructionDTO(int number, Instruction ins) {
        InstrKindDTO kind;
        switch (ins.kind()) {
            case 'B': kind = InstrKindDTO.BASIC; break;
            default:  kind = InstrKindDTO.SYNTHETIC;
        }
        LabelDTO lineLabel = labelDTO(ins.getMyLabel());
        InstructionBodyDTO body = toBody(ins);
        int cycles = ins.cycles();
        return new InstructionDTO(number, kind, lineLabel, body, cycles);
    }

    private static InstructionBodyDTO toBody(Instruction ins) {
        switch (ins.getName()) {
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
                NeutralInstruction n = (NeutralInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.NEUTRAL,
                        toVarRef(n.getVariable()),
                        null, null, null, null, 0L, null);
            }
            case "ASSIGNMENT": {
                AssignmentInstruction a = (AssignmentInstruction) ins;
                return new InstructionBodyDTO(InstrOpDTO.ASSIGNMENT,
                        null,
                        toVarRef(a.getVariable()),
                        toVarRef(a.getToAssign()),
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
            case "QUOTE": {
                QuotationInstruction q = (QuotationInstruction) ins;
                String shownArgs = ArgsWithComposition(q.getFunctionArguments());
                return new InstructionBodyDTO(
                        InstrOpDTO.QUOTE,
                        toVarRef(q.getVariable()),
                        null, null, null, null, 0L,
                        null,
                        q.getFunctionName(),
                        q.getUserString(),
                        shownArgs
                );
            }
            case "JUMP_EQUAL_FUNCTION": {
                JumpEqualFunctionInstruction f = (JumpEqualFunctionInstruction) ins;
                VarRefDTO v = toVarRef(f.getVariable());
                String shownArgs = ArgsWithComposition(f.getFunctionArguments());
                return new InstructionBodyDTO(
                        InstrOpDTO.JUMP_EQUAL_FUNCTION,
                        v,
                        null,
                        null,
                        v,
                        null,
                        0L,
                        labelDTO(f.getTargetLabel()),
                        f.getFunctionName(),
                        f.getUserString(),
                        shownArgs
                );
            }


            default:
                throw new IllegalStateException("Unknown instruction name: " + ins.getName());
        }
    }

    private static List<VarRefDTO> computeInputsInUse(List<Instruction> all) {
        Set<Integer> seen = new HashSet<>();
        List<VarRefDTO> out = new ArrayList<>();

        for (Instruction ins : all) {
            Variable var = ins.getVariable();
            if (var != null && var.getType() == VariableType.INPUT) {
                int idx = getValueFromPrefix(var.getRepresentation());
                if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
            }
            switch (ins.getName()) {
                case "ASSIGNMENT": {
                    AssignmentInstruction a = (AssignmentInstruction) ins;
                    Variable s = a.getToAssign();
                    if (s != null && s.getType() == VariableType.INPUT) {
                        int idx = getValueFromPrefix(s.getRepresentation());
                        if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
                    }
                    break;
                }
                case "JUMP_EQUAL_VARIABLE": {
                    JumpEqualVariableInstruction j = (JumpEqualVariableInstruction) ins;
                    Variable r = j.getToCompare();
                    if (r != null && r.getType() == VariableType.INPUT) {
                        int idx = getValueFromPrefix(r.getRepresentation());
                        if (seen.add(idx)) out.add(new VarRefDTO(VarOptionsDTO.x, idx));
                    }
                    break;
                }
                case "QUOTE": {
                    QuotationInstruction q = (QuotationInstruction) ins;
                    addInputsFromFunctionArguments(seen, out, q.getFunctionArguments());
                    break;
                }

                case "JUMP_EQUAL_FUNCTION": {
                    JumpEqualFunctionInstruction f = (JumpEqualFunctionInstruction) ins;
                    addInputsFromFunctionArguments(seen, out, f.getFunctionArguments());
                    break;
                }

                default: break;
            }
        }
        out.sort(Comparator.comparingInt(VarRefDTO::getIndex));
        return out;
    }

    private static int getValueFromPrefix(String rep) {
        if (rep == null || rep.isEmpty()) return 0;
        char c = rep.charAt(0);
        if ((c == 'x' || c == 'z') && rep.length() > 1) {
            try { return parseInt(rep.substring(1)); } catch (Exception ignore) {}
        }
        return 0;
    }

    private static void addInputsFromFunctionArguments(Set<Integer> seen, List<VarRefDTO> out, String args) {
        if (args == null || args.isBlank()) return;
        int n = args.length();
        for (int i = 0; i < n; i++) {
            char c = args.charAt(i);
            if (c == 'x') {
                int j = i + 1;
                int val = 0;
                boolean hasDigit = false;

                while (j < n) {
                    char d = args.charAt(j);
                    if (d >= '0' && d <= '9') {
                        hasDigit = true;
                        val = val * 10 + (d - '0');
                        j++;
                    } else {
                        break;
                    }
                }
                if (hasDigit && val >= 0 && seen.add(val)) {
                    out.add(new VarRefDTO(VarOptionsDTO.x, val));
                }
                i = j - 1;
            }
        }
    }

    private static List<LabelDTO> computeLabelsInUse(List<Instruction> all) {
        Set<Integer> regular = new HashSet<>();
        boolean hasExit = false;

        for (Instruction ins : all) {
            hasExit |= addLabel(regular, ins.getMyLabel());

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
                default: break;
            }
        }

        List<Integer> sorted = new ArrayList<>(regular);
        sorted.sort(Comparator.naturalOrder());

        List<LabelDTO> out = new ArrayList<>();
        for (int n : sorted) out.add(new LabelDTO("L" + n, false));
        if (hasExit) out.add(new LabelDTO("EXIT", true));
        return out;
    }

    private static boolean addLabel(Set<Integer> regular, Label label) {
        if (label == null) return false;
        String rep = label.getLabelRepresentation();
        if (rep == null || rep.isEmpty()) return false;
        switch (rep) {
            case "EMPTY":
                return false;
            case "EXIT":
                return true;
            default:
                if (rep.charAt(0) == 'L') {
                    try {
                        int num = parseInt(rep.substring(1));
                        regular.add(num);
                    } catch (Exception ignore) {}
                }
                return false;
        }
    }

    private static VarRefDTO toVarRef(Variable var) {
        if (var == null) return null;
        VarOptionsDTO type = switch (var.getType()) {
            case INPUT -> VarOptionsDTO.x;
            case RESULT -> VarOptionsDTO.y;
            case WORK  -> VarOptionsDTO.z;
        };
        int idx = getValueFromPrefix(var.getRepresentation());
        return new VarRefDTO(type, idx);
    }

    private static LabelDTO labelDTO(Label label) {
        if (label == null) return new LabelDTO("EMPTY", false);
        String rep = label.getLabelRepresentation();
        if (rep == null || rep.isEmpty()) return new LabelDTO("EMPTY", false);
        return switch (rep) {
            case "EMPTY" -> new LabelDTO("EMPTY", false);
            case "EXIT" -> new LabelDTO("EXIT", true);
            default -> new LabelDTO(rep, false);
        };
    }

    private static String ArgsWithComposition(String args) {
        if (args == null || args.isBlank() || nameToUserString == null || nameToUserString.isEmpty()) return args;
        StringBuilder out = new StringBuilder(args.length());
        int n = args.length();

        for (int i = 0; i < n; i++) {
            char c = args.charAt(i);
            out.append(c);
            if (c == '(') {
                int j = i + 1;
                while (j < n && Character.isWhitespace(args.charAt(j))) {
                    out.append(args.charAt(j));
                    j++;
                }
                int nameStart = j;
                while (j < n) {
                    char d = args.charAt(j);
                    if (d == ',' || d == ')') break;
                    j++;
                }
                String rawName = args.substring(nameStart, j).trim();
                if (!rawName.isEmpty()) {
                    String pretty = nameToUserString.getOrDefault(rawName, rawName);
                    out.append(pretty); // מוסיפים רק את ה-userString, בלי למחוק כלום
                }
                i = j - 1;
            }
        }
        return out.toString();
    }
}
