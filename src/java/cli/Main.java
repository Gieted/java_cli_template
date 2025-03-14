package cli;

import sun.misc.Signal;
import cli.screens.MainMenu;

import static cli.Static.listenToInput;
import static cli.RawMode.disableRawMode;
import static cli.RawMode.enableRawMode;
import static cli.Static.runInitLoop;
import static cli.Ui.clearScreen;
import static cli.Ui.drawUi;
import static cli.Ui.print;
import static cli.Ui.restoreBuffer;
import static cli.Ui.showCursor;
import static cli.Ui.switchBuffer;
import static cli.Ui.updateUi;

public class Main {
    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                print(restoreBuffer + showCursor);
                disableRawMode();
            }));
            enableRawMode();
            print(switchBuffer);
            MainMenu.show();
            Static.inputConsumer.set(Input::onInput);
            listenToInput();
            Static.init = Main::init;
            runInitLoop();
        } catch (Throwable throwable) {
            print(restoreBuffer + clearScreen + showCursor);
            throwable.printStackTrace();
            System.exit(1);
        }
    }

    @SuppressWarnings("sunapi")
    public static void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(Services::stopAllServices));
        Static.inputConsumer.set(Input::onInput);
        Signal.handle(new Signal("WINCH"), _ -> updateUi()); // listen to console window resize
        Config.load();

        // insert any additional init actions here

        drawUi();
        EventLoop.start();
    }
}
