package de.windfwm.money;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.GameContext;
import de.windfwm.data.Models;
import de.windfwm.util.AmountParser;

import java.util.List;
import java.util.regex.Matcher;

/**
 * Erfasst Geldbewegungen aus Chat-/Actionbar-Nachrichten (über editierbare Regeln)
 * und den Kontostand aus der Sidebar.
 */
public final class MoneyTracker {
    private String lastActionbar = "";
    private long lastActionbarTime;
    private long lastChatEntryTime;
    private int tickCounter;
    /** Wird bei jeder Änderung erhöht, damit Statistiken wissen, wann neu gerechnet werden muss. */
    private int version;
    public String lastSidebarInfo = "";

    public int version() {
        return version;
    }

    private boolean active() {
        WindConfig c = WindFwm.cfg();
        return !c.general.windSmpOnly || GameContext.isWindSmp();
    }

    public Models.ServerProfile profile() {
        return WindFwm.data().profile(GameContext.serverKey());
    }

    /**
     * Prüft eine Nachricht gegen alle aktiven Regeln.
     * @return die passende Regel oder null
     */
    public MoneyRule handle(String text, boolean actionbar) {
        if (text == null || text.isEmpty() || !active()) return null;
        WindConfig c = WindFwm.cfg();
        if (actionbar) {
            if (!c.money.readActionbar) return null;
            long now = System.currentTimeMillis();
            // Actionbar-Texte werden oft mehrfach gesendet -> doppelte Zählung vermeiden
            if (text.equals(lastActionbar) && now - lastActionbarTime < 3000) {
                lastActionbarTime = now;
                return null;
            }
            lastActionbar = text;
            lastActionbarTime = now;
        }
        for (MoneyRule rule : c.money.rules) {
            if (!rule.enabled || !rule.appliesTo(actionbar)) continue;
            Match match = match(rule, text);
            if (match == null) continue;
            apply(rule, match);
            return rule;
        }
        return null;
    }

    /** Ergebnis eines Regel-Treffers. */
    public record Match(double amount, String name, String item) {}

    /** Testet eine Regel ohne Seiteneffekte (für den Regel-Editor). */
    public static Match match(MoneyRule rule, String text) {
        java.util.regex.Pattern p = rule.compiled();
        if (p == null) return null;
        Matcher m = p.matcher(text);
        if (!m.find()) return null;
        double amount = AmountParser.parse(group(m, "amount"));
        if (Double.isNaN(amount)) return null;
        return new Match(amount, group(m, "name"), group(m, "item"));
    }

    private static String group(Matcher m, String name) {
        try {
            String g = m.group(name);
            return g == null ? null : g.trim();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void apply(MoneyRule rule, Match match) {
        String source = match.name() != null ? match.name() : (match.item() != null ? match.item() : "");
        switch (rule.kind) {
            case MoneyRule.BALANCE -> setBalance(match.amount(), "Chat");
            case MoneyRule.EXPENSE -> addEntry(-match.amount(), rule.category, source, "", false);
            default -> addEntry(match.amount(), rule.category, source, "", false);
        }
    }

    public void addEntry(double signedAmount, String category, String source, String note, boolean manual) {
        Models.ServerProfile p = profile();
        long now = System.currentTimeMillis();
        p.entries.add(new Models.MoneyEntry(now, signedAmount, category, source, note, manual));
        int max = Math.max(100, WindFwm.cfg().money.maxHistory);
        if (p.entries.size() > max) p.entries.subList(0, p.entries.size() - max).clear();
        if (p.hasBalance && !manual) {
            // Schätzung bis die nächste echte Anzeige kommt
            p.balance += signedAmount;
        }
        lastChatEntryTime = now;
        WindFwm.data().profiles.markDirty();
        version++;
    }

    public void setBalance(double value, String source) {
        Models.ServerProfile p = profile();
        p.hasBalance = true;
        p.balance = value;
        p.balanceTime = System.currentTimeMillis();
        p.balanceSource = source;
        WindFwm.data().profiles.markDirty();
        version++;
    }

    public void clearHistory() {
        profile().entries.clear();
        WindFwm.data().profiles.markDirty();
        version++;
    }

    public void removeEntry(Models.MoneyEntry e) {
        profile().entries.remove(e);
        WindFwm.data().profiles.markDirty();
        version++;
    }

    /** Alle 20 Ticks die Sidebar prüfen. */
    public void tick() {
        if (++tickCounter % 20 != 0 || !GameContext.inWorld()) return;
        try {
            ScoreboardReader.checkTitle(GameContext.mc().world);
        } catch (Exception ignored) {
            // Scoreboard kann während Weltwechsel kurz ungültig sein
        }
        if (!active()) return;
        WindConfig c = WindFwm.cfg();
        if (!c.money.readScoreboard) return;
        List<String> lines;
        try {
            lines = ScoreboardReader.sidebarLines(GameContext.mc().world);
        } catch (Exception e) {
            return;
        }
        ScoreboardReader.Reading found = ScoreboardReader.findBalance(lines, c.money.balanceKeywords);
        lastSidebarInfo = lines.isEmpty() ? "Keine Sidebar" : (found == null ? "Sidebar ohne Kontostand-Zeile" : "Kontostand aus Sidebar");
        if (found == null) return;
        Models.ServerProfile p = profile();
        boolean fromSidebar = p.hasBalance && p.balanceSource.startsWith("Sidebar");
        // Die Sidebar rundet (z. B. 21.1M). Solange der genauere Schätzwert zur Anzeige passt, behalten.
        if (fromSidebar && Math.abs(p.balance - found.value()) <= found.step() / 2 + 0.005) return;
        if (fromSidebar && c.money.scoreboardDeltas && System.currentTimeMillis() - lastChatEntryTime > 3000) {
            double delta = found.value() - p.balance;
            if (Math.abs(delta) >= Math.max(0.01, found.step())) {
                p.entries.add(new Models.MoneyEntry(System.currentTimeMillis(), delta, "Sonstiges", "Sidebar", "", false));
            }
        }
        setBalance(found.value(), found.step() > 1 ? "Sidebar (gerundet)" : "Sidebar");
    }
}
