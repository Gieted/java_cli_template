package cli.screens;

import lombok.val;
import cli.ui_components.TextInput;
import cli.ui_components.StatusBar;

import static cli.Input.escape;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Ui.Screen;
import static cli.Ui.setCursorPosition;
import static cli.Ui.shouldShowCursor;

public class DocPathScreen {
    private static DocPathScreen state;
    private final TextInput input = new TextInput("");

    public static void show() {
        Screen.current = Screen.docPath;
        state = new DocPathScreen();
        state.input.isFocused = true;
    }

    public static void onInput(String input) {
        if (input.equals(escape)) {
            MainMenu.show();
        } else {
            state.input.onInput(input);
        }
    }

    public static void draw() {
        shouldShowCursor = true;

        print(clearScreen + StatusBar.draw() +
              """
                  
                  Input docpath:
                  
                  """);

        state.input.draw();

        val segments = state.input.content.split("\\.");
        val result = new StringBuilder();

        var i = 0;
        for (val segment : segments) {
            result.append("\n");
            if (i != 0) {
                result.append(" ".repeat((i - 1) * 2)).append("|_ ");
            }
            result.append(segment);
            i++;
        }

        print(
            """
                
                """ + result + "\n" + """
                
                ESC - go back to main menu
                """);

        print(setCursorPosition());
    }
}
