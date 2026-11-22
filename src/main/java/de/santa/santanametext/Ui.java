package de.santa.santanametext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Flache, moderne Widgets. */
final class Ui {
    private Ui() {}

    static final int ACCENT = 0xFF7C5CFF;
    static final int ACCENT_DIM = 0xB05B43D6;
    static final int OFF = 0x28FFFFFF;
    static final int HOVER = 0x4CFFFFFF;

    private static final java.util.Map<Integer, float[][][]> CORNERS = new java.util.HashMap<>();

    /** [0]=Abdeckung aussen, [1]=Abdeckung innen (Radius-1), je r x r Pixel der Ecke oben links. */
    private static float[][][] corner(int r) {
        return CORNERS.computeIfAbsent(r, rr -> {
            float[][][] t = new float[2][rr][rr];
            int S = 4;
            for (int py = 0; py < rr; py++) {
                for (int px = 0; px < rr; px++) {
                    int o = 0, in = 0;
                    for (int sy = 0; sy < S; sy++) {
                        for (int sx = 0; sx < S; sx++) {
                            double dx = rr - (px + (sx + 0.5) / S);
                            double dy = rr - (py + (sy + 0.5) / S);
                            double d = Math.sqrt(dx * dx + dy * dy);
                            if (d <= rr) o++;
                            if (d <= rr - 1) in++;
                        }
                    }
                    t[0][py][px] = o / (float) (S * S);
                    t[1][py][px] = in / (float) (S * S);
                }
            }
            return t;
        });
    }

    private static int alpha(int argb, float f) {
        int a = Math.round(((argb >>> 24) & 255) * f);
        return a <= 0 ? 0 : (a << 24) | (argb & 0xFFFFFF);
    }

    private static void px(DrawContext ctx, int x, int y, int argb) {
        if ((argb >>> 24) != 0) ctx.fill(x, y, x + 1, y + 1, argb);
    }

    /** Abgerundetes, kantengeglättetes Rechteck mit Fuellung und 1px Rand. */
    static void round(DrawContext ctx, int x, int y, int w, int h, int r, int fill, int border) {
        r = Math.max(2, Math.min(r, Math.min(w, h) / 2));
        boolean hasB = (border >>> 24) != 0;
        int bw = hasB ? 1 : 0;
        // Mitte + Seiten
        ctx.fill(x + r, y + bw, x + w - r, y + h - bw, fill);
        ctx.fill(x + bw, y + r, x + r, y + h - r, fill);
        ctx.fill(x + w - r, y + r, x + w - bw, y + h - r, fill);
        if (hasB) {
            ctx.fill(x + r, y, x + w - r, y + 1, border);
            ctx.fill(x + r, y + h - 1, x + w - r, y + h, border);
            ctx.fill(x, y + r, x + 1, y + h - r, border);
            ctx.fill(x + w - 1, y + r, x + w, y + h - r, border);
        }
        float[][][] c = corner(r);
        for (int j = 0; j < r; j++) {
            for (int i = 0; i < r; i++) {
                float o = c[0][j][i], in = hasB ? c[1][j][i] : c[0][j][i];
                int bc = hasB ? alpha(border, Math.max(0, o - in)) : 0;
                int fc = alpha(fill, in);
                int[] xs = {x + i, x + w - 1 - i};
                int[] ys = {y + j, y + h - 1 - j};
                for (int xx : xs) {
                    for (int yy : ys) {
                        px(ctx, xx, yy, bc);
                        px(ctx, xx, yy, fc);
                    }
                }
            }
        }
    }

