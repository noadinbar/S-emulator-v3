package app;

import api.DisplayAPI;
import api.LoadAPI;
import exportToDTO.LoadAPIImpl;

import menu.Menu;
import screens.*;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        LoadAPI loadAPI = new LoadAPIImpl();
        DisplayAPI displayAPI = null;
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
                    action.run();
                    displayAPI = action.getDisplayAPI();
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
                    if (displayAPI == null) {
                        System.out.println("No program loaded. Choose 'Load XML' first.\n");
                        break;
                    }
                    new ExpandAction(displayAPI).run();
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
                    if (displayAPI == null) {
                        System.out.println("No program loaded. Choose 'Load XML' first.");
                        break;
                    }
                    new screens.HistoryAction(displayAPI).run();
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
