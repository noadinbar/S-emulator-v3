package screens;

import api.DisplayAPI;
import api.LoadAPI;
import exceptions.InvalidFileExtensionException;
import exceptions.InvalidXmlFormatException;
import exceptions.UndefinedLabelException;

import java.nio.file.Paths;
import java.util.Scanner;

public class LoadXMLAction {
    private final LoadAPI loadAPI;
    private DisplayAPI displayAPI;

    public LoadXMLAction(LoadAPI loadAPI) {
        this.loadAPI = loadAPI;
    }

    public void run() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter XML path: ");
        String path = sc.nextLine().trim();

        try {
            displayAPI = loadAPI.loadFromXml(Paths.get(path)); // Path
            System.out.println("Program loaded successfully.");
        } catch (InvalidFileExtensionException | UndefinedLabelException | InvalidXmlFormatException e) {
            System.out.println("Error: " + e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public DisplayAPI getDisplayAPI() { return displayAPI; }
}
