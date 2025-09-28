package exportToDTO;

import api.DisplayAPI;
import api.LoadAPI;
import exceptions.InvalidFileExtensionException;
import exceptions.InvalidXmlFormatException;
import exceptions.UndefinedFunctionException;
import exceptions.UndefinedLabelException;
import jakarta.xml.bind.JAXBException;
import structure.program.ProgramImpl;
import structure.program.SProgram;
import utils.XMLToStructure;
import api.XMLLoader;

import java.nio.file.Path;

public class LoadAPIImpl implements LoadAPI {
    @Override
    public DisplayAPI loadFromXml(Path xmlPath) throws Exception {
        String name = (xmlPath != null && xmlPath.getFileName() != null)
                ? xmlPath.getFileName().toString()
                : "";
        if (!name.toLowerCase().endsWith(".xml")) {
            throw new InvalidFileExtensionException(
                    String.format("The file must have .xml extension (got %s).", name)
            );
        }

        final SProgram s;

        try {
            s = new XMLLoader().loadFromXml(xmlPath);
        } catch (JAXBException e) {
            String msg = (e.getLinkedException() != null && e.getLinkedException().getMessage() != null)
                    ? e.getLinkedException().getMessage()
                    : (e.getMessage() != null ? e.getMessage() : "Failed to parse XML.");
            throw new InvalidXmlFormatException("Invalid XML: " + msg, e);  // ← Unchecked
        }

        ProgramImpl program = new XMLToStructure().toProgram(s);
        try {
            program.validate();
            return new DisplayAPIImpl(program);
        }
        catch (UndefinedLabelException e){
            throw e;
        }
        catch (UndefinedFunctionException e){
            throw e;
        }
    }
}
