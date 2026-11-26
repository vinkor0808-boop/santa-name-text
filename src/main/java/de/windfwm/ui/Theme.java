package de.windfwm.ui;

import de.windfwm.WindFwm;

/** Farbpalette: Anthrazit, Schwarz/Grau, dezentes Eisblau. Alle Farben ARGB. */
public final class Theme {
    public static final int BACKDROP = 0xB80B0C0E;
    public static final int BG = 0xFF16181B;
    public static final int SIDEBAR = 0xFF121416;
    public static final int SURFACE = 0xFF1E2125;
    public static final int SURFACE_2 = 0xFF262A2F;
    public static final int SURFACE_3 = 0xFF2E333A;
    public static final int BORDER = 0xFF30353C;
    public static final int BORDER_SOFT = 0xFF24282D;
    public static final int TEXT = 0xFFE6E9EC;
    public static final int TEXT_DIM = 0xFF9AA2AB;
    public static final int TEXT_FAINT = 0xFF636B74;
    public static final int GREEN = 0xFF6FDC9C;
    public static final int RED = 0xFFFF6B6B;
    public static final int YELLOW = 0xFFFFD36B;

    private Theme() {}

    /** Akzentfarbe (Standard Eisblau 0x7FD3FF), einstellbar. */
    public static int accent() {
        return 0xFF000000 | (WindFwm.cfg().general.accent & 0xFFFFFF);
    }

    public static int accent(int alpha) {
        return (alpha << 24) | (WindFwm.cfg().general.accent & 0xFFFFFF);
    }

    public static int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
    }

    /** Multipliziert den Alphakanal (für Ein-/Ausblendungen). */
    public static int fade(int color, float f) {
        int a = (color >>> 24) & 0xFF;
        return withAlpha(color, Math.round(a * Math.max(0, Math.min(1, f))));
    }

    /** Lineare Mischung zweier Farben. */
    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int a = Math.round(((c1 >>> 24) & 0xFF) * (1 - t) + ((c2 >>> 24) & 0xFF) * t);
        int r = Math.round(((c1 >> 16) & 0xFF) * (1 - t) + ((c2 >> 16) & 0xFF) * t);
        int g = Math.round(((c1 >> 8) & 0xFF) * (1 - t) + ((c2 >> 8) & 0xFF) * t);
        int b = Math.round((c1 & 0xFF) * (1 - t) + (c2 & 0xFF) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int moneyColor(double v) {
        return v >= 0 ? GREEN : RED;
    }

    /** Auswahl an Akzentfarben für die Einstellungen. */
    public static final int[] ACCENTS = {0x7FD3FF, 0x9FB8FF, 0x7FFFE0, 0xB8E2FF, 0xC9A7FF, 0xFFFFFF};
}
