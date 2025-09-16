package exportToDTO;

import api.DebugAPI;
import api.DisplayAPI;
import api.ExecutionAPI;
import display.Command2DTO;
import display.Command3DTO;
import execution.HistoryDTO;
import exceptions.ProgramNotLoadedException;

import java.nio.file.Path;

public class UninitializedDisplayAPI implements DisplayAPI {

    private static ProgramNotLoadedException notLoaded() {
        return new ProgramNotLoadedException(
                "No program is loaded yet. Load XML first (1)."
        );
    }

    @Override
    public Command2DTO getCommand2() {
        throw notLoaded();
    }

    @Override
    public Command3DTO expand(int degree) {
        throw notLoaded();
    }

    @Override
    public ExecutionAPI execution() {
        throw notLoaded();
    }

    @Override
    public ExecutionAPI executionForDegree(int degree) {
        throw notLoaded();
    }

    @Override
    public DebugAPI debugForDegree(int degree) { throw notLoaded(); }

    @Override
    public HistoryDTO getHistory() {
        throw notLoaded();
    }

    @Override
    public void saveState(Path path) {
        throw notLoaded();
    }

    @Override
    public DisplayAPI loadState(Path path) {
        throw notLoaded();
    }
}
