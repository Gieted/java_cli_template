package cli.screens;

import cli.ui_components.LoadingAnimation;
import cli.ui_components.StatusBar;

import static cli.Input.arrowDown;
import static cli.Input.arrowUp;
import static cli.Input.controlA;
import static cli.Input.escape;
import static cli.Services.killService;
import static cli.Ui.Screen;
import static cli.Services.isRunning;
import static cli.Services.isStarting;
import static cli.Services.startService;
import static cli.Services.stopService;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Ui.resetColor;
import static cli.Ui.whiteBackground;
import static cli.Services.Service;

public class ServicesScreen {
    public static ServicesScreen state;
    private int selectionStart = 0;
    private int selectionEnd = 0;
    public boolean showLogs = false;

    public static void show() {
        Screen.current = Screen.services;
        state = new ServicesScreen();
    }

    public static void onInput(String input) {
        if (state.showLogs) {
            if (input.equals(escape)) {
                state.showLogs = false;
            } else {
                LogViewer.onInput(input);
            }

            return;
        }

        switch (input) {
            case escape -> {
                if (hasSelection()) {
                    state.selectionEnd = state.selectionStart;
                } else {
                    MainMenu.show();
                }
            }

            case arrowUp -> {
                if (!hasSelection()) {
                    state.selectionStart--;
                    if (state.selectionStart < 0) {
                        state.selectionStart = Service.values().length - 1;
                    }
                }
                state.selectionEnd = state.selectionStart;
            }

            case arrowDown -> {
                if (!hasSelection()) {
                    state.selectionStart++;
                    if (state.selectionStart >= Service.values().length) {
                        state.selectionStart = 0;
                    }
                }
                state.selectionEnd = state.selectionStart;
            }

            case "1", "2", "3", "4", "5", "6", "7", "8", "9" -> {
                state.selectionStart = Integer.parseInt(input) - 1;
                state.selectionEnd = state.selectionStart;
            }

            case controlA -> {
                state.selectionStart = 0;
                state.selectionEnd = Service.values().length - 1;
            }

            case "r" -> {
                for (int i = state.selectionStart; i <= state.selectionEnd; i++) {
                    startService(Service.values()[i]);
                }
            }

            case "s" -> {
                for (int i = state.selectionStart; i <= state.selectionEnd; i++) {
                    stopService(Service.values()[i]);
                }
            }

            case "k" -> {
                for (int i = state.selectionStart; i <= state.selectionEnd; i++) {
                    killService(Service.values()[i]);
                }
            }

            case "\r", "g" -> state.showLogs = true;
        }
    }

    public static void draw() {
        if (state.showLogs) {
            LogViewer.draw();
            return;
        }

        print(clearScreen + StatusBar.draw() + """
            
            Services:
            
            """);

        for (int i = 0; i < Service.values().length; i++) {
            var service = Service.values()[i];

            String icon;
            if (isStarting(service)) {
                icon = LoadingAnimation.draw();
            } else if (isRunning(service)) {
                icon = "●";
            } else {
                icon = "○";
            }

            print(
                (i >= state.selectionStart && i <= state.selectionEnd ? whiteBackground : resetColor) +
                (i + 1) + ". " + icon + " " + service.name + resetColor + "\n"
            );
        }

        print("""
                  
                  ESC -""" + (hasSelection() ? " deselect" : " go back to main menu | ⌃a - select all")
              + " | r - run"
              + " | s - stop"
              + " | k - kill"
        );
    }

    private static boolean hasSelection() {
        return state.selectionStart != state.selectionEnd;
    }
}
