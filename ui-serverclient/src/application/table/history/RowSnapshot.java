package application.table.history;

import execution.ExecutionDTO;
import java.util.EnumSet;
import java.util.List;

public final class RowSnapshot {

    public enum Kind { EXECUTION, DEBUG_TEXT }

    private final EnumSet<Kind> kinds;
    private final ExecutionDTO exec;
    private final List<String> debugText;

    private RowSnapshot(EnumSet<Kind> kinds, ExecutionDTO exec, List<String> debugText) {
        this.kinds = kinds;
        this.exec = exec;
        this.debugText = debugText;
    }

    public static RowSnapshot ofExec(ExecutionDTO e) {
        return new RowSnapshot(EnumSet.of(Kind.EXECUTION), e, null);
    }

    public static RowSnapshot ofDebug(List<String> t) {
        return new RowSnapshot(EnumSet.of(Kind.DEBUG_TEXT), null, t);
    }

    public RowSnapshot merge(RowSnapshot other) {
        if (other == null) return this;
        EnumSet<Kind> k = EnumSet.copyOf(this.kinds);
        k.addAll(other.kinds);
        return new RowSnapshot(
                k,
                this.exec != null ? this.exec : other.exec,
                this.debugText != null ? this.debugText : other.debugText
        );
    }

    public boolean hasExec()      { return kinds.contains(Kind.EXECUTION) && exec != null; }
    public boolean hasDebugText() { return kinds.contains(Kind.DEBUG_TEXT) && debugText != null; }
    public ExecutionDTO getExec() { return exec; }
    public List<String> getDebugText() { return debugText; }
}
