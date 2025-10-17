package display;

public class UploadResultDTO {
    public boolean ok;
    public String name;
    public String error;

    public UploadResultDTO() {}

    public UploadResultDTO(String name) {
        this.ok = true;
        this.name = name;
    }

    public static UploadResultDTO error(String msg) {
        UploadResultDTO uploadResult = new UploadResultDTO();
        uploadResult.ok = false;
        uploadResult.error = msg;
        return uploadResult;
    }

    public boolean isOk() { return ok; }
    public String getName() { return name; }
    public String getError() { return error; }
}
