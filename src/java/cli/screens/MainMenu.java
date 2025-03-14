package cli.screens;

import cli.Config;
import cli.ui_components.StatusBar;

import java.util.List;

import static cli.Input.arrowDown;
import static cli.Input.arrowUp;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Ui.shouldShowCursor;
import static cli.Ui.whiteBackground;
import static cli.Ui.Screen;

public class MainMenu {
    private static final List<String> optionsList = List.of(
        "Switch task",
        "Services",
        "Docpath"
    );

    private static MainMenu state;
    private int selection = 0;

    public static void show() {
        Screen.current = Screen.mainMenu;
        state = new MainMenu();
    }

    public static void onInput(String input) {
        switch (input) {
            case "q" -> System.exit(0);

            case arrowUp -> {
                state.selection--;
                if (state.selection < 0) {
                    state.selection = optionsList.size() - 1;
                }
            }

            case arrowDown -> {
                state.selection++;
                if (state.selection >= optionsList.size()) {
                    state.selection = 0;
                }
            }

            case "1" -> TaskSwitcher.show();
            case "2" -> ServicesScreen.show();
            case "3" -> DocPathScreen.show();

            case "\r" -> {
                switch (state.selection) {
                    case 0 -> TaskSwitcher.show();
                    case 1 -> ServicesScreen.show();
                    case 2 -> DocPathScreen.show();
                }
            }
        }
    }

    public static void draw() {
        shouldShowCursor = false;

        var task = Config.get("branch");
        if (task == null) {
            task = "-";
        }

        print(clearScreen + StatusBar.draw() +
              """
                  
                  Welcome to the Java CLI template!
                  Press 'q' to exit
                  
                  current task:\s""" + task + whiteBackground + "\n" + """
                  
                  """);

        for (int i = 0; i < optionsList.size(); i++) {
            print(
                (i == state.selection ? whiteBackground : resetColor) +
                (i + 1) + ". " + optionsList.get(i) + resetColor + "\n"
            );
        }
    }
}
