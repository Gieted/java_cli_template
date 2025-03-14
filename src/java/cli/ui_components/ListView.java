package cli.ui_components;

import lombok.val;

import java.util.ArrayList;
import java.util.List;

import static cli.Input.arrowDown;
import static cli.Input.arrowUp;
import static cli.Ui.lightGrayBackground;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Utils.clamp;
import static cli.WindowSize.getWindowSize;

public class ListView {
    public static int maxHeight = 25;
    public final List<String> items = new ArrayList<>();
    private int currentSelection = 0;

    public void onInput(String input) {
        switch (input) {
            case arrowUp -> {
                currentSelection--;
                if (currentSelection < 0) {
                    currentSelection = 0;
                }
            }
            case arrowDown -> {
                currentSelection++;
                if (currentSelection >= items.size()) {
                    currentSelection = items.size() - 1;
                }
            }
        }
    }

    public void draw() {
        if (maxHeight <= 0) {
            return;
        }

        int selectedStart = -1;
        int selectedSize = -1;
        val allLines = new ArrayList<String>();

        {
            var i = 0;
            for (var item : items) {
                if (item.isEmpty()) {
                    item = " ";
                }

                List<String> lines = new ArrayList<>(item.lines().toList());
                lines = lines.subList(0, clamp(lines.size(), 0, 5));

                if (lines.size() > 1) {
                    lines.addFirst("─".repeat(getWindowSize().width));
                    lines.add("...");
                    lines.add("─".repeat(getWindowSize().width));
                }

                if (i == currentSelection) {
                    selectedStart = allLines.size();
                    selectedSize = lines.size();
                }

                allLines.addAll(lines);

                i++;
            }
        }

        val drawStart = clamp(
            // + 1 to make center position floored rather than ceiled
            selectedStart + selectedSize / 2 - maxHeight / 2 + 1,
            0,
            clamp(allLines.size() - maxHeight, 0)
        );
        val drawEnd = drawStart + maxHeight;

        val linesToDraw = allLines.subList(drawStart, clamp(drawEnd, null, allLines.size()));

        selectedStart = selectedStart - drawStart;

        var i = 0;
        for (var line : linesToDraw) {
            if (i >= selectedStart && i < selectedStart + selectedSize) {
                print(lightGrayBackground);
            } else {
                print(resetColor);
            }

            print(line.substring(
                0,
                Math.min(line.length(), getWindowSize().width)
            ) + (i == linesToDraw.size() - 1 ? "" : "\n"));
            i++;
        }

        print(resetColor);
    }
}
