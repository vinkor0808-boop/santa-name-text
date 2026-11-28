package de.santa.oresim;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Glas-Menue fuer Santa OreSim mit Tabs "Allgemein" und "Farben" (auch ueber Mod Menu erreichbar). */
public class OreScreen extends Screen {
    private static final int[] RADII = {16, 32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256};
    private static final int[] SWATCHES = {
            0x33E8FF, 0x0A84FF, 0x5E5CE6, 0xBF5AF2, 0xFF2D92, 0xFF3B30,
            0xFF7A1F, 0xFFD60A, 0x9BEA2C, 0x30D158, 0x00C7BE, 0xFFFFFF};

    private static int tab = 0;          // 0 = Allgemein, 1 = Farben
    private static boolean editDebris = false;

    private final Screen parent;
    private final OreConfig c = OreConfig.INSTANCE;
    private int px, py, pw, ph, L, inner, y0;

    // Allgemein
    private TextFieldWidget seedField;
    private String seedText;
    private String seedStatus = "";
    private int seedStatusColor = 0xFF7A828C;

    // Farben
    private TextFieldWidget hexField;
    private int[] hexBox, previewBox;
    private int sliderY;

    public OreScreen(Screen parent) {
        super(Text.literal("Santa OreSim"));
        this.parent = parent;
        this.seedText = c.hasSeed ? Long.toString(c.seed) : "";
    }

    @Override
    protected void init() {
        pw = Math.min(width - 10, 330);
        ph = Math.min(height - 6, 290);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        L = px + 14;
        inner = pw - 28;

        // Tabs
        int tw = (inner - 6) / 2;
        btn(L, py + 30, tw, 16, () -> Text.literal("Allgemein"), () -> tab == 0, false, () -> { tab = 0; clearAndInit(); });
        btn(L + tw + 6, py + 30, tw, 16, () -> Text.literal("Farben"), () -> tab == 1, false, () -> { tab = 1; clearAndInit(); });
        y0 = py + 56;

        if (tab == 0) initGeneral(); else initColors();

        // Aktionen
        int by = py + ph - 26;
        int bw = (inner - 6) / 2;
        btn(L, by, bw, 18, () -> Text.literal("Neu berechnen"), null, false, () -> {
            OreTracker.reset();
            seedStatus = "Wird neu berechnet ...";
            seedStatusColor = 0xFFB0B8C0;
        });
        btn(L + bw + 6, by, bw, 18, () -> Text.literal("Fertig"), null, true, this::close);
    }

    // ------------------------------------------------------------ Allgemein

    private void initGeneral() {
        int y = y0;
        toggle(L, y, inner, "ESP aktiv", () -> c.enabled, v -> c.enabled = v);
        y += 24;
        int half = (inner - 6) / 2;
        toggle(L, y, half, "Diamanten", () -> c.diamonds, v -> { c.diamonds = v; OreTracker.reset(); });
        toggle(L + half + 6, y, half, "Ancient Debris", () -> c.debris, v -> { c.debris = v; OreTracker.reset(); });

        y += 46;
        int applyW = 70;
        int fw = inner - applyW - 6;
        seedField = new TextFieldWidget(textRenderer, L + 6, y + 5, fw - 12, 10, Text.literal("Seed"));
        seedField.setDrawsBackground(false);
        seedField.setMaxLength(64);
        seedField.setText(seedText);
        seedField.setChangedListener(s -> seedText = s);
        addDrawableChild(seedField);
        btn(L + inner - applyW, y, applyW, 18, () -> Text.literal("Übernehmen"), null, true, this::applySeed);

        y += 44;
        stepper(y, () -> stepRadius(-1), () -> stepRadius(1));
    }

    private void applySeed() {
        String s = seedText.trim();
        if (s.isEmpty()) {
            seedStatus = "Bitte einen Seed eingeben.";
            seedStatusColor = 0xFFFF6B6B;
            return;
        }
        long seed;
        try {
            seed = Long.parseLong(s);
        } catch (NumberFormatException e) {
            seed = s.hashCode(); // Text-Seeds rechnet Minecraft genauso um
        }
        c.seed = seed;
        c.hasSeed = true;
        OreConfig.save();
        OreTracker.reset();
        seedText = Long.toString(seed);
        seedField.setText(seedText);
        seedStatus = "Seed gesetzt.";
        seedStatusColor = 0xFF30D158;
    }

