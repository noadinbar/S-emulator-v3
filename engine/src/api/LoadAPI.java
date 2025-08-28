package api;

import exceptions.InvalidFileExtensionException;
import exceptions.UndefinedLabelException;

import java.nio.file.Path;

public interface LoadAPI {
    DisplayAPI loadFromXml(Path xmlPath) throws Exception;
}
