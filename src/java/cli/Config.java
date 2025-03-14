package cli;

import lombok.SneakyThrows;
import lombok.val;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Config {
    private static Map<String, @Nullable String> config;
    private static List<String> lineFormatting;
    private static List<String> newEntries = new ArrayList<>();

    @SneakyThrows
    public static void load() {
        val string = Files.readString(Paths.get("config.txt"));
        config = new LinkedHashMap<>();
        lineFormatting = new ArrayList<>();
        String key = null;
        val buffer = new StringBuilder();

        for (val token : string.toCharArray()) {
            switch (token) {
                case '\t', '\r' -> {
                }

                case ' ' -> {
                    if (key != null && !buffer.isEmpty()) { // do not escape spaces in values
                        buffer.append(token);
                    }
                }

                case '\n' -> {
                    if (key == null) {
                        if (buffer.isEmpty()) {
                            lineFormatting.add("");
                            continue;
                        }

                        throw new RuntimeException("No value");
                    }

                    config.put(key, buffer.isEmpty() ? null : buffer.toString());
                    lineFormatting.add(key);
                    key = null;
                    buffer.setLength(0);
                }

                case '=' -> {
                    if (key != null) {
                        throw new RuntimeException("Multiple equal signs");
                    }

                    if (buffer.isEmpty()) {
                        throw new RuntimeException("Empty key");
                    }

                    key = buffer.toString();
                    buffer.setLength(0);
                }

                default -> buffer.append(token);
            }
        }
    }

    public static String get(String key) {
        return config.get(key);
    }

    public static void set(String key, String value) {
        if (config.put(key, value) == null) {
            newEntries.add(key);
        }
    }

    @SneakyThrows
    public static void save() {
        val buffer = new StringBuilder();

        for (val entry : lineFormatting) {
            if (entry.isEmpty()) {
                buffer.append('\n');
                continue;
            }

            val value = config.get(entry);
            if (value == null) {
                continue;
            }

            buffer.append(entry).append(" = ").append(value).append('\n');
        }

        if (!newEntries.isEmpty()) {
            buffer.append("\n");
        }

        for (val entry : newEntries) {
            buffer.append(entry).append(" = ").append(config.get(entry)).append('\n');
        }

        Files.writeString(Paths.get("config.txt"), buffer);
        newEntries = new ArrayList<>();
    }

    public static long parseTime(String string) {
        val timeString = new StringBuilder();
        var i = 0;
        for (char character : string.toCharArray()) {
            if (!Character.isDigit(character)) {
                break;
            }

            timeString.append(character);
            i++;
        }

        val time = Long.parseLong(timeString.toString());
        val timeUnit = string.substring(i);

        return switch (timeUnit) {
            case "ms" -> time;
            case "s" -> time * 1000;
            case "m" -> time * 60 * 1000;
            case "h" -> time * 60 * 60 * 1000;
            default -> throw new RuntimeException("Invalid time unit");
        };
    }
}
