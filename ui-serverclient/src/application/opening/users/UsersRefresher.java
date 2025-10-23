package application.opening.users;

import client.requests.authentication.Users;
import client.responses.authentication.UsersResponder;
import org.jetbrains.annotations.NotNull;
import users.UserTableRowDTO;
import javafx.application.Platform;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import utils.HttpClientUtil;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class UsersRefresher extends TimerTask {
    private final AtomicBoolean shouldUpdate;
    private final Consumer<List<UserTableRowDTO>> onUpdate;

    public UsersRefresher(AtomicBoolean shouldUpdate,
                          Consumer<List<UserTableRowDTO>> onUpdate) {
        this.shouldUpdate = shouldUpdate;
        this.onUpdate = onUpdate;
    }

    @Override
    public void run() {
        if (!shouldUpdate.get()) return;

        Request req = Users.build();
        HttpClientUtil.runAsync(req, new Callback() {
            @Override public void onFailure(@NotNull Call call, @NotNull IOException e) { }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!shouldUpdate.get()) { response.close(); return; }
                if (!response.isSuccessful()) { response.close(); return; }

                List<UserTableRowDTO> rows = UsersResponder.parse(response);
                Platform.runLater(() -> {
                    if (shouldUpdate.get()) onUpdate.accept(rows);
                });
            }
        });
    }
}
