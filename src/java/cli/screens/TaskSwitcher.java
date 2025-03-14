package cli.screens;

import cli.Config;
import cli.ui_components.StatusBar;

import static cli.Input.backspace;
import static cli.Input.escape;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Ui.setCursorPosition;
import static cli.Ui.shouldShowCursor;
import static cli.Ui.whiteBackground;
import static cli.Ui.Screen;

public class TaskSwitcher {
    private static TaskSwitcher state;
    private String input = "feature/";

    public static void show() {
        Screen.current = Screen.taskSwitcher;
        state = new TaskSwitcher();
    }

    public static void onInput(String input) {
        switch (input) {
            case escape -> MainMenu.show();
            case "\r" -> {
                Config.set("branch", state.input);
                Config.save();
                MainMenu.show();
            }
            case backspace -> {
                if (state.input.isEmpty()) {
                    return;
                }
                state.input = state.input.substring(0, state.input.length() - 1);
            }
            default -> {
                if (input.length() == 1 && Character.isLetterOrDigit(input.charAt(0))
                    || input.equals("-")
                    || input.equals("/")) {

                    state.input += input;
                }
            }
        }
    }

    public static void draw() {
        shouldShowCursor = true;

        print(clearScreen + StatusBar.draw() +
              """
                  
                  Please type branch name for task you're currently working on.
                  E.g. 'feature/branch-name'
                  
                  """ + whiteBackground + state.input + resetColor + "\n" + """
                  
                  ESC - go back to main menu | ENTER - confirm
                  """ + setCursorPosition(5, state.input.length() + 1));
    }
}
