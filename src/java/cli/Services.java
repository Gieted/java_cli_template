package cli;

import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import cli.screens.LogViewer;
import cli.screens.ServicesScreen;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.nio.file.StandardOpenOption.APPEND;
import static cli.Build.buildServiceWithMaven;
import static cli.Build.buildServiceWithoutMaven;
import static cli.EventLoop.scheduleMacrotask;
import static cli.HotReload.startThread;
import static cli.Ui.updateUi;
import static cli.Ui.Screen;

public class Services {
    @RequiredArgsConstructor
    public enum Service {
        // insert service names here
        ;

        final public String name;
    }

    @RequiredArgsConstructor
    public static final class ServiceData {
        public Process process;
        public final List<String> logs = new ArrayList<>(1000);
    }

    private record LogEntry(String payload) {
    }

    public static final Map<Service, ServiceData> runningOrStartingServices = new HashMap<>();
    private static final Gson gson = new Gson();

    public static boolean isStarting(Service service) {
        val data = runningOrStartingServices.get(service);
        return data != null && data.process == null;
    }

    public static boolean isRunning(Service service) {
        val data = runningOrStartingServices.get(service);
        return data != null && data.process != null && data.process.isAlive();
    }

    @SuppressWarnings("unused")
    public static void addProcess(String serviceName, Process process) {
        val service = Service.valueOf(serviceName);
        val data = new ServiceData();
        data.process = process;
        runningOrStartingServices.put(service, data);
        process.onExit().thenRun(() ->
            scheduleMacrotask(() -> {
                if (runningOrStartingServices.remove(service, data)) {
                    updateUi();
                }
            }));
    }

    @SneakyThrows
    public static void startService(Service service) {
        if (isRunning(service) || isStarting(service)) {
            return;
        }

        Config.load();
        val serviceDirectory = Paths.get(Config.get(service.name + "_service.path"));
        val mainClass = Config.get(service.name + "_service.main_class");
        val buildWithoutMaven = Boolean.parseBoolean(Config.get("build_without_maven"));

        val data = new ServiceData();
        runningOrStartingServices.put(service, data);

        startThread(() -> {
            val logFile = Paths.get("logs", service.name + ".txt");

            Files.createDirectories(logFile.getParent());
            Files.writeString(logFile, "");

            // build
            String runtimeClasspath;
            try {
                val time = System.currentTimeMillis();

                runtimeClasspath = buildWithoutMaven
                    ? buildServiceWithoutMaven(serviceDirectory, data.logs, logFile)
                    : buildServiceWithMaven(serviceDirectory, data.logs, logFile);

                log(data.logs, logFile, "Build successful in " + (System.currentTimeMillis() - time) + " ms");
            } catch (Exception e) {
                val writer = new StringWriter();
                e.printStackTrace(new PrintWriter(writer));
                String exceptionString = writer.toString();
                log(data.logs, logFile, exceptionString);
                return;
            }

            data.process = Runtime.getRuntime().exec(
                new String[]{
                    "java",
                    "-Dspring.output.ansi.enabled=always",
                    "-Dspring.profiles.active=cloud,local-ports",
                    "-Dfile.encoding=UTF-8",
                    "-classpath", "target/classes/" + ":" + runtimeClasspath,
                    mainClass
                }, null, serviceDirectory.toFile()
            );

            updateUi();

            val scanner = new Scanner(data.process.getInputStream());

            while (scanner.hasNext()) {
                val nextLine = scanner.nextLine();
                String payload;
                try {
                    val logEntry = gson.fromJson(nextLine, LogEntry.class);
                    payload = logEntry.payload;
                } catch (Exception _) {
                    payload = nextLine;
                }
                if (payload == null) {
                    payload = nextLine;
                }

                log(data.logs, logFile, payload);
            }

            val errorScanner = new Scanner(data.process.getErrorStream());
            while (errorScanner.hasNext()) {
                log(data.logs, logFile, errorScanner.nextLine());
            }

            scheduleMacrotask(() -> {
                if (runningOrStartingServices.remove(service, data)) {
                    updateUi();
                }
            });
        });
    }

    @SneakyThrows
    public static void log(List<String> logs, Path logFile, String log) {
        if (log == null) {
            System.out.println("test");
        }
        Files.writeString(logFile, log + '\n', APPEND);
        scheduleMacrotask(() -> {
            logs.add(log);
            LogViewer.listView.items.add(log);

            if (Screen.current == Screen.services && ServicesScreen.state.showLogs) {
                updateUi();
            }
        });
    }

    public static void stopService(Service service) {
        val data = runningOrStartingServices.get(service);
        if (data != null && data.process != null) {
            data.process.destroy();
            runningOrStartingServices.remove(service);
        }
    }

    public static void stopAllServices() {
        for (val data : new ArrayList<>(runningOrStartingServices.values())) {
            if (data.process != null) {
                data.process.destroy();
            }
        }
    }

    @SneakyThrows
    public static void killService(Service service) {
        Config.load();
        val port = Config.get(service.name + "_service.port");
        if (port == null) {
            throw new RuntimeException("Please set port");
        }

        startThread(() -> {
            val process = Runtime.getRuntime().exec(new String[]{"lsof", "-i", "tcp:" + port});
            val scanner = new Scanner(process.getInputStream());
            if (!scanner.hasNextLine()) {
                return;
            }
            scanner.nextLine();
            scanner.next();
            val pid = scanner.next();
            Runtime.getRuntime().exec(new String[]{"kill", "-9", pid});
        });
    }
}
