package structure.state;

import java.io.Serializable;

import structure.program.ProgramImpl;
import utils.RunHistory;

public final class EngineState implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ProgramImpl program;
    private final RunHistory history;

    public EngineState(ProgramImpl program, RunHistory history) {
        this.program = program;
        this.history = history;
    }

    public ProgramImpl program() { return program; }
    public RunHistory history() { return history; }
}
