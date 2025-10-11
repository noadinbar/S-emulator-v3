package exportToDTO;

import display.DisplayDTO;
import display.ExpandDTO;
import display.ExpandedInstructionDTO;
import display.InstructionDTO;
import structure.expand.ExpandResult;
import structure.expand.ProgramExpander;
import structure.function.Function;
import structure.instruction.AbstractInstruction;
import structure.instruction.Instruction;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class ExpandMapper {
    private ExpandMapper(){}

    public static ExpandDTO toCommand3(Program program, int degree, int maxDegree) {
        ExpandResult res = ProgramExpander.expandTo(program, degree);

        Program finalProg = res.getExpandedProgram();
        List<List<Instruction>> levels = res.getLevels();   // 0..degree
        int lastLevel = levels.size() - 1;

        List<Map<Instruction, InstructionDTO>> dtoAtLevel = new ArrayList<>(levels.size());
        for (int lvl = 0; lvl <= lastLevel; lvl++) {
            ProgramImpl progL = new ProgramImpl(finalProg.getName());
            for (Instruction ins : levels.get(lvl)) progL.addInstruction(ins);
            for (Function fn : finalProg.getFunctions()) progL.addFunction(fn);
            DisplayDTO c2 = DisplayMapper.toCommand2(progL);

            List<Instruction> insL = levels.get(lvl);
            List<InstructionDTO> dtoL = c2.getInstructions();
            Map<Instruction, InstructionDTO> map = new IdentityHashMap<>();
            int n = Math.min(insL.size(), dtoL.size());
            for (int i = 0; i < n; i++) map.put(insL.get(i), dtoL.get(i));
            dtoAtLevel.add(map);
        }

        DisplayDTO finalC2 = DisplayMapper.toCommand2(finalProg);
        List<InstructionDTO> finalDtos = finalC2.getInstructions();
        List<Instruction>     finalIns  = levels.get(lastLevel);

        Map<Instruction, Integer> levelOf = new IdentityHashMap<>();
        for (int lvl = 0; lvl <= lastLevel; lvl++) {
            for (Instruction ins : levels.get(lvl)) levelOf.put(ins, lvl);
        }

        List<ExpandedInstructionDTO> out = new ArrayList<>();
        for (int i = 0; i < finalIns.size() && i < finalDtos.size(); i++) {
            Instruction    leaf = finalIns.get(i);
            InstructionDTO left = finalDtos.get(i);

            List<InstructionDTO> chain = new ArrayList<>();
            for (Instruction anc : ((AbstractInstruction) leaf).getFamilyTree()) {
                Integer lvl = levelOf.get(anc);
                if (lvl == null) continue;
                InstructionDTO ancDto = dtoAtLevel.get(lvl).get(anc);
                if (ancDto != null) chain.add(ancDto);
            }

            out.add(new ExpandedInstructionDTO(left, chain));
        }

        return new ExpandDTO(
                finalC2.getProgramName(),
                finalC2.getInputsInUse(),
                finalC2.getLabelsInUse(),
                out,
                maxDegree
        );
    }
}
