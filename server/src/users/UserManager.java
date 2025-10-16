package users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Set<String> users = ConcurrentHashMap.newKeySet();

    public boolean addIfAbsent(String username) {
        String key = normalize(username);
        return !key.isEmpty() && users.add(key);
    }

    public boolean exists(String username) {
        return users.contains(normalize(username));
    }

    public void remove(String username) {
        users.remove(normalize(username));
    }

    public Collection<String> list() {
        return new ArrayList<>(users);
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim();
    }
}
