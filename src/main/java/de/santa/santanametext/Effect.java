package de.santa.santanametext;

public enum Effect {
    SOLID("Einfarbig"), RAINBOW("Regenbogen"), GRADIENT("Verlauf"), WAVE("Welle"),
    METALLIC("Metallic"), SHIMMER("Shimmer"), CHROMATIC("Chromatic"), FADE("Fade"),
    GLOWING("Glowing"), ITERATING("Iterating"), NEON("Neon"), BLINK("Blinken"),
    FIRE("Feuer"), ICE("Eis"), SUNSET("Sunset"), OCEAN("Ozean"),
    CANDY("Candy"), MATRIX("Matrix"), PULSE("Pulsieren"), GLITTER("Glitzer");

    public final String label;
    Effect(String label) { this.label = label; }
    public Effect next() { return values()[(ordinal() + 1) % values().length]; }
}
