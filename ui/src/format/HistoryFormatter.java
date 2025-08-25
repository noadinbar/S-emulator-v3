package format;

import execution.HistoryDTO;
import execution.RunHistoryEntryDTO;

public final class HistoryFormatter {
    private HistoryFormatter(){}

    public static String format(HistoryDTO h) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== History — ").append(h.getProgramName()).append(" ===\n");
        if (h.getTotalRuns() == 0) {
            sb.append("No runs yet.\n");
            return sb.toString();
        }
        for (RunHistoryEntryDTO e : h.getEntries()) {
            sb.append(String.format(
                    "#%d | degree=%d | inputs=%s | y=%d | cycles=%d%n",
                    e.getRunNumber(), e.getDegree(), e.getInputs(), e.getYValue(), e.getCycles()
            ));
        }
        return sb.toString();
    }
}
