package de.windfwm.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wandelt Geldbeträge aus Servernachrichten in Zahlen um.
 * Unterstützt "1,234.56", "1.234,56", "1.5K", "2M", "3,2B", "4T".
 */
public final class AmountParser {
    private static final Pattern NUMBER = Pattern.compile("([0-9][0-9.,]*)\\s*([kKmMbBtT])?");

    private AmountParser() {}

    /** @return Betrag oder {@link Double#NaN}, wenn nichts erkannt wurde. */
    public static double parse(String raw) {
        if (raw == null) return Double.NaN;
        Matcher m = NUMBER.matcher(raw.trim());
        if (!m.find()) return Double.NaN;
        return parse(m.group(1), m.group(2));
    }

    public static double parse(String num, String suffix) {
        if (num == null || num.isEmpty()) return Double.NaN;
        // abschließende Trennzeichen entfernen ("1,000." am Satzende)
        while (!num.isEmpty() && (num.endsWith(".") || num.endsWith(","))) {
            num = num.substring(0, num.length() - 1);
        }
        if (num.isEmpty()) return Double.NaN;
        boolean hasSuffix = suffix != null && !suffix.isEmpty();
        int lastDot = num.lastIndexOf('.');
        int lastComma = num.lastIndexOf(',');
        String normalized;
        if (lastDot >= 0 && lastComma >= 0) {
            // Das zuletzt vorkommende Zeichen ist das Dezimaltrennzeichen.
            if (lastDot > lastComma) normalized = num.replace(",", "");
            else normalized = num.replace(".", "").replace(',', '.');
        } else if (lastComma >= 0) {
            if (count(num, ',') > 1) normalized = num.replace(",", "");
            else if (!hasSuffix && isThousands(num, ',')) normalized = num.replace(",", "");
            else normalized = num.replace(',', '.');
        } else if (lastDot >= 0) {
            if (count(num, '.') > 1) normalized = num.replace(".", "");
            else if (!hasSuffix && isThousands(num, '.')) normalized = num.replace(".", "");
            else normalized = num;
        } else {
            normalized = num;
        }
        double value;
        try {
            value = Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
        if (hasSuffix) {
            switch (Character.toLowerCase(suffix.charAt(0))) {
                case 'k' -> value *= 1e3;
                case 'm' -> value *= 1e6;
                case 'b' -> value *= 1e9;
                case 't' -> value *= 1e12;
                default -> { }
            }
        }
        return value;
    }

    /** true, wenn nach dem (einzigen/letzten) Trennzeichen genau drei Ziffern folgen. */
    private static boolean isThousands(String num, char sep) {
        int idx = num.lastIndexOf(sep);
        return idx >= 0 && num.length() - idx - 1 == 3;
    }

    private static int count(String s, char c) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
