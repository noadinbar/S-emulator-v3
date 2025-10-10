package utils;
import okhttp3.MediaType;

public class Constants {
    public static final String BASE_URL = "http://localhost:8080/server_Web_exploded";
    public static final String API_LOAD  = "/api/load-file";

    public static final String HEADER_ACCEPT = "Accept";
    public static final String CONTENT_TYPE_JSON = "application/json";

    public static final MediaType MEDIA_TYPE_XML  = MediaType.parse("application/xml");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

}
