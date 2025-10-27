package display;

import java.util.ArrayList;
import java.util.List;

public class UploadResultDTO {
    public boolean ok;
    public String name;
    public List<String> addedFunctions;
    public String error;

    public UploadResultDTO() {}

    public UploadResultDTO(String name, List<String> functions) {
        // success case
        this.ok = true;
        this.name = name;
        if (functions != null) {
            this.addedFunctions = new ArrayList<>(functions);
        } else {
            this.addedFunctions = new ArrayList<>();
        }
    }

    public static UploadResultDTO error(String msg) {
        // error / validation-failure case
        UploadResultDTO uploadResult = new UploadResultDTO();
        uploadResult.ok = false;
        uploadResult.error = msg;
        uploadResult.addedFunctions = new ArrayList<>();
        return uploadResult;
    }

    public boolean isOk() { return ok; }
    public String getName() { return name; }
    public List<String> getAddedFunctions() { return addedFunctions; }
    public String getError() { return error; }
}
