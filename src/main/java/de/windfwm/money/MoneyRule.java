package de.windfwm.money;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Eine Erkennungsregel für Geldnachrichten. Das Muster ist ein regulärer Ausdruck
 * mit der benannten Gruppe {@code amount}; optional {@code name} (Spieler) und {@code item}.
 * Alle Regeln sind im Money-Tracker editierbar, damit sie an die echten
 * WindSMP-Nachrichten angepasst werden können.
 */
public class MoneyRule {
    public static final String INCOME = "INCOME";
    public static final String EXPENSE = "EXPENSE";
    public static final String BALANCE = "BALANCE";

    public static final String SCOPE_CHAT = "CHAT";
    public static final String SCOPE_ACTIONBAR = "ACTIONBAR";
    public static final String SCOPE_BOTH = "BOTH";

    public String id = "";
    public String name = "";
    public String pattern = "";
    public String kind = INCOME;
    public String category = "Sonstiges";
    public String scope = SCOPE_CHAT;
    public boolean enabled = true;
    public boolean builtin = false;

    private transient Pattern compiled;
    private transient String compiledFrom;
    private transient String error;

    public MoneyRule() {}

    public MoneyRule(String id, String name, String pattern, String kind, String category, String scope, boolean enabled) {
        this.id = id;
        this.name = name;
        this.pattern = pattern;
        this.kind = kind;
        this.category = category;
        this.scope = scope;
        this.enabled = enabled;
        this.builtin = true;
    }

    /** @return kompiliertes Muster oder null, wenn ungültig. */
    public Pattern compiled() {
        if (pattern == null) return null;
        if (!pattern.equals(compiledFrom)) {
            compiledFrom = pattern;
            try {
                compiled = Pattern.compile(pattern);
                error = compiled.pattern().contains("?<amount>") ? null : "Gruppe (?<amount>...) fehlt";
                if (error != null) compiled = null;
            } catch (PatternSyntaxException e) {
                compiled = null;
                error = e.getDescription();
            }
        }
        return compiled;
    }

    public String error() {
        compiled();
        return error;
    }

    public boolean appliesTo(boolean actionbar) {
        if (SCOPE_BOTH.equals(scope)) return true;
        return actionbar ? SCOPE_ACTIONBAR.equals(scope) : SCOPE_CHAT.equals(scope);
    }

    public String kindLabel() {
        return switch (kind) {
            case EXPENSE -> "Ausgabe";
            case BALANCE -> "Kontostand";
            default -> "Einnahme";
        };
    }

    public String scopeLabel() {
        return switch (scope) {
            case SCOPE_ACTIONBAR -> "Actionbar";
            case SCOPE_BOTH -> "Chat + Actionbar";
            default -> "Chat";
        };
    }

    /**
     * Standardregeln. Die Nachrichtenformate stammen aus der Analyse der FWM-Referenz
     * (dort für HugoSMP/DonutSMP/WindSMP hinterlegt) und sind NICHT einzeln auf WindSMP
     * verifiziert. Sie lassen sich im Spiel prüfen, abschalten und anpassen.
     */
    public static List<MoneyRule> defaults() {
        String amt = "\\$ ?(?<amount>[0-9][0-9.,]*\\s?[KMBTkmbt]?)";
        String player = "(?<name>[A-Za-z0-9_.*]{2,17}?)";
        List<MoneyRule> list = new ArrayList<>();
        // --- auf WindSMP bestätigt (Screenshots vom 26.11.2026) ---
        list.add(new MoneyRule("wind_order_payment", "WindSMP: Order payment",
                "^Order payment: \\+" + amt, INCOME, "Auftrag", SCOPE_BOTH, true));
        list.add(new MoneyRule("wind_bought", "WindSMP: Bought … for $",
                "^Bought (?<item>.+?) for " + amt + "\\.?$", EXPENSE, "Kauf", SCOPE_BOTH, true));
        list.add(new MoneyRule("wind_sell_plus", "WindSMP: +$ (Verkauf)",
                "^\\+" + amt + "$", INCOME, "Verkauf", SCOPE_BOTH, true));
        // Vor dem Namen kann ein Spielerkopf-Symbol stehen -> beliebige Zeichen davor erlauben
        String headName = ".*?(?<name>[A-Za-z0-9_]{2,16})";
        list.add(new MoneyRule("wind_pay_in", "WindSMP: Zahlung erhalten",
                "^You received " + amt + " from " + headName + "\\.?$", INCOME, "Zahlung", SCOPE_CHAT, true));
        list.add(new MoneyRule("wind_pay_out", "WindSMP: Zahlung gesendet",
                "^You paid " + headName + " " + amt + "\\.?$", EXPENSE, "Zahlung", SCOPE_CHAT, true));
        // --- aus der FWM-Referenz, auf WindSMP noch nicht bestätigt ---
        list.add(new MoneyRule("pay_in_alt", "Zahlung erhalten (alt.)",
                "^" + player + " paid you " + amt + "\\.?$", INCOME, "Zahlung", SCOPE_CHAT, true));
        list.add(new MoneyRule("sell_container", "Verkauf (Container)",
                "^Sold container items for " + amt + "\\.?$", INCOME, "Verkauf", SCOPE_CHAT, true));
        list.add(new MoneyRule("buy", "Kauf",
                "^You bought (?<item>.+?) for " + amt + "\\.?$", EXPENSE, "Kauf", SCOPE_CHAT, true));
        list.add(new MoneyRule("deliver", "Auftrag geliefert",
                "^You delivered (?<item>.+?) and received " + amt + "\\.?$", INCOME, "Auftrag", SCOPE_CHAT, true));
        list.add(new MoneyRule("sell_bonus", "Verkauf mit Bonus (Actionbar)",
                "\\+\\$(?<amount>[0-9][0-9.,]*)\\s*\\(Basis:", INCOME, "Verkauf", SCOPE_ACTIONBAR, false));
        list.add(new MoneyRule("balance", "Kontostand-Anzeige",
                "(?i)^(?:your )?(?:balance|kontostand|guthaben)(?: is)?\\s*:?\\s*" + amt, BALANCE, "Kontostand", SCOPE_CHAT, true));
        return list;
    }
}
