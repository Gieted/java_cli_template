package cli;

import cli.screens.DocPathScreen;
import cli.screens.MainMenu;
import cli.screens.ProgramCrashed;
import cli.screens.ServicesScreen;
import cli.screens.TaskSwitcher;
import cli.ui_components.LoadingAnimation;

import static cli.EventLoop.scheduleMacrotask;

public class Ui {
    public enum Screen {
        docPath,
        mainMenu,
        programCrashed,
        services,
        taskSwitcher;

        public static Screen current;
    }

    public static final String clearScreen = "\u001b[2J\u001b[H";
    public static final String whiteBackground = "\u001b[47m";
    public static final String black = "\u001b[30m";
    public static final String gray = "\u001b[90m";
    public static final String green = "\u001b[32m";
    public static final String yellow = "\u001b[33m";
    public static final String lightGrayBackground = "\u001b[47;1m";
    public static final String resetColor = "\u001b[0m";
    public static final String showCursor = "\u001b[?25h";
    private static final String hideCursor = "\u001b[?25l";
    public static final String switchBuffer = "\u001b[?1049h";
    public static final String restoreBuffer = "\u001b[?1049l";
    public static final String cursorBar = "\u001B[5 q";
    private static final int maxFps = 20;
    public static int drawPositionX = 1;
    public static int drawPositionY = 1;
    public static int cursorX = 1;
    public static int cursorY = 1;
    public static boolean shouldShowCursor = false;
    private static boolean drawnRecently = false;
    private static boolean skippedDraw = false;

    public static void drawUi() {
        drawPositionX = 1;
        drawPositionY = 1;
        shouldShowCursor = false;

        switch (Screen.current) {
            case docPath -> DocPathScreen.draw();
            case mainMenu -> MainMenu.draw();
            case programCrashed -> ProgramCrashed.draw();
            case services -> ServicesScreen.draw();
            case taskSwitcher -> TaskSwitcher.draw();
        }

        LoadingAnimation.stopIfNotDrawn();

        print(shouldShowCursor ? showCursor : hideCursor);
    }

    public static void updateUi() {
        scheduleMacrotask(() -> {
            if (drawnRecently) {
                skippedDraw = true;
                return;
            }

            drawUi();
            drawnRecently = true;
            scheduleMacrotask(1000 / maxFps, () -> {
                drawnRecently = false;
                if (skippedDraw) {
                    skippedDraw = false;
                    updateUi();
                }
            });
        });
    }

    public static void print(String text) {
        var actualText = text
            .replace(clearScreen, "")
            .replace(whiteBackground, "")
            .replace(black, "")
            .replace(gray, "")
            .replace(lightGrayBackground, "")
            .replace(resetColor, "")
            .replace(hideCursor, "")
            .replace(showCursor, "");

        for (var character : actualText.toCharArray()) {
            if (character == '\n') {
                drawPositionY++;
                drawPositionX = 1;
            } else if (character != '\r' && character != '\t') {
                drawPositionX++;
            }
        }

        System.out.print(text.replace("\n", "\r\n"));
    }

    public static String setCursorPosition(int row, int col) {
        return "\u001b[" + row + ";" + col + "H";
    }

    public static String setCursorPosition() {
        return setCursorPosition(cursorY, cursorX);
    }
}
