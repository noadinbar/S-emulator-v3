package api;
import java.nio.file.Path;

public interface EngineService {
    Object loadFromXml(Path xmlPath);
}
