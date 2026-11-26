package de.windfwm.feature;

import de.windfwm.WindFwm;
import de.windfwm.core.GameContext;
import de.windfwm.data.Models;
import de.windfwm.util.AmountParser;
import de.windfwm.util.Fmt;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Zählt WindSMP-Orders anhand der Chatzeilen
 * "Order payment: +$800K" und "Claimed 7 ordered item(s)." (auf WindSMP bestätigt).
 */
public final class OrderTracker {
    private static final Pattern PAYMENT = Pattern.compile("^Order payment: \\+\\$ ?([0-9][0-9.,]*\\s?[KMBTkmbt]?)");
    private static final Pattern CLAIMED = Pattern.compile("^Claimed ([0-9][0-9.,]*) ordered item");
    private static final int MAX_ENTRIES = 20000;

    private long sessionStart;

    // zwischengespeicherte Statistik
    public double sessionTotal, todayTotal, allTotal, perHour, average, biggest;
    public int sessionCount, todayCount, allCount, sessionClaimed, todayClaimed, allClaimed;
    private long lastCompute;
    private int lastSize = -1;

    public void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay) return;
            try {
                handle(Fmt.strip(message.getString()));
            } catch (Exception e) {
                WindFwm.LOG.error("[WindFWM] Order-Erkennung fehlgeschlagen", e);
            }
        });
    }

    public void onJoin() {
        sessionStart = System.currentTimeMillis();
        lastSize = -1;
    }

    public void onLeave() {
        sessionStart = 0;
    }

    public long sessionStart() {
        return sessionStart;
    }

    public Models.OrderProfile profile() {
        return WindFwm.data().orders(GameContext.serverKey());
    }

    /** Für Tests und den Chat-Listener. @return true, wenn die Zeile erkannt wurde */
    public boolean handle(String line) {
        Matcher m = PAYMENT.matcher(line);
        if (m.find()) {
            double v = AmountParser.parse(m.group(1));
            if (!Double.isNaN(v)) {
                Models.OrderProfile p = profile();
                p.payments.add(new Models.Payment(System.currentTimeMillis(), v));
                if (p.payments.size() > MAX_ENTRIES) p.payments.remove(0);
                changed();
                return true;
            }
        }
        m = CLAIMED.matcher(line);
        if (m.find()) {
            double n = AmountParser.parse(m.group(1));
            if (!Double.isNaN(n)) {
                Models.OrderProfile p = profile();
                p.claims.add(new Models.Claim(System.currentTimeMillis(), (int) Math.round(n)));
                if (p.claims.size() > MAX_ENTRIES) p.claims.remove(0);
                changed();
                return true;
            }
        }
        return false;
    }

    private void changed() {
        WindFwm.data().orders.markDirty();
        lastSize = -1;
    }

    public void reset() {
        Models.OrderProfile p = profile();
        p.payments.clear();
        p.claims.clear();
        changed();
    }

    /** Statistik höchstens einmal pro Sekunde neu berechnen. */
    public void update() {
        Models.OrderProfile p = profile();
        long now = System.currentTimeMillis();
        int size = p.payments.size() + p.claims.size();
        if (size == lastSize && now - lastCompute < 1000) return;
        lastSize = size;
        lastCompute = now;
        long today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long hourAgo = now - 3_600_000L;
        sessionTotal = todayTotal = allTotal = biggest = 0;
        sessionCount = todayCount = allCount = 0;
        double lastHour = 0;
        for (Models.Payment pay : p.payments) {
            allTotal += pay.amount;
            allCount++;
            biggest = Math.max(biggest, pay.amount);
            if (pay.t >= today) {
                todayTotal += pay.amount;
                todayCount++;
            }
            if (sessionStart > 0 && pay.t >= sessionStart) {
                sessionTotal += pay.amount;
                sessionCount++;
            }
            if (pay.t >= hourAgo) lastHour += pay.amount;
        }
        sessionClaimed = todayClaimed = allClaimed = 0;
        for (Models.Claim c : p.claims) {
            allClaimed += c.count;
            if (c.t >= today) todayClaimed += c.count;
            if (sessionStart > 0 && c.t >= sessionStart) sessionClaimed += c.count;
        }
        average = allCount > 0 ? allTotal / allCount : 0;
        // kürzere Sitzung als 1 h: auf die tatsächliche Dauer hochrechnen (mind. 5 min)
        long span = 3_600_000L;
        if (sessionStart > 0) span = Math.min(span, Math.max(5 * 60_000L, now - sessionStart));
        perHour = lastHour / (span / 3_600_000.0);
    }
}
