package de.windfwm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.windfwm.WindFwm;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Lädt und speichert ein Objekt als JSON-Datei.
 * Gespeichert wird atomar (temporäre Datei + Verschieben), damit bei einem Absturz
 * keine halb geschriebenen Dateien entstehen. Defekte Dateien werden gesichert statt gelöscht.
 */
public final class JsonStore<T> {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WindFWM-IO");
        t.setDaemon(true);
        return t;
    });

    private final Path file;
    private final Class<T> type;
    private final Supplier<T> fallback;
    private T value;
    private boolean dirty;

    public JsonStore(Path file, Class<T> type, Supplier<T> fallback) {
        this.file = file;
        this.type = type;
        this.fallback = fallback;
    }

    public T get() {
        if (value == null) load();
        return value;
    }

    public void load() {
        T loaded = null;
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                loaded = GSON.fromJson(reader, type);
            } catch (Exception e) {
                WindFwm.LOG.error("[WindFWM] Konnte {} nicht lesen, lege Sicherung an", file.getFileName(), e);
                backupBroken();
            }
        }
        value = loaded != null ? loaded : fallback.get();
        dirty = loaded == null;
    }

    private void backupBroken() {
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".broken-" + System.currentTimeMillis());
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            WindFwm.LOG.error("[WindFWM] Sicherung fehlgeschlagen", e);
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Serialisiert auf dem aktuellen Thread, schreibt im Hintergrund. */
    public void saveAsync() {
        if (value == null) return;
        final String json;
        try {
            json = GSON.toJson(value);
        } catch (Exception e) {
            WindFwm.LOG.error("[WindFWM] Konnte {} nicht serialisieren", file.getFileName(), e);
            return;
        }
        dirty = false;
        IO.execute(() -> write(json));
    }

    /** Synchron speichern (beim Beenden). */
    public void saveNow() {
        if (value == null) return;
        try {
            write(GSON.toJson(value));
            dirty = false;
        } catch (Exception e) {
            WindFwm.LOG.error("[WindFWM] Konnte {} nicht speichern", file.getFileName(), e);
        }
    }

    private synchronized void write(String json) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            WindFwm.LOG.error("[WindFWM] Schreiben von {} fehlgeschlagen", file.getFileName(), e);
        }
    }

    /** Wartet kurz, bis ausstehende Schreibvorgänge erledigt sind. */
    public static void flushIo() {
        try {
            IO.submit(() -> { }).get(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // beim Beenden nicht blockieren
        }
    }
}
