package utils;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SimpleCookieManager implements CookieJar {

    private final static String CACHE_MANAGER_EXTRACT_PREFIX = "    [Cookie Manager] ---> ";
    private final static String CACHE_MANAGER_STORE_PREFIX = "    [Cookie Manager] <--- ";
    private final Map<String, Map<String, Cookie>> cookies = new HashMap<>();

    @NotNull
    @Override
    public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
        String host = httpUrl.host();
        List<Cookie> cookiesPerDomain = Collections.emptyList();
        synchronized (this) {
            if (cookies.containsKey(host)) {
                cookiesPerDomain = new ArrayList<>(cookies.get(host).values());
            }
        }
        return cookiesPerDomain;
    }

    @Override
    public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> responseCookies) {
        String host = httpUrl.host();
        synchronized (this) {
            Map<String, Cookie> cookiesMap = cookies.computeIfAbsent(host, key -> new HashMap<>());
            responseCookies
                    .stream()
                    .filter(cookie -> !cookiesMap.containsKey(cookie.name()))  // I have the freedom to choose not to accept changes in existing cookie
                    .forEach(cookie -> {
                        cookiesMap.put(cookie.name(), cookie);
                    });
        }
    }

}