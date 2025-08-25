package exportToDTO;

import execution.HistoryDTO;
import execution.RunHistoryEntryDTO;
import structure.program.Program;
import structure.program.ProgramImpl;
import utils.RunHistory;

import java.util.ArrayList;
import java.util.List;

final class HistoryMapper {
    private HistoryMapper(){}

    static HistoryDTO toHistory(Program program) {
        List<RunHistory> hist = ((ProgramImpl) program).getRunHistory();
        List<RunHistoryEntryDTO> entries = new ArrayList<>();
        for (RunHistory r : hist) {
            entries.add(new RunHistoryEntryDTO(
                    r.getRunNumber(),
                    r.getDegree(),
                    r.getInputs(),
                    r.getYValue(),
                    r.getCycles()
            ));
        }
        return new HistoryDTO(program.getName(), entries);
    }
}
