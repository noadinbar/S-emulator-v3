import structure.execution.ProgramExecutor;
import structure.execution.ProgramExecutorImpl;
import structure.instruction.*;
import structure.label.LabelImpl;
import structure.program.Program;
import structure.program.ProgramImpl;
import structure.variable.Variable;
import structure.variable.VariableImpl;
import structure.variable.VariableType;

public class Main {

    public static void main(String[] args) {

        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
        Variable z1 = new VariableImpl(VariableType.WORK, 1);

        LabelImpl l1 = new LabelImpl(1);
        LabelImpl l2 = new LabelImpl(1);

        Instruction increase = new IncreaseInstruction(x1, l1);
        Instruction decrease = new DecreaseInstruction(z1, l2);
        Instruction noop = new NoOpInstruction(Variable.RESULT);
        Instruction jnz = new JumpNotZeroInstruction(x1, l2);

        Program p = new ProgramImpl("test");
        p.addInstruction(increase);
        p.addInstruction(increase);
        p.addInstruction(decrease);
        p.addInstruction(jnz);

        ProgramExecutor programExecutor = new ProgramExecutorImpl(p);
        long result = programExecutor.run(3L, 6L, 2L);
        System.out.println(result);;


        sanity();
    }

    private static void sanity() {
        /*

        {y = x1}

        [L1] x1 ← x1 – 1
             y ← y + 1
             IF x1 != 0 GOTO L1
        * */

        Variable x1 = new VariableImpl(VariableType.INPUT, 1);
        LabelImpl l1 = new LabelImpl(1);

        Program p = new ProgramImpl("SANITY");
        p.addInstruction(new DecreaseInstruction(x1, l1));
        p.addInstruction(new IncreaseInstruction(Variable.RESULT));
        p.addInstruction(new JumpNotZeroInstruction(x1, l1));

        long result = new ProgramExecutorImpl(p).run(4L);
        System.out.println(result);
    }
}
