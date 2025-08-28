package exportToDTO;

import api.DisplayAPI;
import api.LoadAPI;
import structure.program.ProgramImpl;
import structure.program.SProgram;
import utils.ParseResult;
import utils.XMLToStructure;
import api.XMLLoader;

import java.nio.file.Path;

public class LoadAPIImpl implements LoadAPI {
    @Override
    public DisplayAPI loadFromXml(Path xmlPath) throws Exception {
        SProgram s = new XMLLoader().loadFromXml(xmlPath);
        ProgramImpl program = new XMLToStructure().toProgram(s);

        ParseResult pr = program.validate();
        if (!pr.isSuccess()) {
            throw new Exception(pr.getMessage());
        }
        return new DisplayAPIImpl(program);
    }
}
