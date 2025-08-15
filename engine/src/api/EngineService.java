package api;

import structure.program.SProgram;
import java.nio.file.Path;


public interface EngineService {
    SProgram loadFromXml(Path xmlPath) throws Exception;
}