    static void box(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        ctx.fill(x, y, x + w, y + h, fill);
        ctx.fill(x, y, x + w, y + 1, border);
        ctx.fill(x, y + h - 1, x + w, y + h, border);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);
    }

    static class Btn extends ClickableWidget {
        private final Supplier<Text> label;
        private final Supplier<Boolean> selected;
        private final Runnable action;
        private final boolean accent;

        Btn(int x, int y, int w, int h, Supplier<Text> label, Supplier<Boolean> selected, boolean accent, Runnable action) {
            super(x, y, w, h, Text.empty());
            this.label = label;
            this.selected = selected;
            this.accent = accent;
            this.action = action;
        }

        @Override
        public void onClick(Click click, boolean doubled) { action.run(); }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            boolean sel = selected != null && selected.get();
            boolean hov = isHovered();
            int bg = accent ? (hov ? 0xFF9A82FF : ACCENT) : sel ? (hov ? 0xE0765CF0 : ACCENT_DIM) : hov ? HOVER : OFF;
            int border = sel || accent ? 0xFFC9BDFF : hov ? 0x80FFFFFF : 0x28FFFFFF;
            round(ctx, getX(), getY(), getWidth(), getHeight(), 4, bg, border);
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.drawCenteredTextWithShadow(tr, label.get(), getX() + getWidth() / 2,
                    getY() + (getHeight() - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    /** Farbwaehler: Saettigung/Helligkeit-Feld + Farbton-Leiste. */
    static final class Picker extends ClickableWidget {
        private float h, s, v;
        private boolean dragHue;
        private final Consumer<Integer> onPick;
        private static final int BAR = 10;

        Picker(int x, int y, int w, int hgt, int rgb, Consumer<Integer> onPick) {
            super(x, y, w, hgt, Text.empty());
            this.onPick = onPick;
            float[] hsv = toHsv(rgb);
            h = hsv[0]; s = hsv[1]; v = hsv[2];
        }

        private int fieldW() { return getWidth() - BAR - 4; }

        @Override
        public void onClick(Click click, boolean doubled) {
            dragHue = click.x() >= getX() + fieldW() + 2;
            apply(click.x(), click.y());
        }

        @Override
        protected void onDrag(Click click, double dx, double dy) { apply(click.x(), click.y()); }

        private void apply(double mx, double my) {
            if (dragHue) {
                h = clamp((float) ((my - getY()) / (getHeight() - 1)));
            } else {
                s = clamp((float) ((mx - getX()) / (fieldW() - 1)));
                v = 1f - clamp((float) ((my - getY()) / (getHeight() - 1)));
            }
            onPick.accept(hsvToRgb(h, s, v));
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mx, int my, float delta) {
            int x = getX(), y = getY(), fw = fieldW(), hh = getHeight();
            for (int i = 0; i < fw; i += 2) {
                int top = 0xFF000000 | hsvToRgb(h, i / (float) fw, 1f);
                ctx.fillGradient(x + i, y, Math.min(x + i + 2, x + fw), y + hh, top, 0xFF000000);
            }
            int bx = x + fw + 4;
            for (int j = 0; j < hh; j += 2) {
                int col = 0xFF000000 | hsvToRgb(j / (float) hh, 1f, 1f);
                ctx.fill(bx, y + j, bx + BAR, Math.min(y + j + 2, y + hh), col);
            }
            // Marker
            int cx = x + Math.round(s * (fw - 1));
            int cy = y + Math.round((1f - v) * (hh - 1));
            ctx.drawStrokedRectangle(cx - 2, cy - 2, 5, 5, 0xFFFFFFFF);
            ctx.drawStrokedRectangle(cx - 3, cy - 3, 7, 7, 0xFF000000);
            int my2 = y + Math.round(h * (hh - 1));
            ctx.fill(bx - 2, my2 - 1, bx + BAR + 2, my2 + 1, 0xFFFFFFFF);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {}
    }

    static float clamp(float f) { return f < 0 ? 0 : Math.min(f, 1f); }

    static int hsvToRgb(float h, float s, float v) {
        return net.minecraft.util.math.MathHelper.hsvToRgb(h >= 1f ? 0.9999f : h, s, v) & 0xFFFFFF;
    }

    static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = (((g - b) / d) % 6) / 6f;
        else if (max == g) h = (((b - r) / d) + 2) / 6f;
        else h = (((r - g) / d) + 4) / 6f;
        if (h < 0) h += 1f;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
