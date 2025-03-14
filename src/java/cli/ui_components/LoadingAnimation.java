package cli.ui_components;

import lombok.val;

import static cli.HotReload.startThread;
import static cli.Ui.updateUi;

public class LoadingAnimation {
    private static final String[] frames = {
        "⠋",
        "⠙",
        "⠹",
        "⠸",
        "⠼",
        "⠴",
        "⠦",
        "⠧",
        "⠇",
        "⠏"
    };

    private static Thread thread;
    private static boolean isDrawn = false;

    public static String draw() {
        isDrawn = true;

        if (thread == null) {
            thread = startThread(() -> {
                while (!Thread.interrupted()) {
                    val timeToNextFrame = System.currentTimeMillis() % 100;
                    Thread.sleep(timeToNextFrame);
                    updateUi();
                }
            });
        }

        val time = System.currentTimeMillis();
        return frames[(int) (time / 100 % frames.length)];
    }

    public static void stopIfNotDrawn() {
        if (!isDrawn && thread != null) {
            thread.interrupt();
            thread = null;
        }
        isDrawn = false;
    }
}
