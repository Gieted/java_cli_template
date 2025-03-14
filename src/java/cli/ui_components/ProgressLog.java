package cli.ui_components;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import cli.Utils;

import java.util.ArrayList;
import java.util.List;

import static cli.EventLoop.scheduleMacrotask;
import static cli.Ui.updateUi;

public class ProgressLog {
    @RequiredArgsConstructor
    public static class Entry {
        public final String text;
        public boolean isCompleted;
        public long startTime;
        public String duration;
    }

    public List<Entry> entries = new ArrayList<>();

    public String draw() {
        val result = new StringBuilder();

        for (val entry : entries) {
            result.append(" ");
            String time;
            if (entry.isCompleted) {
                result.append("✅ ");
                time = entry.duration;
            } else {
                result.append(LoadingAnimation.draw()).append(" ");
                time = String.format("%.2f", (System.currentTimeMillis() - entry.startTime) / 1000.0);
            }
            result.append(entry.text).append(" • ").append(time).append("s\n");
        }

        return result.toString();
    }

    public Entry add(String text) {
        return add(text, false);
    }

    public Entry add(String text, boolean completed) {
        val entry = new Entry(text);
        entry.isCompleted = completed;
        if (completed) {
            entry.duration = "0";
        } else {
            entry.startTime = System.currentTimeMillis();
        }
        scheduleMacrotask(() -> {
            entries.add(entry);
            updateUi();
        });
        return entry;
    }

    @SneakyThrows
    public <T> T add(String text, Utils.SupplierWithException<T> function) {
        val entry = add(text);
        val result = function.get();
        markAsCompleted(entry);
        return result;
    }

    public void add(String text, Utils.RunnableWithException function) {
        add(text, () -> {
            function.run();
            return null;
        });
    }

    public static void markAsCompleted(Entry entry) {
        scheduleMacrotask(() -> {
            entry.isCompleted = true;
            entry.duration = String.format("%.2f", (System.currentTimeMillis() - entry.startTime) / 1000.0);
            updateUi();
        });
    }
}
