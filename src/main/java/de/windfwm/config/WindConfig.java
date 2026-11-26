package de.windfwm.config;

import de.windfwm.money.MoneyRule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Alle Einstellungen der Mod. Wird als config/windfwm/config.json gespeichert. */
public class WindConfig {
    public int version = 2;
    /** 2 = WindSMP-Regeln ergänzt */
    public General general = new General();
    public Money money = new Money();
    public Stats stats = new Stats();
    public Chat chat = new Chat();
    public Waypoints waypoints = new Waypoints();
    public Hud hud = new Hud();
    public Map<String, WindowState> windows = new HashMap<>();

    public static class General {
        public boolean animations = true;
        /** Geld-Erkennung und Chat-Anpassungen nur auf WindSMP aktiv (keine Server-Sperre für den Rest). */
        public boolean windSmpOnly = false;
        public int accent = 0x7FD3FF;
        public boolean showWelcome = true;
    }

    public static class Money {
        public boolean readScoreboard = true;
        public boolean readActionbar = true;
        /** Unerklärte Änderungen des Scoreboard-Kontostands als Bewegung erfassen. */
        public boolean scoreboardDeltas = false;
        public int maxHistory = 5000;
        public List<String> balanceKeywords = new ArrayList<>(List.of(
                "money", "balance", "kontostand", "guthaben", "geld", "bank", "coins"));
        public List<MoneyRule> rules = MoneyRule.defaults();
    }

    public static class Stats {
        public int rateWindowMinutes = 60;
        /** Einnahmen pro Stunde als Netto (Einnahmen minus Ausgaben). */
        public boolean netRate = false;
        public double dailyGoal = 0;
    }

    public static class Chat {
        public boolean enabled = true;
        public boolean timestamps = false;
        public boolean highlightOwnName = true;
        public boolean markMoney = true;
        public boolean logPlayerChat = true;
        public int logSize = 500;
        public List<String> hidden = new ArrayList<>();
        public List<String> highlight = new ArrayList<>();
        /** Erkennung privater Nachrichten (allgemeine Formulierungen, editierbar). */
        public String privatePattern = "(?i)(whispers to you|you whisper to|flüstert dir|-> ?(me|mir)\\b|\\b(me|ich) ->)";
    }

    public static class Waypoints {
        public boolean currentServerOnly = true;
        public boolean currentDimensionOnly = false;
        public String trackedId = null;
    }

    public static class Hud {
        public boolean enabled = true;
        /** 0 = oben links, 1 = oben rechts, 2 = unten links, 3 = unten rechts */
        public int corner = 0;
        public boolean showBalance = true;
        public boolean showRate = true;
        public boolean showWaypoint = true;
        public boolean showSession = false;
    }

    public static class WindowState {
        public int x = -1, y = -1, w = 0, h = 0;
        public boolean open = false;
    }
}
