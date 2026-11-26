package de.windfwm.data;

import de.windfwm.config.WindConfig;
import de.windfwm.util.JsonStore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Laden/Speichern aller Dateien in config/windfwm/. */
public final class DataManager {
    private static final int SAVE_INTERVAL_TICKS = 100;

    public final JsonStore<WindConfig> config;
    public final JsonStore<Models.OrderData> orders;
    private final List<JsonStore<?>> all;
    private int ticks;

    public DataManager() {
        Path dir = FabricLoader.getInstance().getConfigDir().resolve("windfwm");
        // eigene Dateinamen, damit alte WindFWM-1.x-Dateien nicht stören
        config = new JsonStore<>(dir.resolve("addon-config.json"), WindConfig.class, WindConfig::new);
        orders = new JsonStore<>(dir.resolve("orders.json"), Models.OrderData.class, Models.OrderData::new);
        all = List.of(config, orders);
    }

    public void loadAll() {
        for (JsonStore<?> s : all) s.load();
        WindConfig c = config.get();
        if (c.general == null) c.general = new WindConfig.General();
        if (c.quick == null) c.quick = new ArrayList<>();
        if (c.auto == null) c.auto = new WindConfig.Auto();
        if (c.auto.messages == null) c.auto.messages = new ArrayList<>();
        if (c.orders == null) c.orders = new WindConfig.Orders();
        if (orders.get().servers == null) orders.get().servers = new HashMap<>();
    }

    public WindConfig cfg() {
        return config.get();
    }

    public Models.OrderProfile orders(String server) {
        Models.OrderProfile p = orders.get().servers.computeIfAbsent(server, k -> new Models.OrderProfile());
        if (p.payments == null) p.payments = new ArrayList<>();
        if (p.claims == null) p.claims = new ArrayList<>();
        return p;
    }

    public void tick() {
        if (++ticks % SAVE_INTERVAL_TICKS != 0) return;
        for (JsonStore<?> s : all) if (s.isDirty()) s.saveAsync();
    }

    public void saveAllNow() {
        JsonStore.flushIo();
        for (JsonStore<?> s : all) s.saveNow();
    }
}
