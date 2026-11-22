package de.santa.santanametext;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Modernes Glas-Menue mit runden Ecken: Tabs, Effekte, 40 Schriften, Farbwaehler, Live-Vorschau. */
public class SantaScreen extends Screen {
    private static final int[] SWATCHES = {
            0xFF3B30, 0xFF9500, 0xFFD60A, 0x9BEA2C, 0x30D158, 0x00C7BE,
            0x32D4FF, 0x0A84FF, 0x5E5CE6, 0xBF5AF2, 0xFF2D92, 0xFFFFFF};
    private static final String[] TAB_NAMES = {"Prefix", "Name", "Suffix", "Trenner", "Oben", "Unten"};
    private static final int PER_PAGE = 20;

    private final Screen parent;
    private final SantaConfig c = SantaConfig.get();
    private int tab = 1;
    private boolean editB = false;
    private boolean showFonts = false;
    private int fontPage = 0;
    private int px, py, pw, ph;
    private TextFieldWidget hexField;
    private final List<int[]> fieldBoxes = new ArrayList<>();
    private final List<Object[]> hints = new ArrayList<>();

    public SantaScreen(Screen parent) {
        super(Text.literal("Santa Name Text"));
        this.parent = parent;
    }

    private SantaConfig.TagElement cur() {
        switch (tab) {
            case 0: return c.prefix;
            case 1: return c.name;
            case 2: return c.suffix;
            case 3: return c.sep;
            case 4: return c.top;
            default: return c.bottom;
        }
    }

    private void btn(int x, int y, int w, int h, Supplier<Text> label, Supplier<Boolean> sel, boolean accent, Runnable r) {
        addDrawableChild(new Ui.Btn(x, y, w, h, label, sel, accent, r));
    }

    @Override
    protected void init() {
        fieldBoxes.clear();
        hints.clear();
        pw = Math.min(width - 10, 460);
        ph = Math.min(height - 6, 312);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int inner = pw - 24;
        int colW = (inner - 12) / 2;
        int L = px + 12;
        int R = L + colW + 12;
        SantaConfig.TagElement e = cur();

        // Tabs
        int y = py + 76;
        int tw = (inner - 5 * 3) / 6;
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            btn(L + i * (tw + 3), y, tw, 16, () -> Text.literal(TAB_NAMES[idx]), () -> tab == idx, false, () -> {
                tab = idx;
                clearAndInit();
            });
        }

        // ---- Linke Spalte ----
        y += 22;
        int y0 = y;
        if (tab == 1) {
            btn(L, y, colW, 16, () -> Text.literal("Dein Spielername"), null, false, () -> {});
        } else {
            field(L, y, colW, 16, hintFor(), e.text, 40, s -> e.text = s);
        }
        y += 20;
        int pillW = 48;
        btn(L, y, pillW, 16, () -> Text.literal("Farbe A"), () -> !editB, false, () -> { editB = false; clearAndInit(); });
        btn(L + pillW + 2, y, pillW, 16, () -> Text.literal("Farbe B"), () -> editB, false, () -> { editB = true; clearAndInit(); });
        int hx = L + 2 * pillW + 6;
        hexField = field(hx, y, colW - (2 * pillW + 6), 16, "#Hex", hex(editB ? e.colorB : e.colorA), 7, s -> {
            int old = editB ? e.colorB : e.colorA;
            int nv = parseHex(s, old);
            if (editB) e.colorB = nv; else e.colorA = nv;
        });

        y += 20;
        int stylesY = py + ph - 26 - 6 - 16;
        int swY = stylesY - 14;
        int pickH = Math.max(36, swY - 4 - y);
        addDrawableChild(new Ui.Picker(L, y, colW, pickH, editB ? e.colorB : e.colorA, rgb -> {
            if (editB) e.colorB = rgb; else e.colorA = rgb;
            hexField.setText(hex(rgb));
        }));

