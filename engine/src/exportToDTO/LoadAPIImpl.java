package exportToDTO;

import api.DisplayAPI;
import api.LoadAPI;
import structure.program.ProgramImpl;
import structure.program.SProgram;
import utils.ParseResult;
import utils.XMLToStructure;
import api.XMLLoader; // משתמש במחלקה/ממשק שקיימים אצלך

import java.nio.file.Path;

public class LoadAPIImpl implements LoadAPI {
    @Override
    public DisplayAPI loadFromXml(Path xmlPath) throws Exception {
        // 1) טען XML ל-SProgram (כבר קיים אצלך)
        SProgram s = new XMLLoader().loadFromXml(xmlPath);

        // 2) המר למודל הפנימי
        ProgramImpl program = new XMLToStructure().toProgram(s);

        // 3) ולידציה – בלי הדפסות; אם יש בעיה -> Exception עם הודעה
        ParseResult pr = program.validate();
        if (!pr.isSuccess()) {
            throw new Exception(pr.getMessage());
        }

        // 4) החזר API של פקודה 2 מוכן לשימוש (engine נשאר פסיבי)
        return new DisplayAPIImpl(program);
    }
}
