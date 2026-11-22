package de.santa.santanametext;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

public final class NameBuilder {
    private NameBuilder() {}

    /** Render-State des eigenen Spielers (wird vom Mixin gesetzt). */
    public static Text lastMain;

    public static double bounce() {
        return SantaConfig.get().bounce ? Math.abs(Math.sin(Util.getMeasuringTimeMs() / 260.0)) * 0.12 : 0;
    }

    /** Tier-Zusatz (links/rechts) + Prefix | Name | Suffix, Trenner frei faerbbar. */
    public static Text main(String playerName, Text original) {
        SantaConfig c = SantaConfig.get();
        Text extras = (original != null && c.tiersMode != 2) ? extras(original, playerName) : null;
        MutableText out = Text.empty();
        if (extras != null && c.tiersMode == 1) {
            out.append(extras);
            out.append(sep(c));
        }
        if (!c.prefix.text.isBlank()) {
            out.append(styled(c.prefix, c.prefix.text));
            out.append(sep(c));
        }
        out.append(styled(c.name, playerName));
        if (!c.suffix.text.isBlank()) {
            out.append(sep(c));
            out.append(styled(c.suffix, c.suffix.text));
        }
        if (extras != null && c.tiersMode == 0) {
            out.append(sep(c));
            out.append(extras);
        }
        return out;
    }

    private static Text sep(SantaConfig c) {
        String t = c.sep.text == null ? "" : c.sep.text.trim();
        if (t.isEmpty()) return Text.literal(" ");
        return Text.empty().append(Text.literal(" ")).append(styled(c.sep, t)).append(Text.literal(" "));
    }

    /** Alles aus dem urspruenglichen Anzeigenamen, was nicht der Spielername ist (z.B. Tier). */
    private static Text extras(Text original, String playerName) {
        List<Integer> cps = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        original.asOrderedText().accept((index, style, cp) -> {
            cps.add(cp);
            styles.add(style);
            return true;
        });
        int[] nameCps = playerName.codePoints().toArray();
        int at = -1;
        for (int i = 0; i + nameCps.length <= cps.size() && at < 0; i++) {
            boolean ok = true;
            for (int j = 0; j < nameCps.length; j++) {
                if (cps.get(i + j) != nameCps[j]) { ok = false; break; }
            }
            if (ok) at = i;
        }
        if (at < 0) return null;

        MutableText extras = Text.empty();
        StringBuilder pend = new StringBuilder();
        List<Style> pendSt = new ArrayList<>();
        boolean any = false;
        for (int i = 0; i < cps.size(); i++) {
            if (i >= at && i < at + nameCps.length) continue;
            int cp = cps.get(i);
            if (!any && Character.isWhitespace(cp)) continue;
            extras.append(Text.literal(new String(Character.toChars(cp))).setStyle(styles.get(i)));
            if (!Character.isWhitespace(cp)) any = true;
        }
        return any ? extras : null;
    }

    public static Text line(SantaConfig.TagElement e) { return styled(e, e.text); }

    public static Text sample(FontStyle fs, boolean selected) {
        Style st = Style.EMPTY.withColor(selected ? 0xFFD60A : 0xFFFFFF);
        if (fs.fontId != null) st = st.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", fs.fontId)));
        return Text.literal(fs.apply("Ab")).setStyle(st);
    }

    public static Text styled(SantaConfig.TagElement e, String raw) {
        MutableText out = Text.empty();
        String t = e.font.apply(raw);
        Style base = Style.EMPTY.withBold(e.bold).withItalic(e.italic)
                .withUnderline(e.underline).withStrikethrough(e.strike);
        if (e.font.fontId != null) {
            base = base.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", e.font.fontId)));
        }
        int[] cps = t.codePoints().toArray();
        long time = Util.getMeasuringTimeMs();
        for (int i = 0; i < cps.length; i++) {
            int rgb = color(e.effect, i, cps.length, time, e.colorA, e.colorB);
            out.append(Text.literal(new String(Character.toChars(cps[i]))).setStyle(base.withColor(rgb)));
        }
        return out;
    }

