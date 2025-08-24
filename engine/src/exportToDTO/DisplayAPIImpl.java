package exportToDTO;

import api.DisplayAPI;
import display.Command2DTO;

import structure.program.Program;

public class DisplayAPIImpl implements DisplayAPI {
    private final Program program;
    public DisplayAPIImpl(Program program) { this.program = program; }
    @Override public Command2DTO getCommand2() { return DisplayMapper.toCommand2(program); }
}
