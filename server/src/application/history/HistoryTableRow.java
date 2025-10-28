package application.history;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryTableRow {

    private final int runNumber;               // sequential per user (1,2,3,...)
    private final String targetType;           // "PROGRAM" or "FUNCTION"
    private final String targetName;           // program name or function userString
    private final String architectureType;     // generation ("I","II","III","IV"...)
    private final int degree;                  // chosen degree
    private final long finalY;                 // Y register at end of run
    private final long cyclesCount;            // total cycles consumed
    private final List<Long> inputs;           // original inputs used to start the run
    private final List<String> outputsSnapshot;// snapshot(s) of final machine state for show status
    private final String runMode;              // "EXECUTION" or "DEBUG"
    private final String username;             // which user executed this run

    public HistoryTableRow(int runNumber,
                           String targetType,
                           String targetName,
                           String architectureType,
                           int degree,
                           long finalY,
                           long cyclesCount,
                           List<Long> inputs,
                           List<String> outputsSnapshot,
                           String runMode,
                           String username) {

        this.runNumber = runNumber;
        this.targetType = targetType;
        this.targetName = targetName;
        this.architectureType = architectureType;
        this.degree = degree;
        this.finalY = finalY;
        this.cyclesCount = cyclesCount;
        this.runMode = runMode;
        this.username = username;

        // Defensive copy so callers cannot mutate lists after construction
        if (inputs != null) {
            this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));
        } else {
            this.inputs = Collections.emptyList();
        }

        if (outputsSnapshot != null) {
            this.outputsSnapshot = Collections.unmodifiableList(new ArrayList<>(outputsSnapshot));
        } else {
            this.outputsSnapshot = Collections.emptyList();
        }
    }

    public int getRunNumber() {
        return runNumber;
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

    public long getFinalY() {
        return finalY;
    }

    public long getCyclesCount() {
        return cyclesCount;
    }

    public List<Long> getInputs() {
        return inputs;
    }

    public List<String> getOutputsSnapshot() {
        return outputsSnapshot;
    }

    public String getRunMode() {
        return runMode;
    }

    public String getUsername() {
        return username;
    }
}
