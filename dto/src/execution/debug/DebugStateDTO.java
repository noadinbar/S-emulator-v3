package execution.debug;

import execution.VarValueDTO;
import java.util.List;


public final class DebugStateDTO {
    private final int degree;
    private final int pc;
    private final long cyclesSoFar;
    private final List<VarValueDTO> vars;
    private final boolean terminated;

    public DebugStateDTO(int degree, int pc, long cyclesSoFar, List<VarValueDTO> vars, boolean terminated) {
        this.degree = degree;
        this.pc = pc;
        this.cyclesSoFar = cyclesSoFar;
        this.vars = (vars == null) ? List.of() : List.copyOf(vars);
        this.terminated = terminated;
    }

    public int getDegree() { return degree; }
    public int getPc() { return pc; }
    public long getCyclesSoFar() { return cyclesSoFar; }
    public List<VarValueDTO> getVars() { return vars; }

}
