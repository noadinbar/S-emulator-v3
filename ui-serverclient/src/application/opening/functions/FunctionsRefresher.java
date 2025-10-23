package application.opening.functions;

import client.responses.info.FunctionsResponder;
import display.FunctionRowDTO;
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

/** Polls the server for Functions table rows and pushes them to the UI. */
public final class FunctionsRefresher extends TimerTask {

    private final AtomicBoolean shouldUpdate;
    private final Consumer<List<FunctionRowDTO>> onUpdate;

    public FunctionsRefresher(AtomicBoolean shouldUpdate, Consumer<List<FunctionRowDTO>> onUpdate) {
        this.shouldUpdate = shouldUpdate;
        this.onUpdate = onUpdate;
    }

    @Override
    public void run() {
        if (!shouldUpdate.get()) return;

        String url = Constants.BASE_URL + Constants.API_FUNCTIONS;
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        HttpClientUtil.runAsync(request, new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                if (!shouldUpdate.get()) return;
                System.err.println("Functions refresh failed: "
                        + e.getClass().getSimpleName()
                        + (e.getMessage() != null ? (": " + e.getMessage()) : ""));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!shouldUpdate.get()) { response.close(); return; }
                if (response.isSuccessful()) {
                    List<FunctionRowDTO> rows = FunctionsResponder.rowsParse(response);
                    Platform.runLater(() -> onUpdate.accept(rows));
                } else {
                    System.err.println("Functions refresh HTTP " + response.code());
                    response.close();
                }
            }
        });
    }
}
