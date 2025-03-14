package cli;

import lombok.SneakyThrows;
import lombok.val;
import cli.screens.ProgramCrashed;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;

import static cli.Ui.drawUi;
import static cli.Ui.Screen;
import static cli.Utils.clamp;

public class EventLoop {
    public record Task(Runnable runnable, long time) {
    }

    private static final LinkedList<Task> macrotasks = new LinkedList<>();
    public static boolean active = false;

    public static void scheduleMacrotask(Runnable runnable) {
        scheduleMacrotask(0, runnable);
    }

    public static synchronized Task scheduleMacrotask(long timeout, Runnable runnable) {
        val time = System.currentTimeMillis() + timeout;
        Task task;

        if (macrotasks.isEmpty()) {
            task = new Task(runnable, time);
            macrotasks.add(task);
        } else {
            val iterator = macrotasks.listIterator(macrotasks.size());
            while (iterator.hasPrevious()) {
                val other = iterator.previous();
                if (other.time <= time) {
                    iterator.next();
                    break;
                }
            }
            task = new Task(runnable, time);
            iterator.add(task);
        }

        EventLoop.class.notify();
        return task;
    }

    public synchronized static void cancelMacrotask(Task task) {
        macrotasks.remove(task);
    }

    @SneakyThrows
    public static void start() {
        if (active) {
            return;
        }

        active = true;
        try {
            while (active) {
                Runnable task;
                synchronized (EventLoop.class) {
                    val next = macrotasks.peek();
                    if (next == null) {
                        EventLoop.class.wait();
                        continue;
                    }
                    if (next.time > System.currentTimeMillis()) {
                        EventLoop.class.wait(clamp(next.time - System.currentTimeMillis(), 0L));
                        continue;
                    }

                    macrotasks.poll();
                    task = next.runnable;
                }

                try {
                    task.run();
                } catch (Throwable throwable) {
                    if (Screen.current == Screen.programCrashed) {
                        throw throwable;
                    }

                    val writer = new StringWriter();
                    throwable.printStackTrace(new PrintWriter(writer));
                    ProgramCrashed.error = writer.toString();
                    ProgramCrashed.show();
                    drawUi();
                }
            }
        } finally {
            active = false;
        }
    }

    public static void stop() {
        active = false;
    }
}
