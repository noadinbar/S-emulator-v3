package screens;

import api.DisplayAPI;
import exceptions.ProgramNotLoadedException;

import java.nio.file.Path;
import java.util.Scanner;

public class SaveAction {
    private final DisplayAPI api;

    public SaveAction(DisplayAPI api) { this.api = api; }

    public void run() {
        try {
            api.getHistory(); // או api.getCommand2(); שתיהן זורקות אם לא נטען XML
        } catch (ProgramNotLoadedException e) {
            System.out.println(e.getMessage());
            return;
        }
        Scanner sc = new Scanner(System.in);
        System.out.print("Enter FULL path including file name (without extension): ");
        String raw = sc.nextLine().trim();

        try {
            Path inPath = Path.of(raw);
            String target = raw.toLowerCase().endsWith(".ser") ? raw : raw + ".ser";
            System.out.println("Saving to: " + target);

            api.saveState(inPath);
            System.out.println("State saved: " + target);
        } catch (RuntimeException e) {
            System.out.println("Save failed: " + e.getMessage());
        }

    }
}
