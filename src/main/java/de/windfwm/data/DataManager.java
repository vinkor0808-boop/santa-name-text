package de.windfwm.data;

import de.windfwm.config.WindConfig;
import de.windfwm.util.JsonStore;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.List;

/** Zentrale Stelle für alle gespeicherten Daten (config/windfwm/*.json). */
public final class DataManager {
    private static final int SAVE_INTERVAL_TICKS = 100; // 5 Sekunden

    public final Path dir;
    public final JsonStore<WindConfig> config;
    public final JsonStore<Models.Profiles> profiles;
    public final JsonStore<Models.ShopData> shops;
    public final JsonStore<Models.PlayerData> players;
    public final JsonStore<Models.WaypointData> waypoints;
    private final List<JsonStore<?>> all;
    private int ticks;

    public DataManager() {
        dir = FabricLoader.getInstance().getConfigDir().resolve("windfwm");
        config = new JsonStore<>(dir.resolve("config.json"), WindConfig.class, WindConfig::new);
        profiles = new JsonStore<>(dir.resolve("profiles.json"), Models.Profiles.class, Models.Profiles::new);
        shops = new JsonStore<>(dir.resolve("shops.json"), Models.ShopData.class, Models.ShopData::new);
        players = new JsonStore<>(dir.resolve("players.json"), Models.PlayerData.class, Models.PlayerData::new);
        waypoints = new JsonStore<>(dir.resolve("waypoints.json"), Models.WaypointData.class, Models.WaypointData::new);
        all = List.of(config, profiles, shops, players, waypoints);
    }

    public void loadAll() {
        for (JsonStore<?> s : all) s.load();
        sanitize();
    }

    /** Fehlende Teile (z. B. aus alten oder handbearbeiteten Dateien) ergänzen. */
    private void sanitize() {
        WindConfig c = config.get();
        if (c.general == null) c.general = new WindConfig.General();
        if (c.money == null) c.money = new WindConfig.Money();
        if (c.money.rules == null || c.money.rules.isEmpty()) c.money.rules = de.windfwm.money.MoneyRule.defaults();
        migrateRules(c);
        if (c.money.balanceKeywords == null) c.money.balanceKeywords = new WindConfig.Money().balanceKeywords;
        if (c.stats == null) c.stats = new WindConfig.Stats();
        if (c.chat == null) c.chat = new WindConfig.Chat();
        if (c.chat.hidden == null) c.chat.hidden = new java.util.ArrayList<>();
        if (c.chat.highlight == null) c.chat.highlight = new java.util.ArrayList<>();
        if (c.waypoints == null) c.waypoints = new WindConfig.Waypoints();
        if (c.hud == null) c.hud = new WindConfig.Hud();
        if (c.windows == null) c.windows = new java.util.HashMap<>();
        if (profiles.get().servers == null) profiles.get().servers = new java.util.HashMap<>();
        if (shops.get().shops == null) shops.get().shops = new java.util.ArrayList<>();
        if (shops.get().lists == null) shops.get().lists = new java.util.ArrayList<>();
        if (players.get().players == null) players.get().players = new java.util.ArrayList<>();
        if (waypoints.get().list == null) waypoints.get().list = new java.util.ArrayList<>();
    }

    /** Fehlende Standardregeln (z. B. neue WindSMP-Regeln) in bestehende Konfigurationen einfügen. */
    private void migrateRules(WindConfig c) {
        java.util.List<de.windfwm.money.MoneyRule> defaults = de.windfwm.money.MoneyRule.defaults();
        int insertAt = 0;
        for (de.windfwm.money.MoneyRule d : defaults) {
            boolean exists = c.money.rules.stream().anyMatch(r -> d.id.equals(r.id));
            if (!exists && d.id.startsWith("wind_")) {
                c.money.rules.add(insertAt++, d);
                config.markDirty();
            }
        }
        // alte doppelte Regel abschalten, die jetzt von "wind_order_payment" abgedeckt wird
        for (de.windfwm.money.MoneyRule r : c.money.rules) {
            if (java.util.Set.of("order_payment", "pay_in", "pay_out").contains(r.id) && r.enabled) {
                r.enabled = false;
                config.markDirty();
            }
        }
        if (c.version < 2) {
            c.version = 2;
            config.markDirty();
        }
    }

    public WindConfig cfg() {
        return config.get();
    }

    public Models.ServerProfile profile(String server) {
        Models.Profiles p = profiles.get();
        Models.ServerProfile sp = p.servers.get(server);
        if (sp == null) {
            sp = new Models.ServerProfile();
            p.servers.put(server, sp);
            profiles.markDirty();
        }
        if (sp.entries == null) sp.entries = new java.util.ArrayList<>();
        return sp;
    }

    public void tick() {
        if (++ticks % SAVE_INTERVAL_TICKS != 0) return;
        for (JsonStore<?> s : all) if (s.isDirty()) s.saveAsync();
    }

    public void saveAllNow() {
        JsonStore.flushIo(); // zuerst ausstehende (ältere) Hintergrund-Schreibvorgänge abschließen
        for (JsonStore<?> s : all) s.saveNow();
    }
}
