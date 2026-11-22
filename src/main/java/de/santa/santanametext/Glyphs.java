package de.santa.santanametext;

import java.util.Map;
import java.util.function.IntUnaryOperator;

/** Unicode-Umwandlungen: aus normalem Text wird z.B. fette, kursive oder verschnoerkelte Schrift. */
final class Glyphs {
    private Glyphs() {}

    static final Map<Character, Integer> ITALIC_EX = Map.of('h', 0x210E);
    static final Map<Character, Integer> SCRIPT_EX = Map.ofEntries(
            Map.entry('B', 0x212C), Map.entry('E', 0x2130), Map.entry('F', 0x2131), Map.entry('H', 0x210B),
            Map.entry('I', 0x2110), Map.entry('L', 0x2112), Map.entry('M', 0x2133), Map.entry('R', 0x211B),
            Map.entry('e', 0x212F), Map.entry('g', 0x210A), Map.entry('o', 0x2134));
    static final Map<Character, Integer> FRAKTUR_EX = Map.of(
            'C', 0x212D, 'H', 0x210C, 'I', 0x2111, 'R', 0x211C, 'Z', 0x2128);
    static final Map<Character, Integer> DOUBLE_EX = Map.of(
            'C', 0x2102, 'H', 0x210D, 'N', 0x2115, 'P', 0x2119, 'Q', 0x211A, 'R', 0x211D, 'Z', 0x2124);

    static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    static final String SUPER = "ᵃᵇᶜᵈᵉᶠᵍʰⁱʲᵏˡᵐⁿᵒᵖqʳˢᵗᵘᵛʷˣʸᶻ";
    static final String UPSIDE = "ɐqɔpǝɟƃɥᴉɾʞlɯuodbɹsʇnʌʍxʎz";

    static String map(String s, IntUnaryOperator f) {
        StringBuilder sb = new StringBuilder();
        s.codePoints().forEach(cp -> sb.appendCodePoint(f.applyAsInt(cp)));
        return sb.toString();
    }

    static int letter(int cp) {
        if (cp >= 'A' && cp <= 'Z') return cp - 'A';
        if (cp >= 'a' && cp <= 'z') return cp - 'a';
        return -1;
    }

    /** Mathematische Alphabete (fett, kursiv, Schreibschrift ...). digit < 0: Ziffern bleiben. */
    static String math(String s, int upper, int lower, int digit, Map<Character, Integer> ex) {
        return map(s, cp -> {
            if (ex != null && cp < 128) {
                Integer e = ex.get((char) cp);
                if (e != null) return e;
            }
            if (cp >= 'A' && cp <= 'Z') return upper + (cp - 'A');
            if (cp >= 'a' && cp <= 'z') return lower + (cp - 'a');
            if (digit >= 0 && cp >= '0' && cp <= '9') return digit + (cp - '0');
            return cp;
        });
    }

    static String bold(String s)         { return math(s, 0x1D400, 0x1D41A, 0x1D7CE, null); }
    static String italic(String s)       { return math(s, 0x1D434, 0x1D44E, -1, ITALIC_EX); }
    static String boldItalic(String s)   { return math(s, 0x1D468, 0x1D482, -1, null); }
    static String script(String s)       { return math(s, 0x1D49C, 0x1D4B6, -1, SCRIPT_EX); }
    static String boldScript(String s)   { return math(s, 0x1D4D0, 0x1D4EA, -1, null); }
    static String fraktur(String s)      { return math(s, 0x1D504, 0x1D51E, -1, FRAKTUR_EX); }
    static String boldFraktur(String s)  { return math(s, 0x1D56C, 0x1D586, -1, null); }
    static String doubleStruck(String s) { return math(s, 0x1D538, 0x1D552, 0x1D7D8, DOUBLE_EX); }
    static String sans(String s)         { return math(s, 0x1D5A0, 0x1D5BA, 0x1D7E2, null); }
    static String sansBold(String s)     { return math(s, 0x1D5D4, 0x1D5EE, 0x1D7EC, null); }
    static String sansItalic(String s)   { return math(s, 0x1D608, 0x1D622, -1, null); }
    static String sansBoldItalic(String s) { return math(s, 0x1D63C, 0x1D656, -1, null); }
    static String mono(String s)         { return math(s, 0x1D670, 0x1D68A, 0x1D7F6, null); }
    static String fullwidth(String s)    { return math(s, 0xFF21, 0xFF41, 0xFF10, null); }

    static String smallCaps(String s) {
        return map(s, cp -> { int l = letter(cp); return l >= 0 ? SMALL_CAPS.charAt(l) : cp; });
    }

    static String superscript(String s) {
        return map(s, cp -> {
            int l = letter(cp);
            if (l >= 0) return SUPER.charAt(l);
            if (cp >= '0' && cp <= '9') return "⁰¹²³⁴⁵⁶⁷⁸⁹".charAt(cp - '0');
            return cp;
        });
    }

    static String upsideDown(String s) {
        int[] cps = s.codePoints().toArray();
        StringBuilder sb = new StringBuilder();
        for (int i = cps.length - 1; i >= 0; i--) {
            int l = letter(cps[i]);
            sb.appendCodePoint(l >= 0 ? UPSIDE.charAt(l) : cps[i]);
        }
        return sb.toString();
    }

    static String circled(String s) {
        return map(s, cp -> {
            if (cp >= 'A' && cp <= 'Z') return 0x24B6 + (cp - 'A');
            if (cp >= 'a' && cp <= 'z') return 0x24D0 + (cp - 'a');
            if (cp == '0') return 0x24EA;
            if (cp >= '1' && cp <= '9') return 0x2460 + (cp - '1');
            return cp;
        });
    }

    /** Nur Buchstaben, Gross- und Kleinbuchstaben werden gleich behandelt. */
    static String letters(String s, int base) {
        return map(s, cp -> { int l = letter(cp); return l >= 0 ? base + l : cp; });
    }

    static String join(String s, String sep) {
        int[] cps = s.codePoints().toArray();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cps.length; i++) {
            if (i > 0) sb.append(sep);
            sb.appendCodePoint(cps[i]);
        }
        return sb.toString();
    }
}
