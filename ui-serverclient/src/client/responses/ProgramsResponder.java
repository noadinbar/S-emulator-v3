package client.responses;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import display.ProgramRowDTO;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

public final class ProgramsResponder {
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<ProgramRowDTO>>() {}.getType();

    private ProgramsResponder() { }

    public static List<ProgramRowDTO> parse(Response response) throws IOException {
        try (ResponseBody body = response.body()) {
            String json = body != null ? body.string() : "[]";
            return GSON.fromJson(json, LIST_TYPE);
        }
    }
}
