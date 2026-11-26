package de.windfwm.core;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.network.ServerInfo;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Liest öffentlich sichtbare Informationen des eigenen Clients:
 * Serveradresse, eigene Position, Dimension und die Tabliste.
 * Es werden keine anderen Spieler verfolgt oder geortet.
 */
public final class GameContext {
    private static Set<String> onlineCache = new HashSet<>();
    private static long onlineCacheTime;

    private GameContext() {}

    public static MinecraftClient mc() {
        return MinecraftClient.getInstance();
    }

    public static boolean inWorld() {
        return mc().player != null && mc().world != null;
    }

    /** Eindeutiger Schlüssel für den aktuellen Server (für getrennte Daten je Server). */
    public static String serverKey() {
        MinecraftClient mc = mc();
        ServerInfo info = mc.getCurrentServerEntry();
        if (info != null && info.address != null && !info.address.isBlank()) {
            String a = info.address.toLowerCase(Locale.ROOT).trim();
            if (a.endsWith(":25565")) a = a.substring(0, a.length() - 6);
            return a;
        }
        if (mc.isInSingleplayer()) return "singleplayer";
        return "offline";
    }

    /** WindSMP wird an der Serveradresse (z. B. play.windsmp.com) oder am Sidebar-Titel erkannt. */
    public static boolean isWindSmp() {
        return serverKey().contains("windsmp") || (inWorld() && de.windfwm.money.ScoreboardReader.windTitle);
    }

    public static String serverLabel() {
        String key = serverKey();
        if (isWindSmp()) return "WindSMP";
        if (key.equals("singleplayer")) return "Einzelspieler";
        if (key.equals("offline")) return "Nicht verbunden";
        return key;
    }

    public static String playerName() {
        MinecraftClient mc = mc();
        if (mc.player != null) return mc.player.getName().getString();
        return mc.getSession().getUsername();
    }

    public static String dimension() {
        MinecraftClient mc = mc();
        if (mc.world == null) return "";
        return mc.world.getRegistryKey().getValue().toString();
    }

    public static String dimensionLabel(String dim) {
        if (dim == null) return "?";
        return switch (dim) {
            case "minecraft:overworld" -> "Oberwelt";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dim.contains(":") ? dim.substring(dim.indexOf(':') + 1) : dim;
        };
    }

    public static int dimensionColor(String dim) {
        if (dim == null) return 0xFF8A9199;
        return switch (dim) {
            case "minecraft:the_nether" -> 0xFFFF7A6B;
            case "minecraft:the_end" -> 0xFFC79BFF;
            default -> 0xFF6FDC9C;
        };
    }

    public static int ping() {
        MinecraftClient mc = mc();
        ClientPlayNetworkHandler h = mc.getNetworkHandler();
        if (h == null || mc.player == null) return -1;
        PlayerListEntry e = h.getPlayerListEntry(mc.player.getUuid());
        return e == null ? -1 : e.getLatency();
    }

    public static int onlineCount() {
        ClientPlayNetworkHandler h = mc().getNetworkHandler();
        return h == null ? 0 : h.getPlayerList().size();
    }

    /** Namen aus der Tabliste (kleingeschrieben), höchstens einmal pro Sekunde neu gelesen. */
    public static Set<String> onlineNames() {
        long now = System.currentTimeMillis();
        if (now - onlineCacheTime < 1000) return onlineCache;
        onlineCacheTime = now;
        Set<String> names = new HashSet<>();
        ClientPlayNetworkHandler h = mc().getNetworkHandler();
        if (h != null) {
            for (PlayerListEntry e : h.getPlayerList()) {
                if (e.getProfile() != null && e.getProfile().name() != null) {
                    names.add(e.getProfile().name().toLowerCase(Locale.ROOT));
                }
            }
        }
        onlineCache = names;
        return names;
    }

    /** Originalschreibweise der Online-Spieler (für "aus Tabliste hinzufügen"). */
    public static java.util.List<String> onlineNamesSorted() {
        java.util.List<String> out = new java.util.ArrayList<>();
        ClientPlayNetworkHandler h = mc().getNetworkHandler();
        if (h != null) {
            for (PlayerListEntry e : h.getPlayerList()) {
                if (e.getProfile() != null && e.getProfile().name() != null) out.add(e.getProfile().name());
            }
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static boolean isOnline(String name) {
        return name != null && onlineNames().contains(name.toLowerCase(Locale.ROOT));
    }
}
