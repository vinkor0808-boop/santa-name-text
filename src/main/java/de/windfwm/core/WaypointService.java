package de.windfwm.core;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.data.Models;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Hilfsfunktionen rund um Waypoints (eigene Position, Entfernung, Richtung). */
public final class WaypointService {
    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};

    private WaypointService() {}

    public static List<Models.Waypoint> all() {
        return WindFwm.data().waypoints.get().list;
    }

    /** Waypoints gemäß Filtereinstellungen, nach Entfernung sortiert. */
    public static List<Models.Waypoint> visible() {
        WindConfig.Waypoints c = WindFwm.cfg().waypoints;
        String server = GameContext.serverKey();
        String dim = GameContext.dimension();
        List<Models.Waypoint> out = new ArrayList<>();
        for (Models.Waypoint w : all()) {
            if (c.currentServerOnly && !server.equals(w.server)) continue;
            if (c.currentDimensionOnly && !dim.equals(w.dimension)) continue;
            out.add(w);
        }
        out.sort(Comparator.comparingDouble(WaypointService::sortDistance));
        return out;
    }

    private static double sortDistance(Models.Waypoint w) {
        double d = distance(w);
        return d < 0 ? Double.MAX_VALUE : d;
    }

    public static Models.Waypoint addAtPlayer(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return null;
        Models.Waypoint w = new Models.Waypoint();
        w.name = name == null || name.isBlank() ? "Waypoint " + (all().size() + 1) : name.trim();
        w.x = (int) Math.floor(mc.player.getX());
        w.y = (int) Math.floor(mc.player.getY());
        w.z = (int) Math.floor(mc.player.getZ());
        w.dimension = GameContext.dimension();
        w.server = GameContext.serverKey();
        w.color = WindFwm.cfg().general.accent;
        all().add(w);
        WindFwm.data().waypoints.markDirty();
        return w;
    }

    public static Models.Waypoint add(String name, int x, int y, int z) {
        Models.Waypoint w = new Models.Waypoint();
        w.name = name == null || name.isBlank() ? "Waypoint " + (all().size() + 1) : name.trim();
        w.x = x;
        w.y = y;
        w.z = z;
        w.dimension = GameContext.dimension().isEmpty() ? "minecraft:overworld" : GameContext.dimension();
        w.server = GameContext.serverKey();
        w.color = WindFwm.cfg().general.accent;
        all().add(w);
        WindFwm.data().waypoints.markDirty();
        return w;
    }

    public static void remove(Models.Waypoint w) {
        all().remove(w);
        if (w.id.equals(WindFwm.cfg().waypoints.trackedId)) {
            WindFwm.cfg().waypoints.trackedId = null;
            WindFwm.data().config.markDirty();
        }
        WindFwm.data().waypoints.markDirty();
    }

    /** @return Entfernung in Blöcken oder -1 (andere Dimension / nicht in einer Welt). */
    public static double distance(Models.Waypoint w) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !GameContext.dimension().equals(w.dimension)) return -1;
        double dx = w.x + 0.5 - mc.player.getX();
        double dy = w.y - mc.player.getY();
        double dz = w.z + 0.5 - mc.player.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** Richtungspfeil relativ zur Blickrichtung. */
    public static String arrow(Models.Waypoint w) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !GameContext.dimension().equals(w.dimension)) return "•";
        double dx = w.x + 0.5 - mc.player.getX();
        double dz = w.z + 0.5 - mc.player.getZ();
        double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
        double diff = wrap(targetYaw - mc.player.getYaw());
        int idx = (int) Math.round(diff / 45.0);
        idx = ((idx % 8) + 8) % 8;
        return ARROWS[idx];
    }

    private static double wrap(double deg) {
        deg %= 360.0;
        if (deg >= 180) deg -= 360;
        if (deg < -180) deg += 360;
        return deg;
    }

    public static Models.Waypoint tracked() {
        String id = WindFwm.cfg().waypoints.trackedId;
        if (id == null) return null;
        for (Models.Waypoint w : all()) if (w.id.equals(id)) return w;
        return null;
    }

    /** Umrechnung Oberwelt <-> Nether als Hinweis. */
    public static String netherHint(Models.Waypoint w) {
        if ("minecraft:overworld".equals(w.dimension)) return "Nether: " + Math.floorDiv(w.x, 8) + " / " + Math.floorDiv(w.z, 8);
        if ("minecraft:the_nether".equals(w.dimension)) return "Oberwelt: " + (w.x * 8) + " / " + (w.z * 8);
        return "";
    }
}
