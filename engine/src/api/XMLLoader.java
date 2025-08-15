package api;

import api.EngineService;
import structure.program.SProgram;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.nio.file.Path;

public class XMLLoader implements EngineService {

    @Override
    public SProgram loadFromXml(Path xmlPath) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(SProgram.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (SProgram) unmarshaller.unmarshal(xmlPath.toFile());
    }
}
