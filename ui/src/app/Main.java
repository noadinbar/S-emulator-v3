package app;

import api.DisplayAPI;
import api.LoadAPI;
import exportToDTO.LoadAPIImpl;

import exportToDTO.UninitializedDisplayAPI;
import menu.Menu;
import screens.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LoadAPI loadAPI = new LoadAPIImpl();
        DisplayAPI displayAPI = new UninitializedDisplayAPI();
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

            Menu selectedOption = Menu.byChoice(choice);
            if (selectedOption == null) {
                System.out.println("Invalid choice. Enter a number 1-6.\n");
                continue;
            }

            switch (selectedOption) {
                case LOAD_XML: {
                    LoadXMLAction action = new LoadXMLAction(loadAPI);
                    action.run();
                    displayAPI = action.getDisplayAPI();
                    System.out.println();
                    break;
                }

                case DISPLAY_PROGRAM: {
                    new DisplayProgramAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case EXPAND: {
                    new ExpandAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case RUN: {
                    new ExecuteAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case HISTORY: {
                    new screens.HistoryAction(displayAPI).run();
                    System.out.println();
                    break;
                }

                case EXIT: {
                    new ExitAction().run();
                    break;
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
