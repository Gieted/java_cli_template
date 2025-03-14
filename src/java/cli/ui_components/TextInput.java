package cli.ui_components;

import lombok.val;

import static cli.Input.arrowLeft;
import static cli.Input.arrowRight;
import static cli.Input.backspace;
import static cli.Ui.cursorX;
import static cli.Ui.cursorY;
import static cli.Ui.drawPositionX;
import static cli.Ui.drawPositionY;
import static cli.Ui.print;
import static cli.Ui.shouldShowCursor;

public final class TextInput implements InputComponent {
    public boolean isFocused = false;
    public String content;
    private int cursorPosition;

    public TextInput(String content) {
        this.content = content;
        cursorPosition = content.length();
    }

    public void onInput(String input) {
        switch (input) {
            case backspace -> {
                if (cursorPosition != 0) {
                    content = content.substring(0, cursorPosition - 1) + content.substring(cursorPosition);
                    cursorPosition--;
                }
            }

            case arrowLeft -> {
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
            }

            case arrowRight -> {
                if (cursorPosition < content.length()) {
                    cursorPosition++;
                }
            }

            default -> {
                val isSingleCharacter =input.length() == 1;
                if (isSingleCharacter) {
                    content = content.substring(0, cursorPosition) + input + content.substring(cursorPosition);
                    cursorPosition++;
                }
            }
        }
    }

    public void draw() {
        if (isFocused) {
            cursorX = drawPositionX + cursorPosition;
            cursorY = drawPositionY;
            shouldShowCursor = true;
        }

        print(content);
    }
}
