package de.windfwm.core;

import de.windfwm.WindFwm;
import de.windfwm.data.Models;
import net.minecraft.client.MinecraftClient;

/** Misst Spielzeit und zurückgelegte Strecke des eigenen Spielers. */
public final class SessionTracker {
    private long sessionStart;
    private long lastTickTime;
    private double lastX, lastY, lastZ;
    private boolean hasLast;
    private String server = "";
    private double sessionDistance;
    private int ticks;

    public void onJoin() {
        sessionStart = System.currentTimeMillis();
        lastTickTime = sessionStart;
        hasLast = false;
        sessionDistance = 0;
        server = GameContext.serverKey();
        Models.ServerProfile p = WindFwm.data().profile(server);
        p.sessions++;
        WindFwm.data().profiles.markDirty();
        WindFwm.stats().invalidate();
    }

    public void onLeave() {
        sessionStart = 0;
        de.windfwm.money.ScoreboardReader.windTitle = false;
        hasLast = false;
    }

    public long sessionStart() {
        return sessionStart;
    }

    public long sessionLength() {
        return sessionStart == 0 ? 0 : System.currentTimeMillis() - sessionStart;
    }

    public double sessionDistance() {
        return sessionDistance;
    }

    public void tick(MinecraftClient mc) {
        if (mc.player == null || sessionStart == 0) return;
        long now = System.currentTimeMillis();
        Models.ServerProfile p = WindFwm.data().profile(server);
        long dt = now - lastTickTime;
        lastTickTime = now;
        if (dt > 0 && dt < 5000) p.playtimeMs += dt;

        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        if (hasLast) {
            double dx = x - lastX, dy = y - lastY, dz = z - lastZ;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            // Sprünge (Teleport, Respawn) nicht mitzählen
            if (d > 0.001 && d < 10) {
                p.distance += d;
                sessionDistance += d;
            }
        }
        lastX = x;
        lastY = y;
        lastZ = z;
        hasLast = true;
        if (++ticks % 200 == 0) WindFwm.data().profiles.markDirty();
    }
}
