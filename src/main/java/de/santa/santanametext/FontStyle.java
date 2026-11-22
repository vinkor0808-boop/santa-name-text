package de.santa.santanametext;

import java.util.function.UnaryOperator;

/** 40 Schrift-Stile. fontId != null: Minecraft-eigene Schriftart. */
public enum FontStyle {
    NORMAL("Normal", s -> s, null),
    BOLD("Fett", Glyphs::bold, null),
    ITALIC("Kursiv", Glyphs::italic, null),
    BOLD_ITALIC("Fett Kursiv", Glyphs::boldItalic, null),
    SCRIPT("Schreibschrift", Glyphs::script, null),
    BOLD_SCRIPT("Schreibschrift fett", Glyphs::boldScript, null),
    FRAKTUR("Fraktur", Glyphs::fraktur, null),
    BOLD_FRAKTUR("Fraktur fett", Glyphs::boldFraktur, null),
    DOUBLE("Doppelstrich", Glyphs::doubleStruck, null),
    SANS("Sans", Glyphs::sans, null),
    SANS_BOLD("Sans fett", Glyphs::sansBold, null),
    SANS_ITALIC("Sans kursiv", Glyphs::sansItalic, null),
    SANS_BOLD_ITALIC("Sans fett kursiv", Glyphs::sansBoldItalic, null),
    MONO("Monospace", Glyphs::mono, null),
    FULLWIDTH("Breit", Glyphs::fullwidth, null),
    SMALL_CAPS("Kapitaelchen", Glyphs::smallCaps, null),
    CIRCLED("Kreise", Glyphs::circled, null),
    CIRCLED_NEG("Kreise voll", s -> Glyphs.letters(s, 0x1F150), null),
    SQUARED("Quadrate", s -> Glyphs.letters(s, 0x1F130), null),
    SQUARED_NEG("Quadrate voll", s -> Glyphs.letters(s, 0x1F170), null),
    SUPER("Hochgestellt", Glyphs::superscript, null),
    UPSIDE("Kopfstand", Glyphs::upsideDown, null),
    SPACED("Gesperrt", s -> Glyphs.join(s, " "), null),
    DOTTED("Punktiert", s -> Glyphs.join(s, "·"), null),
    BOLD_SPACED("Fett gesperrt", s -> Glyphs.join(Glyphs.bold(s), " "), null),
    SCRIPT_SPACED("Schreibschrift gesperrt", s -> Glyphs.join(Glyphs.script(s), " "), null),
    SMALL_CAPS_SPACED("Kapitaelchen gesperrt", s -> Glyphs.join(Glyphs.smallCaps(s), " "), null),
    GALACTIC("Galaktisch", s -> s, "alt"),
    ILLAGER("Illager", s -> s, "illageralt"),
    UNIFORM("Uniform", s -> s, "uniform"),
    STARS("Sterne", s -> "\u2605 " + s + " \u2605", null),
    SPARKLE("Funkeln", s -> "\u2726 " + Glyphs.script(s) + " \u2726", null),
    CROWN("Krone", s -> "\u265B " + Glyphs.bold(s) + " \u265B", null),
    HEARTS("Herzen", s -> "\u2665 " + Glyphs.script(s) + " \u2665", null),
    ARROWS("Pfeile", s -> "\u00BB " + Glyphs.smallCaps(s) + " \u00AB", null),
    DIAMOND("Diamant", s -> "\u2756 " + Glyphs.boldFraktur(s) + " \u2756", null),
    MOON("Mond", s -> "\u263E " + Glyphs.sansBold(s) + " \u263D", null),
    BLOCK_SPACED("Sans fett gesperrt", s -> Glyphs.join(Glyphs.sansBold(s), " "), null),
    DOUBLE_SPACED("Doppelstrich gesperrt", s -> Glyphs.join(Glyphs.doubleStruck(s), " "), null),
    FRAKTUR_SPACED("Fraktur gesperrt", s -> Glyphs.join(Glyphs.boldFraktur(s), " "), null);

    public final String label;
    public final String fontId;
    private final UnaryOperator<String> fn;

    FontStyle(String label, UnaryOperator<String> fn, String fontId) {
        this.label = label;
        this.fn = fn;
        this.fontId = fontId;
    }

    public String apply(String s) { return fn.apply(s); }
}
