package execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RunHistoryEntryDTO {

    private final int runNumber;
    private final String username;
    private final String targetType;       // "PROGRAM" or "FUNCTION"
    private final String targetName;       // program name or function userString
    private final String architectureType;
    private final int degree;
    private final long finalY;
    private final long cyclesCount;
    private final List<Long> inputsSnapshot;
    private final List<String> outputsSnapshot;
    private final String runMode;          // "EXECUTION" or "DEBUG"

    public RunHistoryEntryDTO(
            int runNumber,
            String username,
            String targetType,
            String targetName,
            String architectureType,
            int degree,
            long finalY,
            long cyclesCount,
            List<Long> inputsSnapshot,
            List<String> outputsSnapshot,
            String runMode
    ) {
        this.runNumber = runNumber;
        this.username = username;
        this.targetType = targetType;
        this.targetName = targetName;
        this.architectureType = architectureType;
        this.degree = degree;
        this.finalY = finalY;
        this.cyclesCount = cyclesCount;
        if (inputsSnapshot == null) {
            this.inputsSnapshot = Collections.emptyList();
        } else {
            this.inputsSnapshot = Collections.unmodifiableList(new ArrayList<>(inputsSnapshot));
        }
        if (outputsSnapshot == null) {
            this.outputsSnapshot = Collections.emptyList();
        } else {
            this.outputsSnapshot = Collections.unmodifiableList(new ArrayList<>(outputsSnapshot));
        }
        this.runMode = runMode;
    }

    public int getRunNumber() { return runNumber;}
    public String getUsername() { return username; }
    public String getTargetType() {return targetType;}
    public String getTargetName() {return targetName;}

    public String getArchitectureType() {
        return architectureType;
    }


    public int getDegree() {
        return degree;
    }

    public long getFinalY() {
        return finalY;
    }

    public long getCyclesCount() {
        return cyclesCount;
    }

    public List<Long> getInputs() {
        return inputsSnapshot;
    }

    public List<String> getOutputsSnapshot() {
        return outputsSnapshot;
    }

    public String getRunMode() {
        return runMode;
    }
}
