package cli.screens;

import lombok.val;
import cli.ui_components.ListView;

import static cli.HotReload.reload;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Ui.Screen;
import static cli.WindowSize.getWindowSize;

public class ProgramCrashed {
    public static String error;
    private static ListView listView;

    public static void show() {
        Screen.current = Screen.programCrashed;
        listView = new ListView();
        listView.items.addAll(error.lines().toList());
    }

    public static void onInput(String input) {
        switch (input) {
            case "q" -> System.exit(0);
            case "\r" -> reload();
            default -> listView.onInput(input);
        }
    }

    public static void draw() {
        val header = clearScreen +"""
            
            Program crashed
            
            """;

        val footer = """
            
            q - quit | ENTER - restart
            """;

        print(header);

        ListView.maxHeight = getWindowSize().height - ((int) header.lines().count()) - ((int) footer.lines().count());
        listView.draw();

        print(footer);
    }
}
