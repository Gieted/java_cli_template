package cli;

import lombok.SneakyThrows;
import lombok.val;
import cli.screens.DocPathScreen;
import cli.screens.MainMenu;
import cli.screens.ServicesScreen;
import cli.screens.TaskSwitcher;

import java.io.InterruptedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static cli.Build.compileJava;
import static cli.EventLoop.scheduleMacrotask;
import static cli.Static.setInitMethod;
import static cli.Ui.clearScreen;
import static cli.Ui.print;
import static cli.Utils.RunnableWithException;
import static cli.Utils.sneakyThrows;
import static cli.Ui.Screen;

public class HotReload {
    private static class ChildFirstClassLoader extends URLClassLoader {
        ChildFirstClassLoader(URL[] urls) {
            super(urls);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals("cli.Static")) {
                return super.loadClass(name, resolve);
            }

            Class<?> loadedClass = findLoadedClass(name);
            if (loadedClass == null) {
                try {
                    loadedClass = findClass(name);
                } catch (ClassNotFoundException e) {
                    loadedClass = super.loadClass(name, resolve);
                }
            }

            if (resolve) {
                resolveClass(loadedClass);
            }
            return loadedClass;
        }
    }

    public static List<Thread> activeThreads = new ArrayList<>();

    public static Thread startThread(RunnableWithException runnable) {
        val thread = new Thread(sneakyThrows(() -> {
            try {
                runnable.run();
            } catch (InterruptedException | InterruptedIOException ignore) {
            } finally {
                scheduleMacrotask(() ->
                    activeThreads.remove(Thread.currentThread()));
            }
        }));
        thread.start();
        activeThreads.add(thread);
        return thread;
    }

    @SneakyThrows
    public static void hotReload() {
        print(clearScreen + "\nHot reloading...");

        try {
            Config.load();
            val javaHome = Config.get("java_home");
            val javac = javaHome == null ? "javac" : Paths.get(javaHome, "bin", "javac").toString();
            val sourcePath = Paths.get("src", "java");
            val classpath = "libs/compile/*:libs/provided/*";
            val outputPath = Paths.get("build");
            val workingDirectory = Paths.get(".");
            compileJava(sourcePath, classpath, outputPath, workingDirectory, System.out::println, javac);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        reload();
    }

    @SneakyThrows
    public static void reload() {
        for (val thread : activeThreads) {
            thread.interrupt();
        }

        EventLoop.stop();

        val classLoader = new ChildFirstClassLoader(new URL[]{
            Paths.get("build").toUri().toURL(),
            Paths.get("libs", "compile").toUri().toURL(),
            Paths.get("libs", "runtime").toUri().toURL()});

        val addProcess = classLoader.loadClass("cli.Services")
            .getDeclaredMethod("addProcess", String.class, Process.class);

        for (val service : Services.runningOrStartingServices.entrySet()) {
            if (service.getValue().process != null) {
                addProcess.invoke(null, service.getKey().name(), service.getValue().process);
            }
        }

        val restoreActiveScreen = classLoader.loadClass("cli.HotReload")
            .getDeclaredMethod("restoreActiveScreen", String.class);

        restoreActiveScreen.invoke(null, Screen.current.name());

        val init = classLoader.loadClass("cli.Main")
            .getMethod("init");

        val currentClassLoader = HotReload.class.getClassLoader();
        if (currentClassLoader instanceof URLClassLoader urlClassLoader) {
            urlClassLoader.close();
        }

        setInitMethod(init);
    }

    @SuppressWarnings("unused")
    public static void restoreActiveScreen(String name) {
        val screen = Screen.valueOf(name);
        switch (screen) {
            case docPath -> DocPathScreen.show();
            case services -> ServicesScreen.show();
            case taskSwitcher -> TaskSwitcher.show();
            default -> MainMenu.show();
        }
    }
}
