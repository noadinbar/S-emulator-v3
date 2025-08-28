package structure.instruction;

import structure.expand.ExpansionManager;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.util.Collections;
import java.util.List;

public abstract class AbstractInstruction implements Instruction {

    private final InstructionKind kind;
    private final InstructionType instType;
    private final Label myLabel;
    private final Variable variable;
    private final int degree;
    private List<Instruction> familyTree = Collections.emptyList();

    public AbstractInstruction(InstructionKind instKind ,InstructionType type, Variable variable, int degree) {
        this(instKind, type, variable, FixedLabel.EMPTY, degree);
    }

    public AbstractInstruction(InstructionKind instKind, InstructionType type, Variable variable, Label label, int degree) {
        this.kind=instKind;
        this.instType = type;
        this.myLabel = label;
        this.variable = variable;
        this.degree=degree;
    }

    @Override
    public String getName() {
        return instType.getName();
    }

    @Override
    public int cycles() {
        return instType.getCycles();
    }

    @Override
    public char kind(){return kind.getKind();}

    @Override
    public Label getMyLabel() {
        return myLabel;
    }

    @Override
    public Variable getVariable() {
        return variable;
    }

    @Override
    public int getDegree() {
        return degree;
    }

    @Override
    public List<Instruction> expand(ExpansionManager prog) {
        return Collections.singletonList(this);
    }

    public List<Instruction> getFamilyTree() {
        return familyTree;
    }

    public void setFamilyTree(List<Instruction> familyTree) {
        this.familyTree = familyTree;
    }
}
