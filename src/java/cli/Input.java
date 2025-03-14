package cli;

 
import cli.screens.DocPathScreen;
import cli.screens.MainMenu;
import cli.screens.ProgramCrashed;
import cli.screens.ServicesScreen;
import cli.screens.TaskSwitcher;

import static cli.EventLoop.cancelMacrotask;
import static cli.EventLoop.scheduleMacrotask;
import static cli.HotReload.hotReload;
import static cli.Ui.Screen;
import static cli.Ui.updateUi;

public class Input {
    public static final String arrowUp = "\u001B[A";
    public static final String arrowDown = "\u001B[B";
    public static final String arrowRight = "\u001B[C";
    public static final String arrowLeft = "\u001B[D";
    public static final String backspace = "\u007F";
    public static final String escape = "\u001B";
    public static final String controlA = "\u0001";
    public static final String controlR = "\u0012";
    public static final String controlE = "\u0005";

    private static final int escapeTimeout = 50;

    private static EventLoop.Task escapeTask;
    private static final StringBuilder buffer = new StringBuilder();

    public static void onInput(char input) {
        scheduleMacrotask(() -> {
            buffer.append(input);

            if (escapeTask == null && input == escape.charAt(0)) {
                escapeTask = scheduleMacrotask(escapeTimeout, Input::popEscapeTimeout);
                return;
            }

            var sequence = buffer.toString();

            if (escapeTask == null
                || sequence.equals(arrowUp)
                || sequence.equals(arrowDown)
                || sequence.equals(arrowLeft)
                || sequence.equals(arrowRight)) {

                buffer.setLength(0);
                cancelMacrotask(escapeTask);
                escapeTask = null;
                acceptSequence(sequence);
            }
        });
    }

    private static void popEscapeTimeout() {
        if (buffer.length() < 2 || buffer.charAt(1) != '[') { // ignore unknown special sequence
            acceptSequence(escape);
        }

        for (int i = 1; i < buffer.length(); i++) {
            var token = buffer.charAt(i);
            onInput(token);
        }

        buffer.setLength(0);
        escapeTask = null;
    }

    private static void acceptSequence(String sequence) {
        if (sequence.equals(controlR)) {
            hotReload();
            return;
        }

        switch (Screen.current) {
            case docPath -> DocPathScreen.onInput(sequence);
            case mainMenu -> MainMenu.onInput(sequence);
            case programCrashed -> ProgramCrashed.onInput(sequence);
            case services -> ServicesScreen.onInput(sequence);
            case taskSwitcher -> TaskSwitcher.onInput(sequence);
        }

        updateUi();
    }
}
