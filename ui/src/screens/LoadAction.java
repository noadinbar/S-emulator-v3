package screens;

import api.DisplayAPI;
import java.nio.file.Path;
import java.util.Scanner;

public class LoadAction {
    private DisplayAPI api;

    public LoadAction(DisplayAPI api) {
        this.api = api;
    }

    public DisplayAPI getDisplayAPI() { return api; }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter full path to load state (.ser): ");
        String p = sc.nextLine().trim();
        try {
            api = api.loadState(Path.of(p));
            System.out.println("State loaded.");
        } catch (RuntimeException e) {
            System.out.println("Load failed: " + e.getMessage());
        }
    }
}
