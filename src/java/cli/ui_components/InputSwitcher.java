package cli.ui_components;

import lombok.val;

import java.util.List;

import static java.lang.Math.abs;
import static cli.Input.arrowLeft;
import static cli.Input.arrowRight;
import static cli.Input.backspace;
import static cli.Ui.cursorX;
import static cli.Ui.cursorY;
import static cli.Ui.drawPositionX;
import static cli.Ui.drawPositionY;
import static cli.Ui.gray;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Ui.shouldShowCursor;
import static cli.Ui.whiteBackground;

public final class InputSwitcher implements InputComponent {
    public boolean isFocused = false;
    private boolean isHighlighted = true;
    private int cursorPosition;
    private final List<String> options;
    private String content;
    private String suggestion = "";

    public InputSwitcher(List<String> options) {
        this.options = options;
        content = options.getFirst();
    }

    public void onInput(String input) {
        switch (input) {
            case backspace -> {
                if (isHighlighted) {
                    content = "";
                    cursorPosition = 0;
                } else if (cursorPosition != 0) {
                    content = content.substring(0, cursorPosition - 1) + content.substring(cursorPosition);
                    cursorPosition--;
                }
            }

            case arrowLeft -> {
                if (isHighlighted) {
                    val currentlySelected = options.indexOf(content);
                    content = options.get(abs(currentlySelected - 1) % options.size());
                } else if (cursorPosition > 0) {
                    cursorPosition--;
                }
            }

            case arrowRight -> {
                if (isHighlighted) {
                    val currentlySelected = options.indexOf(content);
                    content = options.get((currentlySelected + 1) % options.size());
                } else if (cursorPosition < content.length()) {
                    cursorPosition++;
                }
            }

            case " " -> {
                if (isHighlighted) {
                    val currentlySelected = options.indexOf(content);
                    content = options.get((currentlySelected + 1) % options.size());
                } else {
                    content = options.getFirst();
                }
            }

            default -> {
                if (isHighlighted) {
                    content = "";
                    cursorPosition = 0;
                }

                val isSingleCharacter = input.length() == 1;
                if (isSingleCharacter) {
                    content = content.substring(0, cursorPosition) + input + content.substring(cursorPosition);
                    cursorPosition++;
                }
            }
        }

        suggestion = content.isEmpty() ? "" : options.stream().filter(it -> it.startsWith(content)).findFirst().orElse(
            "");
        if (!suggestion.isEmpty()) {
            suggestion = suggestion.substring(content.length());
        }

        if (!options.contains(content)) {
            isHighlighted = false;
        }
    }

    public void draw() {
        if (isFocused) {
            cursorX = drawPositionX + cursorPosition;
            cursorY = drawPositionY;

            if (isHighlighted) {
                print(whiteBackground);
            } else {
                shouldShowCursor = true;
            }
        } else {
            getContent();
        }

        print(content + gray + suggestion + resetColor);
    }

    public String getContent() {
        content += suggestion;
        suggestion = "";
        if (!options.contains(content)) {
            content = options.getFirst();
        }
        isHighlighted = true;

        return content;
    }
}
