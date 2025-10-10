package utils;
import okhttp3.MediaType;

public class Constants {
    public static final String BASE_URL = "http://localhost:8080/server_Web_exploded";
    public static final String API_LOAD  = "/api/load";

    public static final MediaType MEDIA_TYPE_XML  = MediaType.parse("application/xml");
    public static final MediaType MEDIA_TYPE_JSON = MediaType.parse("application/json");

}
