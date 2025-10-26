package utils;

import client.requests.info.Status;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class CreditsRefresher extends TimerTask {
    private static final Gson GSON = new Gson();

    private final AtomicBoolean shouldUpdate;
    private final Consumer<JsonObject> onUpdate;
    private final String programName;

    public CreditsRefresher(AtomicBoolean shouldUpdate,
                            Consumer<JsonObject> onUpdate,
                            String programName) {
        this.shouldUpdate = shouldUpdate;
        this.onUpdate = onUpdate;
        this.programName = programName;
    }

    @Override
    public void run() {
        if (!shouldUpdate.get()) {
            return;
        }

        // Build GET /api/status?program=<programName>
        Request req = Status.build(programName);

        // async call so we don't block the timer thread
        HttpClientUtil.runAsync(req, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // ignore network errors; header just won't update this tick
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        response.close();
                        return;
                    }

                    String json = response.body().string();
                    response.close();
                    JsonObject js = GSON.fromJson(json, JsonObject.class);

                    Platform.runLater(() -> {
                        if (shouldUpdate.get()) {
                            onUpdate.accept(js);
                        }
                    });
                } catch (Exception ignore) {
                    // swallow parse/close failures safely
                }
            }
        });
    }
}