    private static int color(Effect e, int i, int n, long t, int a, int b) {
        switch (e) {
            case RAINBOW: {
                float h = ((t / 1500f) + i * 0.07f) % 1f;
                return MathHelper.hsvToRgb(h, 0.85f, 1f);
            }
            case GRADIENT:
                return lerp(a, b, n > 1 ? i / (float) (n - 1) : 0f);
            case WAVE: {
                float p = (float) ((Math.sin(t / 500.0 - i * 0.45) + 1) / 2);
                return lerp(a, b, p);
            }
            case METALLIC: {
                double p = ((t / 90.0) - i * 1.2) % 12;
                if (p < 0) p += 12;
                float s = p < 3 ? (float) Math.sin(p / 3 * Math.PI) : 0f;
                return lerp(a, 0xFFFFFF, s * 0.9f);
            }
            case FIRE: {
                float f = (float) (Math.sin(t / 130.0 + i * 1.7) * 0.5 + 0.5);
                return lerp(0xFF3B00, 0xFFD000, f);
            }
            case ICE: {
                float f = (float) (Math.sin(t / 400.0 + i * 0.6) * 0.5 + 0.5);
                return lerp(0x3FB8FF, 0xE8FBFF, f);
            }
            case PULSE: {
                float f = (float) (0.65 + 0.35 * Math.sin(t / 300.0));
                return lerp(0x000000, a, f);
            }
            case SHIMMER: {
                double p = ((t / 45.0) - i * 1.0) % 16;
                if (p < 0) p += 16;
                float f = p < 2 ? (float) Math.sin(p / 2 * Math.PI) : 0f;
                return lerp(a, 0xFFFFFF, f);
            }
            case CHROMATIC:
                return (((t / 220) + i) % 2 == 0) ? 0xFF4A4A : 0x4AE8FF;
            case FADE: {
                float f = (float) (Math.sin(t / 500.0 - i * 0.5) * 0.5 + 0.5);
                return lerp(0x2A2A2A, a, 0.15f + 0.85f * f);
            }
            case GLOWING: {
                float f = (float) (Math.sin(t / 380.0) * 0.5 + 0.5);
                return lerp(a, 0xFFFFFF, f * 0.8f);
            }
            case ITERATING: {
                int pos = (int) ((t / 130) % Math.max(1, n + 3));
                int d = Math.abs(i - pos);
                return d == 0 ? 0xFFFFFF : d == 1 ? lerp(a, 0xFFFFFF, 0.45f) : a;
            }
            case NEON: {
                long k = (t / 70 + i * 13) % 23;
                return k == 0 ? lerp(a, 0x000000, 0.55f) : lerp(a, 0xFFFFFF, 0.25f);
            }
            case BLINK:
                return ((t / 450) % 2 == 0) ? a : b;
            case SUNSET: {
                float p = (float) (((t / 1800.0) + i * 0.08) % 1.0);
                return p < 0.5f ? lerp(0xFF8A00, 0xFF2D92, p * 2) : lerp(0xFF2D92, 0xFF8A00, (p - 0.5f) * 2);
            }
            case OCEAN: {
                float p = (float) (Math.sin(t / 700.0 - i * 0.4) * 0.5 + 0.5);
                return lerp(0x0A84FF, 0x30FFD0, p);
            }
            case CANDY:
                return (((t / 350) + i) % 2 == 0) ? 0xFF7AC8 : 0x7AE8FF;
            case MATRIX: {
                int pos = (int) ((t / 100) % Math.max(1, n + 4));
                int d = pos - i;
                if (d < 0 || d > 4) return 0x0E8A2E;
                return lerp(0xC8FFD0, 0x0E8A2E, d / 4f);
            }
            case GLITTER:
                return ((t / 120 + i * 7) % 9 == 0) ? 0xFFFFFF : a;
            default:
                return a;
        }
    }

    private static int lerp(int a, int b, float p) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * p));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * p));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * p));
        return (r << 16) | (g << 8) | bl;
    }
}
