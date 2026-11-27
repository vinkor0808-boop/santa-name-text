package de.santa.oresim;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/** Glas-Menue fuer Santa OreSim (auch ueber Mod Menu erreichbar). */
public class OreScreen extends Screen {
    private static final int[] RADII = {16, 32, 48, 64, 80, 96, 112, 128, 160, 192, 224, 256};

    private final Screen parent;
    private final OreConfig c = OreConfig.INSTANCE;
    private int px, py, pw, ph;
    private TextFieldWidget seedField;
    private String seedText;
    private String seedStatus = "";
    private int seedStatusColor = 0xFF7A828C;
    private int[] seedBox;

    public OreScreen(Screen parent) {
        super(Text.literal("Santa OreSim"));
        this.parent = parent;
        this.seedText = c.hasSeed ? Long.toString(c.seed) : "";
    }

    @Override
    protected void init() {
        pw = Math.min(width - 10, 300);
        ph = Math.min(height - 6, 236);
        px = (width - pw) / 2;
        py = (height - ph) / 2;
        int L = px + 14;
        int inner = pw - 28;
        int y = py + 34;

        // Haupt-Schalter
        toggle(L, y, inner, "ESP aktiv", () -> c.enabled, v -> c.enabled = v);
        y += 24;
        int half = (inner - 6) / 2;
        toggle(L, y, half, "Diamanten", () -> c.diamonds, v -> { c.diamonds = v; OreTracker.reset(); });
        toggle(L + half + 6, y, half, "Ancient Debris", () -> c.debris, v -> { c.debris = v; OreTracker.reset(); });

        // Seed
        y += 36;
        int applyW = 70;
        seedBox = new int[]{L, y, inner - applyW - 6, 18};
        seedField = new TextFieldWidget(textRenderer, L + 6, y + 5, seedBox[2] - 12, 10, Text.literal("Seed"));
        seedField.setDrawsBackground(false);
        seedField.setMaxLength(64);
        seedField.setText(seedText);
        seedField.setChangedListener(s -> seedText = s);
        addDrawableChild(seedField);
        btn(L + inner - applyW, y, applyW, 18, () -> Text.literal("Übernehmen"), null, true, this::applySeed);

        // Radius
        y += 38;
        btn(L, y, 22, 18, () -> Text.literal("-"), null, false, () -> stepRadius(-1));
        btn(L + inner - 22, y, 22, 18, () -> Text.literal("+"), null, false, () -> stepRadius(1));

        // Aktionen
        int by = py + ph - 28;
        int bw = (inner - 6) / 2;
        btn(L, by, bw, 18, () -> Text.literal("Neu berechnen"), null, false, () -> {
            OreTracker.reset();
            seedStatus = "Wird neu berechnet ...";
            seedStatusColor = 0xFFB0B8C0;
        });
        btn(L + bw + 6, by, bw, 18, () -> Text.literal("Fertig"), null, true, this::close);
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
            // Text-Seeds werden von Minecraft genauso per hashCode umgerechnet
            seed = s.hashCode();
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
                Ui.round(ctx, tx, ty, 16, 8, 4, on ? 0xFF30D158 : 0xFF4A505A, 0x00000000);
                int kx = on ? tx + 9 : tx + 1;
                Ui.round(ctx, kx, ty + 1, 6, 6, 3, 0xFFFFFFFF, 0xFFFFFFFF);
                ctx.drawTextWithShadow(textRenderer, name, tx + 22, getY() + 5, 0xFFFFFFFF);
            }
        });
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int L = px + 14;
        int inner = pw - 28;

        // Glas-Panel
        Ui.round(ctx, px - 2, py - 2, pw + 4, ph + 4, 11, 0x30000000, 0x00000000);
        Ui.round(ctx, px, py, pw, ph, 10, 0xB8101722, 0x55FFFFFF);
        ctx.fillGradient(px + 4, py + 2, px + pw - 4, py + 40, 0x28FFFFFF, 0x04FFFFFF);
        Ui.round(ctx, px + 24, py, pw - 48, 2, 1, 0xFF33E8FF, 0xFF33E8FF);

        ctx.drawTextWithShadow(textRenderer, "SANTA ORESIM", L, py + 12, 0xFF33E8FF);
        String st = status();
        ctx.drawTextWithShadow(textRenderer, st, px + pw - 14 - textRenderer.getWidth(st), py + 12, 0xFF7A828C);

        // Farblegende neben den Erz-Schaltern
        int sy = py + 34 + 24 + 22;
        ctx.fill(L, sy, L + 6, sy + 2, 0xFF33E8FF);
        ctx.drawTextWithShadow(textRenderer, "Cyan", L + 9, sy - 3, 0xFF6B737D);
        int half = (inner - 6) / 2;
        ctx.fill(L + half + 6, sy, L + half + 12, sy + 2, 0xFFFF7A1F);
        ctx.drawTextWithShadow(textRenderer, "Orange", L + half + 15, sy - 3, 0xFF6B737D);

        // Seed
        ctx.drawTextWithShadow(textRenderer, "Welt-Seed", L, seedBox[1] - 11, 0xFFB0B8C0);
        Ui.round(ctx, seedBox[0], seedBox[1], seedBox[2], seedBox[3], 4, 0x88000000, 0x40FFFFFF);
        if (!seedStatus.isEmpty())
            ctx.drawTextWithShadow(textRenderer, seedStatus, L, seedBox[1] + 22, seedStatusColor);

        // Radius
        int ry = seedBox[1] + 38;
        ctx.drawTextWithShadow(textRenderer, "Radius", L, ry - 11, 0xFFB0B8C0);
        int bx = L + 26, bw = inner - 52;
        Ui.round(ctx, bx, ry, bw, 18, 4, 0x88000000, 0x40FFFFFF);
        float t = (c.radius - 16) / (float) (256 - 16);
        int fillW = Math.max(8, Math.round((bw - 4) * t));
        Ui.round(ctx, bx + 2, ry + 2, fillW, 14, 3, 0xB033E8FF, 0x00000000);
        ctx.drawCenteredTextWithShadow(textRenderer, c.radius + " Blöcke", bx + bw / 2, ry + 5, 0xFFFFFFFF);

        super.render(ctx, mx, my, delta);

        if (seedText.isEmpty() && !seedField.isFocused())
            ctx.drawTextWithShadow(textRenderer, "z. B. -5746398532788178098", seedBox[0] + 6, seedBox[1] + 5, 0xFF7A828C);
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
