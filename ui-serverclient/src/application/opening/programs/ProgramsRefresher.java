package application.opening.programs;

import client.responses.info.ProgramsResponder;
import display.ProgramRowDTO;
import javafx.application.Platform;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import utils.Constants;
import utils.HttpClientUtil;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Pull-all-the-information-always (TimerTask + AtomicBoolean), aligned with runAsync(Request, Callback). */
public final class ProgramsRefresher extends TimerTask {

    private final AtomicBoolean shouldUpdate;
    private final Consumer<List<ProgramRowDTO>> onUpdate;

    public ProgramsRefresher(AtomicBoolean shouldUpdate, Consumer<List<ProgramRowDTO>> onUpdate) {
        this.shouldUpdate = shouldUpdate;
        this.onUpdate = onUpdate;
    }

    @Override
    public void run() {
        if (!shouldUpdate.get()) return;

        String url = Constants.BASE_URL + Constants.API_PROGRAMS;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        HttpClientUtil.runAsync(request, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (!shouldUpdate.get()) return;
                System.err.println("Programs refresh failed: "
                        + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? (": " + e.getMessage()) : ""));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!shouldUpdate.get()) { response.close(); return; }
                if (response.isSuccessful()) {
                    List<ProgramRowDTO> rows = ProgramsResponder.parse(response);
                    Platform.runLater(() -> onUpdate.accept(rows));
                } else {
                    System.err.println("Programs refresh HTTP " + response.code());
                    response.close();
                }
            }
        });
    }
}
