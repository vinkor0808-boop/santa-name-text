package de.windfwm.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * Zeichenfunktionen: abgerundete Flächen, Schatten, Text, Icons.
 * {@link #alpha} wird von Fenstern für Ein-/Ausblend-Animationen gesetzt.
 */
public final class Draw {
    public static float alpha = 1f;
    private static final int[][] INSETS = new int[9][];

    static {
        for (int r = 0; r < INSETS.length; r++) {
            INSETS[r] = new int[r];
            for (int i = 0; i < r; i++) {
                double dy = r - i - 0.5;
                INSETS[r][i] = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            }
        }
    }

    private Draw() {}

    public static TextRenderer font() {
        return MinecraftClient.getInstance().textRenderer;
    }

    private static int c(int color) {
        return alpha >= 0.999f ? color : Theme.fade(color, alpha);
    }

    public static void rect(DrawContext ctx, int x, int y, int w, int h, int color) {
        if (w <= 0 || h <= 0) return;
        ctx.fill(x, y, x + w, y + h, c(color));
    }

    /** Abgerundetes Rechteck; überlappungsfrei, daher auch mit Transparenz sauber. */
    public static void round(DrawContext ctx, int x, int y, int w, int h, int radius, int color) {
        if (w <= 0 || h <= 0) return;
        int r = Math.max(0, Math.min(Math.min(radius, INSETS.length - 1), Math.min(w, h) / 2));
        int col = c(color);
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, col);
            return;
        }
        int[] in = INSETS[r];
        for (int i = 0; i < r; i++) {
            ctx.fill(x + in[i], y + i, x + w - in[i], y + i + 1, col);
            ctx.fill(x + in[i], y + h - i - 1, x + w - in[i], y + h - i, col);
        }
        ctx.fill(x, y + r, x + w, y + h - r, col);
    }

    /** Fläche mit 1px Rand (Füllfarbe sollte deckend sein). */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, int radius, int fill, int border) {
        round(ctx, x, y, w, h, radius, border);
        round(ctx, x + 1, y + 1, w - 2, h - 2, Math.max(0, radius - 1), fill);
    }

    /** Weicher Schatten aus mehreren transparenten Ebenen. */
    public static void shadow(DrawContext ctx, int x, int y, int w, int h, int radius, int strength) {
        for (int i = 4; i >= 1; i--) {
            int a = strength / (i + 1);
            round(ctx, x - i, y - i + 2, w + i * 2, h + i * 2, radius + i, Theme.withAlpha(0x000000, a));
        }
    }

    public static void hLine(DrawContext ctx, int x, int y, int w, int color) {
        rect(ctx, x, y, w, 1, color);
    }

    public static void text(DrawContext ctx, String s, int x, int y, int color) {
        if (s == null || s.isEmpty()) return;
        ctx.drawText(font(), s, x, y, textColor(color), false);
    }

    public static void textShadow(DrawContext ctx, String s, int x, int y, int color) {
        if (s == null || s.isEmpty()) return;
        ctx.drawText(font(), s, x, y, textColor(color), true);
    }

    public static void textCentered(DrawContext ctx, String s, int cx, int y, int color) {
        text(ctx, s, cx - width(s) / 2, y, color);
    }

    public static void textRight(DrawContext ctx, String s, int right, int y, int color) {
        text(ctx, s, right - width(s), y, color);
    }

    /** Text auf maximale Breite kürzen (mit "…"). */
    public static String trim(String s, int maxWidth) {
        if (s == null) return "";
        if (width(s) <= maxWidth) return s;
        String ell = "…";
        int w = maxWidth - width(ell);
        if (w <= 0) return "";
        return font().trimToWidth(s, w) + ell;
    }

    public static void textTrim(DrawContext ctx, String s, int x, int y, int maxWidth, int color) {
        text(ctx, trim(s, maxWidth), x, y, color);
    }

    /** Vergrößerter Text (z. B. Kontostand). */
    public static void textScaled(DrawContext ctx, String s, int x, int y, float scale, int color) {
        Matrix3x2fStack m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        ctx.drawText(font(), s, 0, 0, textColor(color), false);
        m.popMatrix();
    }

    public static int width(String s) {
        return s == null ? 0 : font().getWidth(s);
    }

    public static int lineHeight() {
        return font().fontHeight;
    }

    private static int textColor(int color) {
        int col = c(color);
        // Text mit sehr kleinem Alpha nicht zeichnen lassen, aber auch nicht "voll" werden lassen
        if (((col >>> 24) & 0xFF) < 6) col = Theme.withAlpha(col, 6);
        return col;
    }

    public static void icon(DrawContext ctx, Icons icon, int x, int y, int color) {
        int col = c(color);
        for (int row = 0; row < icon.runs.length; row++) {
            int[] r = icon.runs[row];
            for (int i = 0; i < r.length; i += 2) {
                ctx.fill(x + r[i], y + row, x + r[i] + r[i + 1], y + row + 1, col);
            }
        }
    }

    /** Kleiner gefüllter Kreis (Statuspunkt). */
    public static void dot(DrawContext ctx, int cx, int cy, int color) {
        rect(ctx, cx - 1, cy - 2, 3, 5, color);
        rect(ctx, cx - 2, cy - 1, 5, 3, color);
    }

    /** Fortschrittsbalken 0..1. */
    public static void progress(DrawContext ctx, int x, int y, int w, int h, float t, int color) {
        round(ctx, x, y, w, h, h / 2, Theme.SURFACE_3);
        int fw = Math.round(w * Math.max(0, Math.min(1, t)));
        if (fw > 0) round(ctx, x, y, Math.max(fw, h), h, h / 2, color);
    }
}
