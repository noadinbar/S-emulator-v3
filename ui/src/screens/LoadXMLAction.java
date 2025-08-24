package screens;

import api.DisplayAPI;
import api.LoadAPI;

import java.nio.file.Paths;
import java.util.Scanner;

public class LoadXMLAction {
    private final LoadAPI loadAPI;
    private DisplayAPI displayAPI; // נשמר לשימוש בפקודה 2

    public LoadXMLAction(LoadAPI loadAPI) {
        this.loadAPI = loadAPI;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter XML path: ");
        String path = sc.nextLine().trim();

        try {
            displayAPI = loadAPI.loadFromXml(Paths.get(path));
            System.out.println("Program loaded successfully.");
        } catch (Exception e) {
            displayAPI = null;
            System.out.println("Load failed: " + e.getMessage());
        }
    }

    public DisplayAPI getDisplayAPI() { return displayAPI; }
}
