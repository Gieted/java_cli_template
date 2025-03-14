package cli;

import lombok.SneakyThrows;
import lombok.val;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static cli.EventLoop.scheduleMacrotask;
import static cli.HotReload.startThread;
import static cli.Services.log;
import static cli.Utils.completeFuture;
import static cli.Utils.sneakyThrows;

public class Build {
    @SneakyThrows
    public static String buildServiceWithMaven(Path serviceDirectory, List<String> logs, Path logFile) {
        val providedClasspath = new CompletableFuture<String>();

        scheduleMacrotask(() ->
            startThread(() ->
                completeFuture(providedClasspath, () ->
                    useMaven(
                        serviceDirectory,
                        logs,
                        logFile,
                        "mvn", "dependency:build-classpath", "-DincludeScope=provided"
                    ))));

        return useMaven(
            serviceDirectory,
            logs,
            logFile,
            "mvn", "compile", "dependency:build-classpath", "-DincludeScope=runtime"
        ) + ":" + providedClasspath.get();
    }

    @SneakyThrows
    public static String buildServiceWithoutMaven(Path serviceDirectory, List<String> logs, Path logFile) {
        val classpath = useMaven(
            serviceDirectory,
            logs,
            logFile,
            "mvn", "dependency:build-classpath", "-DincludeScope=compile"
        );

        val classes = serviceDirectory.resolve(Paths.get("target", "classes"));

        // copy resources
        {
            val time = System.currentTimeMillis();
            val resources = serviceDirectory.resolve(Paths.get("src", "main", "resources"));
            try (val files = Files.walk(resources)) {
                files.forEach(sneakyThrows(file -> {
                    if (Files.isDirectory(file)) {
                        return;
                    }

                    val relativePath = resources.relativize(file);
                    val targetPath = classes.resolve(relativePath);
                    log(logs, logFile, "Copying " + relativePath);

                    Files.createDirectories(targetPath.getParent());

                    if (relativePath.toString().equals("application.yaml")) {
                        val content = Files.readString(file);
                        Files.writeString(
                            targetPath,
                            content.replaceFirst("@project\\.version@", "0.0.1")
                        );
                    } else {
                        Files.copy(
                            file,
                            targetPath,
                            REPLACE_EXISTING
                        );
                    }
                }));
            }

            log(logs, logFile, "Copying resources took: " + (System.currentTimeMillis() - time) + " ms");
        }

        // compile
        {
            val time = System.currentTimeMillis();

            val sourcePath = serviceDirectory.resolve(Paths.get("src", "main", "java"));
            compileJava(sourcePath, classpath, classes, serviceDirectory, log -> log(logs, logFile, log), "javac");

            log(logs, logFile, "Compilation took: " + (System.currentTimeMillis() - time) + " ms");
        }

        return useMaven(
            serviceDirectory,
            logs,
            logFile,
            "mvn", "dependency:build-classpath", "-DincludeScope=runtime"
        ) + ":" + useMaven(
            serviceDirectory,
            logs,
            logFile,
            "mvn", "dependency:build-classpath", "-DincludeScope=provided"
        );
    }

    @SneakyThrows
    private static String useMaven(Path path, List<String> logs, Path logFile, String... command) {
        val process = Runtime.getRuntime().exec(command, null, path.toFile());
        val scanner = new Scanner(process.getInputStream());

        var classpathNextLine = false;
        String classpath = null;
        while (scanner.hasNext()) {
            val nextLine = scanner.nextLine();

            if (classpathNextLine) {
                classpath = nextLine;
                classpathNextLine = false;
            } else if (nextLine.contains("Dependencies classpath:")) {
                classpathNextLine = true;
            }

            log(logs, logFile, nextLine);
        }

        if (classpath == null || process.waitFor() != 0) {
            throw new RuntimeException("Build error");
        }

        return classpath;
    }

    @SneakyThrows
    public static void compileJava(
        Path sourcePath,
        String classpath,
        Path outputPath,
        Path workingDirectory,
        Consumer<String> log,
        String javac
    ) {
        val command = new ArrayList<>(List.of(
            javac,
            "-d", outputPath.toString(),
            "-classpath", classpath,
            "-encoding", "UTF-8",
            "-proc:full", // needed to enable Lombok in Java 22
            "-XDignore.symbol.file", // disables warning about proprietary sun API usage
            "-parameters"
        ));

        try (val files = Files.walk(sourcePath)) {
            files.forEach(sneakyThrows(file -> {
                if (!file.getFileName().toString().endsWith(".java") || Files.isDirectory(file)) {
                    return;
                }

                val relativePath = workingDirectory.relativize(file);
                command.add(relativePath.toString());
            }));
        }

        val process = Runtime.getRuntime().exec(command.toArray(new String[0]), null, workingDirectory.toFile());

        val scanner = new Scanner(process.getInputStream());
        while (scanner.hasNext()) {
            log.accept(scanner.nextLine());
        }

        val errorScanner = new Scanner(process.getErrorStream());
        while (errorScanner.hasNext()) {
            log.accept(errorScanner.nextLine());
        }

        if (process.waitFor() != 0) {
            throw new RuntimeException("Build error");
        }
    }
}