    private void stepRadius(int dir) {
        int idx = 0;
        for (int i = 0; i < RADII.length; i++) if (RADII[i] <= c.radius) idx = i;
        idx = Math.max(0, Math.min(RADII.length - 1, idx + dir));
        c.radius = RADII[idx];
    }

    // ------------------------------------------------------------ Farben

    private int curColor() { return editDebris ? c.debrisColor : c.diamondColor; }

    private void setColor(int rgb) {
        if (editDebris) c.debrisColor = rgb & 0xFFFFFF; else c.diamondColor = rgb & 0xFFFFFF;
    }

    private void initColors() {
        int y = y0;
        int half = (inner - 6) / 2;
        btn(L, y, half, 16, () -> Text.literal("Diamant"), () -> !editDebris, false, () -> { editDebris = false; clearAndInit(); });
        btn(L + half + 6, y, half, 16, () -> Text.literal("Ancient Debris"), () -> editDebris, false, () -> { editDebris = true; clearAndInit(); });

        int colW = (inner - 10) / 2;
        int R = L + colW + 10;
        y += 24;

        // Linke Spalte: Farbwaehler, Swatches, Hex
        addDrawableChild(new Ui.Picker(L, y, colW, 66, curColor(), rgb -> {
            setColor(rgb);
            if (hexField != null) hexField.setText(hex(rgb));
        }));
        int sy = y + 70;
        int n = SWATCHES.length / 2;
        int sw = (colW - (n - 1) * 3) / n;
        for (int i = 0; i < SWATCHES.length; i++) {
            final int col = SWATCHES[i];
            int sx = L + (i % n) * (sw + 3);
            int syy = sy + (i / n) * 12;
            addDrawableChild(new Ui.Btn(sx, syy, sw, 9, Text::empty, null, false, () -> {
                setColor(col);
                clearAndInit();
            }) {
                @Override
                protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
                    boolean sel = (curColor() & 0xFFFFFF) == col;
                    Ui.round(ctx, getX(), getY(), getWidth(), getHeight(), 3, 0xFF000000 | col,
                            sel ? 0xFFFFFFFF : isHovered() ? 0xC0FFFFFF : 0x60000000);
                }
            });
        }
        int hy = sy + 26;
        hexBox = new int[]{L, hy, colW, 16};
        hexField = new TextFieldWidget(textRenderer, L + 16, hy + 4, colW - 22, 10, Text.literal("Hex"));
        hexField.setDrawsBackground(false);
        hexField.setMaxLength(7);
        hexField.setText(hex(curColor()));
        hexField.setChangedListener(s -> {
            String h = s.replace("#", "").trim();
            if (h.length() == 6) {
                try { setColor(Integer.parseInt(h, 16)); } catch (NumberFormatException ignored) {}
            }
        });
        addDrawableChild(hexField);

        // Rechte Spalte: Vorschau + Schalter
        previewBox = new int[]{R, y, colW, 64};
        int ty = y + 70;
        toggle(R, ty, colW, "Füllung", () -> c.fill, v -> c.fill = v);
        toggle(R, ty + 21, colW, "Durch Wände", () -> c.throughWalls, v -> c.throughWalls = v);
        toggle(R, ty + 42, colW, "Regenbogen", () -> c.rainbow, v -> c.rainbow = v);

