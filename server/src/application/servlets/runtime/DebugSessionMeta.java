package application.servlets.runtime;

import java.util.List;

public class DebugSessionMeta {

    private final String username;
    private final String targetType;       // "PROGRAM" or "FUNCTION"
    private final String targetName;       // program name or function userString
    private final String architectureType; // generation string ("I"/"II"/...)
    private final int degree;
    private final List<Long> inputs;
    private volatile boolean recorded;

    public DebugSessionMeta(String username,
                            String targetType,
                            String targetName,
                            String architectureType,
                            int degree,
                            List<Long> inputs) {
        this.username = username;
        this.targetType = targetType;
        this.targetName = targetName;
        this.architectureType = architectureType;
        this.degree = degree;
        this.inputs = inputs;
        this.recorded = false;
    }

    public String getUsername() {
        return username;
    }

    public String getTargetType() {
        return targetType;
    }

    public String getTargetName() {
        return targetName;
    }

    public String getArchitectureType() {
        return architectureType;
    }

    public int getDegree() {
        return degree;
    }

    public List<Long> getInputs() {
        return inputs;
    }

    public boolean isRecorded() {
        return recorded;
    }

    public void markRecorded() {
        this.recorded = true;
    }
}