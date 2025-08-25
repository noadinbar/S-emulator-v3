package app;

import api.DisplayAPI;
import api.LoadAPI;
import exportToDTO.LoadAPIImpl;

import menu.Menu;
import screens.LoadXMLAction;
import screens.DisplayProgramAction;
import screens.ExecuteAction;   // ← חדש
import screens.ExitAction;     // ← לא חובה, אבל נוח

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LoadAPI loadAPI = new LoadAPIImpl(); // מה-engine
        DisplayAPI displayAPI = null;        // נשמר אחרי טעינה (פקודה 1)
        Scanner in = new Scanner(System.in);

        while (true) {
            printMenu();
            System.out.print("Choose option: ");
            String line = in.nextLine().trim();

            int choice;
            try {
                choice = Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Enter a number 1-6.\n");
                continue;
            }

            Menu m = Menu.byChoice(choice);
            if (m == null) {
                System.out.println("Invalid choice. Enter a number 1-6.\n");
                continue;
            }

            switch (m) {
                case LOAD_XML: {
                    LoadXMLAction action = new LoadXMLAction(loadAPI);
                    action.run();                          // יבקש path, יטען דרך ה-engine
                    displayAPI = action.getDisplayAPI();   // null אם נכשל
                    System.out.println();
                    break;
                }

                case DISPLAY_PROGRAM: {
                    if (displayAPI == null) {
                        System.out.println("No program loaded. Choose 'Load XML' first.\n");
                        break;
                    }
                    new DisplayProgramAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case EXPAND: {
                    System.out.println("Not implemented yet.\n");
                    break;
                }

                case RUN: { // Execute
                    if (displayAPI == null) {
                        System.out.println("No program loaded. Choose 'Load XML' first.\n");
                        break;
                    }
                    new ExecuteAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case HISTORY: {
                    System.out.println("Not implemented yet.\n");
                    break;
                }

                case EXIT: {
                    new ExitAction().run();
                    break; // לא יגיע לכאן בפועל בגלל System.exit(0)
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("=== S-Emulator ===");
        for (Menu m : Menu.values()) {
            System.out.println(m.num + ") " + m.title);
        }
    }
}
