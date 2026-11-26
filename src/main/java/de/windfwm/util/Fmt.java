package de.windfwm.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/** Formatierung von Geld, Zeit und Strecken. */
public final class Fmt {
    private static final Pattern FORMAT_CODES = Pattern.compile("§.");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_SEC = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM. HH:mm");

    private Fmt() {}

    /** Kompakte Darstellung wie auf dem Server: $1.25M, $830K, $12.50 */
    public static String money(double v) {
        String sign = v < 0 ? "-" : "";
        double a = Math.abs(v);
        String body;
        if (a >= 1e12) body = trim(a / 1e12) + "T";
        else if (a >= 1e9) body = trim(a / 1e9) + "B";
        else if (a >= 1e6) body = trim(a / 1e6) + "M";
        else if (a >= 1e4) body = trim(a / 1e3) + "K";
        else body = String.format(Locale.US, "%,.2f", a);
        return sign + "$" + body;
    }

    /** Vorzeichenbehaftet: +$1.2K / -$300.00 */
    public static String signed(double v) {
        return (v >= 0 ? "+" : "-") + money(Math.abs(v));
    }

    public static String moneyFull(double v) {
        return (v < 0 ? "-$" : "$") + String.format(Locale.US, "%,.2f", Math.abs(v));
    }

    private static String trim(double v) {
        String s = String.format(Locale.US, "%.2f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "");
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static String time(long epochMs) {
        return TIME.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    public static String timeSec(long epochMs) {
        return TIME_SEC.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    public static String dateTime(long epochMs) {
        LocalDate day = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()).toLocalDate();
        if (day.equals(LocalDate.now())) return "Heute " + time(epochMs);
        return DATE_TIME.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    public static String duration(long ms) {
        long totalMin = Math.max(0, ms) / 60000L;
        long h = totalMin / 60, m = totalMin % 60;
        if (h >= 24) return (h / 24) + "d " + (h % 24) + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + "m";
    }

    public static String distance(double blocks) {
        if (blocks >= 1000) return String.format(Locale.GERMANY, "%.1f km", blocks / 1000.0);
        return String.format(Locale.GERMANY, "%.0f m", blocks);
    }

    public static String number(long n) {
        return String.format(Locale.GERMANY, "%,d", n);
    }

    /** Entfernt Legacy-Farbcodes (§a usw.) und überflüssige Leerzeichen. */
    public static String strip(String s) {
        if (s == null) return "";
        return FORMAT_CODES.matcher(s).replaceAll("").trim();
    }
}
