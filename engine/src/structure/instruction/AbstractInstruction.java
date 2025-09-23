package structure.instruction;

import structure.expand.ExpansionManager;
import structure.label.FixedLabel;
import structure.label.Label;
import structure.variable.Variable;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public abstract class AbstractInstruction implements Instruction, Serializable {

    private final InstructionKind kind;
    private final InstructionType instType;
    private final Label myLabel;
    private final Variable variable;
    private List<Instruction> familyTree = Collections.emptyList();
    private static final long serialVersionUID = 1L;

    public AbstractInstruction(InstructionKind instKind ,InstructionType type, Variable variable) {
        this(instKind, type, variable, FixedLabel.EMPTY);
    }

    public AbstractInstruction(InstructionKind instKind, InstructionType type, Variable variable, Label label) {
        this.kind=instKind;
        this.instType = type;
        this.myLabel = label;
        this.variable = variable;
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
