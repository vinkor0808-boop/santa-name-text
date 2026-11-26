package de.windfwm.money;

import de.windfwm.util.AmountParser;
import de.windfwm.util.Fmt;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Liest die für alle sichtbare Sidebar und sucht eine Kontostand-Zeile. */
public final class ScoreboardReader {
    private static final Pattern AMOUNT = Pattern.compile("([0-9][0-9.,]*)\\s?([kKmMbBtT])?(?![a-zA-Z])");

    /** true, wenn der Sidebar-Titel "WindSMP" enthält (zusätzliche Server-Erkennung). */
    public static volatile boolean windTitle;

    /** Kontostand aus der Sidebar; step = Rundung der Anzeige (z. B. 21.1M -> 100000). */
    public record Reading(double value, double step) {}

    private ScoreboardReader() {}

    /** Prüft den Sidebar-Titel auf "WindSMP". */
    public static void checkTitle(ClientWorld world) {
        boolean found = false;
        if (world != null) {
            ScoreboardObjective objective = world.getScoreboard().getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
            if (objective != null) {
                found = Fmt.strip(objective.getDisplayName().getString()).toLowerCase(Locale.ROOT).contains("windsmp");
            }
        }
        windTitle = found;
    }

    public static List<String> sidebarLines(ClientWorld world) {
        List<String> lines = new ArrayList<>();
        if (world == null) return lines;
        Scoreboard sb = world.getScoreboard();
        ScoreboardObjective objective = sb.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) return lines;
        for (ScoreboardEntry entry : sb.getScoreboardEntries(objective)) {
            if (entry.hidden()) continue;
            Team team = sb.getScoreHolderTeam(entry.owner());
            Text line = Team.decorateName(team, entry.name());
            lines.add(Fmt.strip(line.getString()));
        }
        return lines;
    }

    /** @return erkannter Kontostand oder null */
    public static Reading findBalance(List<String> lines, List<String> keywords) {
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            for (String kw : keywords) {
                if (kw == null || kw.isBlank()) continue;
                int idx = lower.indexOf(kw.toLowerCase(Locale.ROOT));
                if (idx < 0) continue;
                Matcher m = AMOUNT.matcher(line);
                if (m.find(idx)) {
                    double v = AmountParser.parse(m.group(1), m.group(2));
                    if (!Double.isNaN(v)) return new Reading(v, step(m.group(1), m.group(2)));
                }
            }
        }
        return null;
    }

    /** Anzeigegenauigkeit: "21.1M" -> 0.1 * 1e6, "21M" -> 1e6, "1,234.56" -> 0.01. */
    private static double step(String num, String suffix) {
        double mult = 1;
        if (suffix != null && !suffix.isEmpty()) {
            switch (Character.toLowerCase(suffix.charAt(0))) {
                case 'k' -> mult = 1e3;
                case 'm' -> mult = 1e6;
                case 'b' -> mult = 1e9;
                case 't' -> mult = 1e12;
                default -> { }
            }
            int dot = Math.max(num.lastIndexOf('.'), num.lastIndexOf(','));
            int decimals = dot < 0 ? 0 : num.length() - dot - 1;
            return Math.pow(10, -decimals) * mult;
        }
        return 1;
    }
}
