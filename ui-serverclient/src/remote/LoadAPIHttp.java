package remote;

import api.DisplayAPI;
import api.LoadAPI;
import client.responses.LoadFileResponder;
import display.DisplayDTO;

import java.nio.file.Path;

public class LoadAPIHttp implements LoadAPI {
    @Override
    public DisplayAPI loadFromXml(Path xmlPath) throws Exception {
        DisplayDTO dto = LoadFileResponder.execute(xmlPath);
        return new RemoteDisplayAPI(dto, null);
    }
}