        // Regler unten
        int ry = ty + 66;
        sliderY = ry;
        stepper(ry, () -> c.fillOpacity = Math.max(0, c.fillOpacity - 5), () -> c.fillOpacity = Math.min(100, c.fillOpacity + 5));
        stepper(ry + 24, () -> c.lineWidth = Math.max(10, c.lineWidth - 5), () -> c.lineWidth = Math.min(60, c.lineWidth + 5));

    }

    // ------------------------------------------------------------ Bausteine

    private void stepper(int y, Runnable minus, Runnable plus) {
        btn(L, y, 22, 18, () -> Text.literal("-"), null, false, minus);
        btn(L + inner - 22, y, 22, 18, () -> Text.literal("+"), null, false, plus);
    }

    private void btn(int x, int y, int w, int h, Supplier<Text> label, Supplier<Boolean> sel, boolean accent, Runnable r) {
        addDrawableChild(new Ui.Btn(x, y, w, h, label, sel, accent, r));
    }

    private void toggle(int x, int y, int w, String name, Supplier<Boolean> get, Consumer<Boolean> set) {
        addDrawableChild(new Ui.Btn(x, y, w, 18, () -> Text.literal(name), get, false, () -> set.accept(!get.get())) {
            @Override
            protected void renderWidget(DrawContext ctx, int mx, int my, float d) {
                boolean on = get.get();
                Ui.round(ctx, getX(), getY(), getWidth(), getHeight(), 4, isHovered() ? Ui.HOVER : Ui.OFF, 0x30FFFFFF);
                int tx = getX() + 5, ty = getY() + 5;
                Ui.round(ctx, tx, ty, 16, 8, 4, on ? accent() : 0xFF4A505A, 0x00000000);
                int kx = on ? tx + 9 : tx + 1;
                Ui.round(ctx, kx, ty + 1, 6, 6, 3, 0xFFFFFFFF, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, name, tx + 22, getY() + 5, 0xFFFFFFFF);
            }
        });
    }

    /** Akzentfarbe des Menues folgt der Diamant-Farbe. */
    private int accent() { return OreRender.color(OreSim.Kind.DIAMOND, null); }

    private void bar(DrawContext ctx, int y, String label, int value, int min, int max) {
        int bx = L + 26, bw = inner - 52;
        Ui.round(ctx, bx, y, bw, 18, 4, 0x88000000, 0x40FFFFFF);
        float t = (value - min) / (float) (max - min);
        int fillW = Math.max(8, Math.round((bw - 4) * t));
        int a = accent();
        Ui.round(ctx, bx + 2, y + 2, fillW, 14, 3, (0xB0 << 24) | (a & 0xFFFFFF), 0x00000000);
        ctx.drawCenteredTextWithShadow(textRenderer, label, bx + bw / 2, y + 5, 0xFFFFFFFF);
    }

    private static String hex(int rgb) { return String.format("#%06X", rgb & 0xFFFFFF); }

    // ------------------------------------------------------------ Zeichnen

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int a = accent();

        // Glas-Panel
        Ui.round(ctx, px - 2, py - 2, pw + 4, ph + 4, 11, 0x30000000, 0x00000000);
        Ui.round(ctx, px, py, pw, ph, 10, 0xB8101722, 0x55FFFFFF);
        ctx.fillGradient(px + 4, py + 2, px + pw - 4, py + 40, 0x28FFFFFF, 0x04FFFFFF);
        Ui.round(ctx, px + 24, py, pw - 48, 2, 1, a, a);

        ctx.drawTextWithShadow(textRenderer, "SANTA ORESIM", L, py + 12, a);
        String st = status();
        ctx.drawTextWithShadow(textRenderer, st, px + pw - 14 - textRenderer.getWidth(st), py + 12, 0xFF7A828C);

        if (tab == 0) renderGeneral(ctx); else renderColors(ctx);

        super.render(ctx, mx, my, delta);

        if (tab == 0 && seedText.isEmpty() && !seedField.isFocused())
            ctx.drawTextWithShadow(textRenderer, "z. B. -5746398532788178098", L + 6, y0 + 70 + 5, 0xFF7A828C);
    }

    private void renderGeneral(DrawContext ctx) {
        int half = (inner - 6) / 2;
        int sy = y0 + 46;
        int dia = OreRender.color(OreSim.Kind.DIAMOND, null), deb = OreRender.color(OreSim.Kind.DEBRIS, null);
        Ui.round(ctx, L + 2, sy - 1, 6, 6, 3, dia, dia);
        ctx.drawTextWithShadow(textRenderer, "Diamant-Farbe", L + 12, sy - 1, 0xFF6B737D);
        Ui.round(ctx, L + half + 8, sy - 1, 6, 6, 3, deb, deb);
        ctx.drawTextWithShadow(textRenderer, "Debris-Farbe", L + half + 18, sy - 1, 0xFF6B737D);

        int fy = y0 + 70;
        ctx.drawTextWithShadow(textRenderer, "Welt-Seed", L, fy - 11, 0xFFB0B8C0);
        Ui.round(ctx, L, fy, inner - 76, 18, 4, 0x88000000, 0x40FFFFFF);
        if (!seedStatus.isEmpty()) ctx.drawTextWithShadow(textRenderer, seedStatus, L, fy + 22, seedStatusColor);

        int ry = fy + 44;
        ctx.drawTextWithShadow(textRenderer, "Radius", L, ry - 11, 0xFFB0B8C0);
        bar(ctx, ry, c.radius + " Blöcke", c.radius, 16, 256);
    }

    private void renderColors(DrawContext ctx) {
        // Hex-Feld
        Ui.round(ctx, hexBox[0], hexBox[1], hexBox[2], hexBox[3], 4, 0x88000000, 0x40FFFFFF);
        int cur = 0xFF000000 | curColor();
        Ui.round(ctx, hexBox[0] + 4, hexBox[1] + 4, 8, 8, 2, cur, 0x80FFFFFF);

        // Vorschau
        int x = previewBox[0], y = previewBox[1], w = previewBox[2], h = previewBox[3];
        Ui.round(ctx, x, y, w, h, 6, 0x90000000, 0x38FFFFFF);
        ctx.drawTextWithShadow(textRenderer, "Vorschau", x + 6, y + 5, 0xFF6B737D);
        OreSim.Kind kind = editDebris ? OreSim.Kind.DEBRIS : OreSim.Kind.DIAMOND;
        int stroke = OreRender.color(kind, null);
        int size = Math.min(34, h - 22);
        int d = size / 3;
        int bx = x + (w - size - d) / 2, by = y + 18 + d / 2;
        drawCube(ctx, bx, by, size, d, stroke);

        // Regler
        int ry = sliderY;
        bar(ctx, ry, "Füllung " + c.fillOpacity + " %", c.fillOpacity, 0, 100);
        bar(ctx, ry + 24, "Linie " + String.format("%.1f", c.lineWidth / 10f) + " px", c.lineWidth, 10, 60);
    }

    /** Kleiner Pseudo-3D-Wuerfel wie die ESP-Box im Spiel. */
    private void drawCube(DrawContext ctx, int x, int y, int s, int d, int stroke) {
        int lw = Math.max(1, Math.round(c.lineWidth / 20f));
        if (c.fill && c.fillOpacity > 0) {
            int fill = OreRender.fillColor(stroke, true);
            ctx.fill(x, y, x + s, y + s, fill);
            // Oberseite + rechte Seite leicht dunkler
            for (int i = 0; i < d; i++) {
                ctx.fill(x + i + 1, y - i - 1, x + s + i + 1, y - i, fill);
                ctx.fill(x + s + i, y - i, x + s + i + 1, y + s - i - 1, fill);
            }
        }
        // Vorderseite
        rect(ctx, x, y, s, s, lw, stroke);
        // Rueckseite
        rect(ctx, x + d, y - d, s, s, lw, (0x90 << 24) | (stroke & 0xFFFFFF));
        // Verbindungen
        for (int i = 0; i <= d; i++) {
            ctx.fill(x + i, y - i, x + i + lw, y - i + lw, stroke);
            ctx.fill(x + s + i - lw, y - i, x + s + i, y - i + lw, stroke);
            ctx.fill(x + s + i - lw, y + s - i - lw, x + s + i, y + s - i, stroke);
            ctx.fill(x + i, y + s - i - lw, x + i + lw, y + s - i, (0x90 << 24) | (stroke & 0xFFFFFF));
        }
    }

    private static void rect(DrawContext ctx, int x, int y, int w, int h, int t, int col) {
        ctx.fill(x, y, x + w, y + t, col);
        ctx.fill(x, y + h - t, x + w, y + h, col);
        ctx.fill(x, y, x + t, y + h, col);
        ctx.fill(x + w - t, y, x + w, y + h, col);
    }

    private String status() {
        if (OreSim.error() != null) return "Fehler";
        if (!c.enabled) return "aus";
        if (!OreSim.isReady()) return "lädt ...";
        return OreTracker.visible().size() + " Boxen";
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void removed() { OreConfig.save(); }

    @Override
    public void close() { client.setScreen(parent); }
}
