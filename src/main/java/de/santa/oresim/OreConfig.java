package de.santa.oresim;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

public final class OreConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("santa-oresim.json");

    public boolean enabled = true;
    public boolean hasSeed = true;
    public long seed = -5746398532788178098L;
    public int radius = 100;
    public boolean diamonds = true;
    public boolean debris = true;
    /** Boxen ausblenden, wenn an der Stelle sichtbar Luft/anderer Block ist. */
    public boolean filter = true;

    public static OreConfig INSTANCE = new OreConfig();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                OreConfig c = GSON.fromJson(Files.readString(FILE), OreConfig.class);
                if (c != null) INSTANCE = c;
            }
        } catch (Exception e) {
            SantaOreSim.LOGGER.warn("Config konnte nicht geladen werden", e);
        }
        INSTANCE.radius = Math.max(16, Math.min(256, INSTANCE.radius));
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            SantaOreSim.LOGGER.warn("Config konnte nicht gespeichert werden", e);
        }
    }
}