        int n = SWATCHES.length;
        int sw = (colW - (n - 1) * 3) / n;
        for (int i = 0; i < n; i++) {
            final int col = SWATCHES[i];
            addDrawableChild(new Ui.Btn(L + i * (sw + 3), swY, sw, 10, () -> Text.empty(), null, false, () -> {
                if (editB) e.colorB = col; else e.colorA = col;
                clearAndInit();
            }) {
                @Override
                protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
                    Ui.round(ctx, getX(), getY(), getWidth(), getHeight(), 3, 0xFF000000 | col,
                            isHovered() ? 0xFFFFFFFF : 0x60000000);
                }
            });
        }

        int stw = (colW - 9) / 4;
        btn(L, stylesY, stw, 16, () -> Text.literal("B").setStyle(Style.EMPTY.withBold(true)), () -> e.bold, false, () -> e.bold = !e.bold);
        btn(L + (stw + 3), stylesY, stw, 16, () -> Text.literal("I").setStyle(Style.EMPTY.withItalic(true)), () -> e.italic, false, () -> e.italic = !e.italic);
        btn(L + 2 * (stw + 3), stylesY, stw, 16, () -> Text.literal("U").setStyle(Style.EMPTY.withUnderline(true)), () -> e.underline, false, () -> e.underline = !e.underline);
        btn(L + 3 * (stw + 3), stylesY, stw, 16, () -> Text.literal("S").setStyle(Style.EMPTY.withStrikethrough(true)), () -> e.strike, false, () -> e.strike = !e.strike);

        // ---- Rechte Spalte: Effekte / Schriften ----
        int ry = y0;
        int half = (colW - 3) / 2;
        btn(R, ry, half, 16, () -> Text.literal("Effekte"), () -> !showFonts, false, () -> { showFonts = false; clearAndInit(); });
        btn(R + half + 3, ry, half, 16, () -> Text.literal("Schriften"), () -> showFonts, false, () -> { showFonts = true; clearAndInit(); });
        ry += 22;
        if (!showFonts) {
            Effect[] ef = Effect.values();
            int cols = 4;
            int bw = (colW - (cols - 1) * 3) / cols;
            for (int i = 0; i < ef.length; i++) {
                final Effect fx = ef[i];
                int gx = R + (i % cols) * (bw + 3);
                int gy = ry + (i / cols) * 21;
                btn(gx, gy, bw, 18, () -> effectLabel(fx, e), () -> fx == e.effect, false, () -> e.effect = fx);
            }
        } else {
            FontStyle[] fonts = FontStyle.values();
            int pages = (fonts.length + PER_PAGE - 1) / PER_PAGE;
            fontPage = Math.min(fontPage, pages - 1);
            int cols = 5;
            int bw = (colW - (cols - 1) * 3) / cols;
            for (int k = 0; k < PER_PAGE; k++) {
                int i = fontPage * PER_PAGE + k;
                if (i >= fonts.length) break;
                final FontStyle fs = fonts[i];
                int gx = R + (k % cols) * (bw + 3);
                int gy = ry + (k / cols) * 21;
                btn(gx, gy, bw, 18, () -> NameBuilder.sample(fs, fs == e.font), () -> fs == e.font, false, () -> e.font = fs);
            }
            int ny = ry + 4 * 21 + 2;
            btn(R, ny, 22, 16, () -> Text.literal("<"), null, false, () -> { fontPage = (fontPage + pages - 1) % pages; clearAndInit(); });
            btn(R + colW - 22, ny, 22, 16, () -> Text.literal(">"), null, false, () -> { fontPage = (fontPage + 1) % pages; clearAndInit(); });
        }

        // ---- Untere Leiste ----
        int by = py + ph - 26;
        int aw = (inner - 3 * 6 - 70) / 3;
        int bx = L;
        toggle(bx, by, aw, "Aktiv", () -> c.enabled, v -> c.enabled = v);
        bx += aw + 6;
        toggle(bx, by, aw, "Bounce", () -> c.bounce, v -> c.bounce = v);
        bx += aw + 6;
        btn(bx, by, aw, 18, () -> Text.literal("Tiers: " + (c.tiersMode == 0 ? "Rechts" : c.tiersMode == 1 ? "Links" : "Aus")),
                null, false, () -> c.tiersMode = (c.tiersMode + 1) % 3);
        bx += aw + 6;
        btn(bx, by, 64, 18, () -> Text.literal("Fertig"), null, true, this::close);
    }

    private String hintFor() {
        switch (tab) {
            case 0: return "Prefix-Text";
            case 2: return "Suffix-Text";
            case 3: return "Trenner (z.B. |)";
            default: return "Zeile";
        }
    }

    /** Effekt-Kachel mit animierter Live-Vorschau. */
    private Text effectLabel(Effect fx, SantaConfig.TagElement e) {
        SantaConfig.TagElement t = new SantaConfig.TagElement(fx.label, FontStyle.NORMAL, fx, e.colorA, e.colorB);
        t.bold = fx == e.effect;
        return NameBuilder.styled(t, fx.label);
    }

    private TextFieldWidget field(int x, int y, int w, int h, String hint, String value, int max, Consumer<String> set) {
        int ty = y + (h - 8) / 2;
        TextFieldWidget f = new TextFieldWidget(textRenderer, x + 6, ty, w - 12, 10, Text.literal(hint));
        f.setDrawsBackground(false);
        f.setMaxLength(max);
        f.setText(value);
        f.setChangedListener(set);
        addDrawableChild(f);
        fieldBoxes.add(new int[]{x, y, w, h});
        hints.add(new Object[]{f, hint, x + 6, ty});
        return f;
    }

    private void toggle(int x, int y, int w, String name, Supplier<Boolean> get, Consumer<Boolean> set) {
        addDrawableChild(new Ui.Btn(x, y, w, 18, () -> Text.literal(name), get, false, () -> set.accept(!get.get())) {
            @Override
            protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
                boolean on = get.get();
                Ui.round(ctx, getX(), getY(), getWidth(), getHeight(), 4, isHovered() ? Ui.HOVER : Ui.OFF, 0x30FFFFFF);
                int tx = getX() + 5, ty = getY() + 5;
                Ui.round(ctx, tx, ty, 16, 8, 4, on ? 0xFF30D158 : 0xFF4A505A, 0x00000000);
                int kx = on ? tx + 9 : tx + 1;
                Ui.round(ctx, kx, ty + 1, 6, 6, 3, 0xFFFFFFFF, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, name, tx + 22, getY() + 5, 0xFFFFFFFF);
            }
        });
    }

    private static String hex(int rgb) { return String.format("%06X", rgb & 0xFFFFFF); }

    private static int parseHex(String s, int fallback) {
        try { return Integer.parseInt(s.replace("#", "").trim(), 16) & 0xFFFFFF; } catch (Exception ex) { return fallback; }
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        // Glas-Panel
        Ui.round(ctx, px - 2, py - 2, pw + 4, ph + 4, 11, 0x30000000, 0x00000000);
        Ui.round(ctx, px, py, pw, ph, 10, 0xB8101722, 0x55FFFFFF);
        ctx.fillGradient(px + 4, py + 2, px + pw - 4, py + 64, 0x28FFFFFF, 0x04FFFFFF);
        Ui.round(ctx, px + 24, py, pw - 48, 2, 1, Ui.ACCENT, Ui.ACCENT);

        // Vorschau (immer sichtbar)
        Ui.round(ctx, px + 12, py + 10, pw - 24, 58, 8, 0x70000000, 0x38FFFFFF);
        ctx.drawTextWithShadow(textRenderer, "SANTA NAME TEXT", px + 20, py + 15, 0xFF9A82FF);
        ctx.drawTextWithShadow(textRenderer, "Vorschau", px + pw - 20 - textRenderer.getWidth("Vorschau"), py + 15, 0xFF6B737D);

        for (int[] b : fieldBoxes) Ui.round(ctx, b[0], b[1], b[2], b[3], 4, 0x88000000, 0x40FFFFFF);

        super.render(ctx, mx, my, delta);

        for (Object[] h : hints) {
            TextFieldWidget f = (TextFieldWidget) h[0];
            if (f.getText().isEmpty() && !f.isFocused())
                ctx.drawTextWithShadow(textRenderer, (String) h[1], (Integer) h[2], (Integer) h[3], 0xFF7A828C);
        }

        // Nametag-Vorschau wie im Spiel
        int cx = px + pw / 2;
        MinecraftClient mc = MinecraftClient.getInstance();
        String name = mc.player != null ? mc.player.getName().getString() : "DeinName";
        int lines = 1 + (c.top.text.isBlank() ? 0 : 1) + (c.bottom.text.isBlank() ? 0 : 1);
        int ly = py + 27 + (38 - lines * 12) / 2;
        if (!c.top.text.isBlank()) {
            tagLine(ctx, NameBuilder.line(c.top), cx, ly);
            ly += 12;
        }
        Text fake = Text.literal(name).append(Text.literal(" HT5").setStyle(Style.EMPTY.withColor(0x32D4FF)));
        tagLine(ctx, NameBuilder.main(name, fake), cx, ly);
        ly += 12;
        if (!c.bottom.text.isBlank()) tagLine(ctx, NameBuilder.line(c.bottom), cx, ly);

        if (showFonts) {
            FontStyle fs = cur().font;
            int colW = (pw - 24 - 12) / 2;
            int R = px + 12 + colW + 12;
            int pages = (FontStyle.values().length + PER_PAGE - 1) / PER_PAGE;
            int ny = py + 76 + 22 + 22 + 4 * 21 + 2;
            ctx.drawCenteredTextWithShadow(textRenderer, "Seite " + (fontPage + 1) + "/" + pages, R + colW / 2, ny + 4, 0xFFB0B8C0);
            ctx.drawTextWithShadow(textRenderer, fs.label, R, ny + 22, 0xFFB0B8C0);
        } else {
            int colW = (pw - 24 - 12) / 2;
            int R = px + 12 + colW + 12;
            int rows = (Effect.values().length + 3) / 4;
            ctx.drawTextWithShadow(textRenderer, "Effekt: " + cur().effect.label, R, py + 76 + 22 + 22 + rows * 21 + 2, 0xFFB0B8C0);
        }
    }

    private void tagLine(DrawContext ctx, Text t, int cx, int y) {
        int w = textRenderer.getWidth(t);
        Ui.round(ctx, cx - w / 2 - 3, y - 2, w + 6, 11, 3, 0x55000000, 0x00000000);
        ctx.drawCenteredTextWithShadow(textRenderer, t, cx, y, 0xFFFFFFFF);
    }

    @Override
    public void removed() { SantaConfig.save(); }

    @Override
    public void close() { client.setScreen(parent); }
}
