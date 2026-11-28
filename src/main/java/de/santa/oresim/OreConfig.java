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

    // Aussehen
    public int diamondColor = 0x33E8FF;
    public int debrisColor = 0xFF7A1F;
    /** Deckkraft der Fuellung in Prozent (0-100). */
    public int fillOpacity = 19;
    /** Linienstaerke in Zehntel-Pixeln (10-60). */
    public int lineWidth = 30;
    public boolean fill = true;
    public boolean throughWalls = true;
    public boolean rainbow = false;

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
        INSTANCE.fillOpacity = Math.max(0, Math.min(100, INSTANCE.fillOpacity));
        INSTANCE.lineWidth = Math.max(10, Math.min(60, INSTANCE.lineWidth));
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
