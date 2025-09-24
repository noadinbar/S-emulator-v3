package structure.expand;

import structure.function.Function;
import structure.instruction.Instruction;
import structure.instruction.AbstractInstruction;
import structure.instruction.synthetic.JumpEqualFunctionInstruction;
import structure.instruction.synthetic.QuotationInstruction;
import structure.program.Program;
import structure.program.ProgramImpl;

import java.util.ArrayList;
import java.util.List;

public final class ProgramExpander {

    private ProgramExpander() {}

    public static ExpandResult expandTo(Program baseProgram, int degree) {
        if (degree == 0) {
            List<List<Instruction>> levels0 = new ArrayList<>();
            levels0.add(new ArrayList<>(baseProgram.getInstructions())); // level 0
            return new ExpandResult(baseProgram, levels0);
        }
        ProgramImpl current = (ProgramImpl) baseProgram;
        int nextLabel = current.findMaxLabelIndex() + 1;
        int nextWork  = current.findMaxWorkIndex() + 1;
        ExpansionManager mgr = new ExpansionManagerImpl(nextLabel, nextWork);

        List<List<Instruction>> levels = new ArrayList<>();
        levels.add(new ArrayList<>(current.getInstructions())); // level 0

        for (int d = 0; d < degree; d++) {
            List<Instruction> nextInstructions = new ArrayList<>();

            for (Instruction ins : current.getInstructions()) {
                List<Instruction> children;
                if (ins instanceof QuotationInstruction qi)
                {
                    children = qi.expand(mgr, current);
                }
                else {
                    children = ins.expand(mgr);

                }
                final List<Instruction> parentChain = ((AbstractInstruction) ins).getFamilyTree();

                for (Instruction child : children) {
                    if (child == ins) {
                        continue;
                    }
                    AbstractInstruction currentInstruction = (AbstractInstruction) child;
                    List<Instruction> chain = new ArrayList<>(1 + parentChain.size());
                    chain.add(ins);
                    chain.addAll(parentChain);
                    currentInstruction.setFamilyTree(chain);
                }
                nextInstructions.addAll(children);
            }

            ProgramImpl nextProgram = new ProgramImpl(current.getName());
            for (Instruction inputInstruction : nextInstructions) {
                nextProgram.addInstruction(inputInstruction);
            }
            for (Function function : current.getFunctions()) {
                nextProgram.addFunction(function);
            }
            current = nextProgram;
            levels.add(new ArrayList<>(current.getInstructions()));
        }
        return new ExpandResult(current, levels);
    }
}
