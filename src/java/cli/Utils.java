package cli;

import lombok.SneakyThrows;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public class Utils {
    public static Runnable sneakyThrows(RunnableWithException runnable) {
        return () -> run(runnable);
    }

    public static <T> Consumer<T> sneakyThrows(ConsumerWithException<T> consumer) {
        return value -> run(consumer, value);
    }

    public static <T> T sneakyThrows(SupplierWithException<T> supplier) {
        return run(supplier);
    }

    @SneakyThrows
    public static void run(RunnableWithException runnable) {
        runnable.run();
    }

    @SneakyThrows
    public static <T> void run(ConsumerWithException<T> supplier, T value) {
        supplier.accept(value);
    }

    @SneakyThrows
    public static <T> T run(SupplierWithException<T> supplier) {
        return supplier.get();
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ConsumerWithException<T> {
        void accept(T t) throws Exception;
    }

    @FunctionalInterface
    public interface SupplierWithException<T> {
        T get() throws Exception;
    }

    public static <T> T also(T value, Runnable additionalAction) {
        additionalAction.run();
        return value;
    }

    public static <T, R> R let(T value, Function<T, R> block) {
        return block.apply(value);
    }

    public static <T> void completeFuture(CompletableFuture<T> future, SupplierWithException<T> completer) {
        try {
            future.complete(completer.get());
        } catch (Throwable throwable) {
            future.completeExceptionally(throwable);
        }
    }

    public static void completeFuture(CompletableFuture<Void> future, RunnableWithException completer) {
        try {
            completer.run();
            future.complete(null);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    public static int clamp(int value, @Nullable Integer min, @Nullable Integer max) {
        //noinspection DuplicatedCode
        assert min == null || max == null || min <= max;

        if (min != null && value < min) {
            return min;
        }

        if (max != null && value > max) {
            return max;
        }

        return value;
    }

    public static int clamp(int value, @Nullable Integer min) {
        return clamp(value, min, null);
    }

    public static long clamp(long value, @Nullable Long min, @Nullable Long max) {
        //noinspection DuplicatedCode
        assert min == null || max == null || min <= max;

        if (min != null && value < min) {
            return min;
        }

        if (max != null && value > max) {
            return max;
        }

        return value;
    }

    public static long clamp(long value, @Nullable Long min) {
        return clamp(value, min, null);
    }
}
