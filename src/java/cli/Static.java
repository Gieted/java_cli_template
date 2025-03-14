package cli;

 

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class Static {
    public static AtomicReference<Consumer<Character>> inputConsumer = new AtomicReference<>();
    public static Runnable init;

    public static void listenToInput() {
        new Thread(() -> {
            while (!Thread.interrupted()) {
                try {
                    var input = (char) System.in.read();
                    inputConsumer.get().accept(input);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    public static void runInitLoop() {
        while (!Thread.interrupted()) {
            init.run();
        }
    }

    public static void setInitMethod(Method initMethod) {
        init = () -> {
            try {
                initMethod.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
