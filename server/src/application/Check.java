package application;

import com.google.gson.Gson;

public class Check {
    String check = new Gson().toJson(java.util.Map.of("ok", true));
}
