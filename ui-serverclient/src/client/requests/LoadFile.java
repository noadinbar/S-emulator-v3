package client.requests;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import utils.Constants;

public class LoadFile {

    public static Request build(Path xmlPath) {
        Objects.requireNonNull(xmlPath, "xmlPath");
        if (!Files.isRegularFile(xmlPath)) {
            throw new IllegalArgumentException("XML path is not a file: " + xmlPath);
        }

        RequestBody fileBody = RequestBody.create(Constants.MEDIA_TYPE_XML, xmlPath.toFile());

        RequestBody multipart = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", xmlPath.getFileName().toString(), fileBody)
                .build();

        String url = Constants.BASE_URL + Constants.API_LOAD;
        return new Request.Builder()
                .url(url)
                .post(multipart)
                .addHeader("Accept", "application/json")
                .build();
    }
}
