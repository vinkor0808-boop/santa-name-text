package de.windfwm.feature;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.ChatSender;
import de.windfwm.core.GameContext;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Automatische Chatnachrichten in einstellbaren Abständen.
 * Schutz vor Spam: Mindestabstand 30 s pro Nachricht, höchstens eine Nachricht alle 5 s,
 * nach dem Betreten eines Servers erst nach Ablauf des Intervalls, keine Befehle (/…).
 */
public final class AutoMessages {
    public static final int MIN_INTERVAL = 30;
    public static final int MAX_INTERVAL = 24 * 3600;
    private static final long GLOBAL_GAP_MS = 5000;

    private final Map<WindConfig.AutoMessage, Long> lastSent = new IdentityHashMap<>();
    private long lastAny;
    private long joinedAt;

    public void onJoin() {
        joinedAt = System.currentTimeMillis();
        lastSent.clear();
    }

    public void onLeave() {
        joinedAt = 0;
        lastSent.clear();
    }

    public static boolean isCommand(String text) {
        return text != null && text.trim().startsWith("/");
    }

    /** Sekunden bis zur nächsten Sendung (oder -1, wenn inaktiv). */
    public long secondsUntilNext(WindConfig.AutoMessage m) {
        if (!running() || !m.enabled || !valid(m)) return -1;
        long base = lastSent.getOrDefault(m, joinedAt);
        long due = base + clamp(m.intervalSeconds) * 1000L;
        return Math.max(0, (due - System.currentTimeMillis()) / 1000);
    }

    public boolean running() {
        WindConfig.Auto c = WindFwm.cfg().auto;
        return c.enabled && joinedAt > 0 && GameContext.inWorld() && (!c.onlyWindSmp || GameContext.isWindSmp());
    }

    public static boolean valid(WindConfig.AutoMessage m) {
        return m.text != null && !m.text.isBlank() && !isCommand(m.text);
    }

    public static int clamp(int seconds) {
        return Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, seconds));
    }

    public void tick() {
        if (!running()) return;
        long now = System.currentTimeMillis();
        if (now - lastAny < GLOBAL_GAP_MS) return;
        for (WindConfig.AutoMessage m : WindFwm.cfg().auto.messages) {
            if (!m.enabled || !valid(m)) continue;
            long base = lastSent.getOrDefault(m, joinedAt);
            if (now - base >= clamp(m.intervalSeconds) * 1000L) {
                if (ChatSender.send(m.text)) {
                    lastSent.put(m, now);
                    lastAny = now;
                }
                return; // pro Durchlauf höchstens eine Nachricht
            }
        }
    }
}
