package cli.screens;

import lombok.val;
import cli.ui_components.ListView;

import static cli.Ui.clearScreen;
import static cli.Ui.shouldShowCursor;
import static cli.Ui.green;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Ui.yellow;
import static cli.WindowSize.getWindowSize;

public class LogViewer {
    public static final ListView listView = new ListView();

    public static void onInput(String input) {
        listView.onInput(input);
    }

    public static void draw() {
        shouldShowCursor = false;

        val header = clearScreen + """
            services:""" + green + " all" + resetColor + " | log_level: " + yellow + "DEBUG" + resetColor + " | max_lines: " + green + "5\n" + resetColor + """
                         
                         """ + resetColor;

        print(header);
        ListView.maxHeight = getWindowSize().height - ((int) header.lines().count());
        listView.draw();
    }
}
