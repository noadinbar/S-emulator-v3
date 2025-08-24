package api;

import java.nio.file.Path;

public interface LoadAPI {
    DisplayAPI loadFromXml(Path xmlPath) throws Exception;
}
