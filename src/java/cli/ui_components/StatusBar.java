package cli.ui_components;

 

import java.util.ArrayList;
import java.util.List;

import static cli.Ui.gray;
import static cli.Ui.resetColor;

public class StatusBar {
    public static List<String> backgroundTasks = new ArrayList<>();

    public static String draw() {
        var result = new StringBuilder(gray);

        var i = 0;
        for (String task : backgroundTasks) {
            result.append(LoadingAnimation.draw()).append(" ").append(task);
            if (i != backgroundTasks.size() - 1) {
                result.append(" | ");
            }
        }

        return result.append(resetColor).toString();
    }
}
