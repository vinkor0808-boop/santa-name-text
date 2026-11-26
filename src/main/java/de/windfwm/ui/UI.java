package de.windfwm.ui;

import de.windfwm.WindFwm;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kleines "Immediate-Mode"-GUI: Widgets werden jedes Frame gezeichnet und melden
 * direkt zurück, ob sie angeklickt/geändert wurden. Zustand (Fokus, Scrollposition,
 * Animationen) wird über IDs gehalten. Klicks gehören immer genau einem "Owner"
 * (oberstes Fenster unter der Maus oder "main"), damit verdeckte Elemente nicht reagieren.
 */
public final class UI {
    public static final int FIELD_H = 16;

    public enum Style { PRIMARY, NORMAL, GHOST, DANGER }

    public DrawContext ctx;
    public int mx, my;
    public long now;
    public float dt;

    public String owner = "main";
    public String hoverOwner = "main";
    public String clickOwner = "main";

    private boolean clickPending, clickConsumed;
    private int clickX, clickY, clickButton;
    private double scrollPending;

    public String focused;
    private boolean focusTouched;
    private final StringBuilder typed = new StringBuilder();
    private final List<int[]> keys = new ArrayList<>();
    private String submitted;
    /** Tastenbelegung, die gerade neu gesetzt wird (null = keine). */
    public KeyBinding capture;

    private final Map<String, Float> anims = new HashMap<>();
    private final Map<String, Integer> cursors = new HashMap<>();
    private final Map<String, Float> scrolls = new HashMap<>();
    private final Map<String, Float> scrollTargets = new HashMap<>();
    private final Map<String, Long> confirms = new HashMap<>();
    private final Deque<int[]> clips = new ArrayDeque<>();
    private String tooltip;
    private long lastFrame;

    // ------------------------------------------------------------------ Frame

    public void begin(DrawContext ctx, int mx, int my) {
        this.ctx = ctx;
        this.mx = mx;
        this.my = my;
        long t = System.nanoTime();
        dt = lastFrame == 0 ? 0.016f : Math.min(0.1f, (t - lastFrame) / 1e9f);
        lastFrame = t;
        now = System.currentTimeMillis();
        submitted = null;
        tooltip = null;
        focusTouched = false;
        owner = "main";
        clips.clear();
    }

    public void end() {
        if (clickPending && !focusTouched) focused = null;
        clickPending = false;
        scrollPending = 0;
        typed.setLength(0);
        keys.clear();
        Draw.alpha = 1f;
        if (tooltip != null) drawTooltip(tooltip);
    }

    // ------------------------------------------------------------------ Eingaben vom Screen

    public void onClick(int x, int y, int button) {
        clickPending = true;
        clickConsumed = false;
        clickX = x;
        clickY = y;
        clickButton = button;
    }

    public void onScroll(double amount) {
        scrollPending += amount;
    }

    public void onChar(String s) {
        if (focused != null && s != null) typed.append(s);
    }

    public void onKey(int key, int modifiers) {
        if (focused == null) return;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            focused = null;
            return;
        }
        if (key == GLFW.GLFW_KEY_V && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
            String clip = MinecraftClient.getInstance().keyboard.getClipboard();
            if (clip != null) typed.append(clip.replace("\n", " ").replace("\r", ""));
            return;
        }
        keys.add(new int[]{key, modifiers});
    }

    /** Textfeld programmatisch fokussieren. */
    public void focus(String id) {
        focused = id;
        focusTouched = true;
    }

    public boolean hasFocus() {
        return focused != null;
    }

    // ------------------------------------------------------------------ Hilfen

    public boolean inside(int px, int py, int x, int y, int w, int h) {
        return px >= x && py >= y && px < x + w && py < y + h;
    }

    private boolean inClip(int px, int py) {
        int[] c = clips.peek();
        return c == null || inside(px, py, c[0], c[1], c[2], c[3]);
    }

    public boolean hover(int x, int y, int w, int h) {
        return owner.equals(hoverOwner) && inside(mx, my, x, y, w, h) && inClip(mx, my);
    }

    /** Linksklick in den Bereich (verbraucht den Klick). */
    public boolean pressed(int x, int y, int w, int h) {
        return pressed(x, y, w, h, 0);
    }

    public boolean pressed(int x, int y, int w, int h, int button) {
        if (!clickPending || clickConsumed || clickButton != button) return false;
        if (!owner.equals(clickOwner)) return false;
        if (!inside(clickX, clickY, x, y, w, h) || !inClip(clickX, clickY)) return false;
        clickConsumed = true;
        return true;
    }

    /** Glättet einen Wert Richtung Ziel (für Animationen). */
    public float anim(String id, float target, float speed) {
        if (!WindFwm.cfg().general.animations) {
            anims.put(id, target);
            return target;
        }
        float v = anims.getOrDefault(id, target);
        v += (target - v) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - v) < 0.002f) v = target;
        anims.put(id, v);
        return v;
    }

    public float anim(String id, float target) {
        return anim(id, target, 14f);
    }

    /** Startwert für eine Animation setzen (z. B. Einblenden ab 0). */
    public void setAnim(String id, float value) {
        anims.put(id, value);
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    private void drawTooltip(String text) {
        int w = Draw.width(text) + 10;
        int x = Math.min(mx + 10, ctx.getScaledWindowWidth() - w - 2);
        int y = Math.max(2, my - 16);
        Draw.panel(ctx, x, y, w, 14, 3, Theme.SURFACE_3, Theme.BORDER);
        Draw.text(ctx, text, x + 5, y + 3, Theme.TEXT);
    }

    // ------------------------------------------------------------------ Widgets

    public boolean button(int x, int y, int w, int h, String label, Style style) {
        String key = owner + "#btn" + x + ":" + y + label;
        boolean hov = hover(x, y, w, h);
        float t = anim(key, hov ? 1 : 0, 18f);
        int bg, fg;
        switch (style) {
            case PRIMARY -> {
                bg = Theme.mix(Theme.accent(), 0xFFFFFFFF, t * 0.18f);
                fg = 0xFF0E1114;
            }
            case DANGER -> {
                bg = Theme.mix(0xFF3A2427, 0xFF5A2B2F, t);
                fg = Theme.RED;
            }
            case GHOST -> {
                bg = Theme.withAlpha(0xFFFFFF, Math.round(t * 14));
                fg = Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t);
            }
            default -> {
                bg = Theme.mix(Theme.SURFACE_3, 0xFF3A4048, t);
                fg = Theme.TEXT;
            }
        }
        Draw.round(ctx, x, y, w, h, 4, bg);
        Draw.textCentered(ctx, Draw.trim(label, w - 6), x + w / 2, y + (h - 8) / 2, fg);
        return pressed(x, y, w, h);
    }

    public boolean button(int x, int y, int w, String label) {
        return button(x, y, w, FIELD_H, label, Style.NORMAL);
    }

    /** Button, der nach dem ersten Klick 3 Sekunden lang "Sicher?" zeigt und erst beim zweiten auslöst. */
    public boolean confirmButton(String id, int x, int y, int w, int h, String label) {
        Long armed = confirms.get(id);
        boolean isArmed = armed != null && now - armed < 3000;
        if (button(x, y, w, h, isArmed ? "Sicher?" : label, isArmed ? Style.DANGER : Style.NORMAL)) {
            if (isArmed) {
                confirms.remove(id);
                return true;
            }
            confirms.put(id, now);
        }
        return false;
    }

    public boolean iconButton(int x, int y, int size, Icons icon, String tip, boolean active) {
        String key = owner + "#ib" + x + ":" + y + icon.name();
        boolean hov = hover(x, y, size, size);
        float t = anim(key, hov ? 1 : 0, 18f);
        if (t > 0.01f || active) {
            Draw.round(ctx, x, y, size, size, 3, active ? Theme.accent(40) : Theme.withAlpha(0xFFFFFF, Math.round(t * 18)));
        }
        int col = active ? Theme.accent() : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t);
        Draw.icon(ctx, icon, x + (size - Icons.SIZE) / 2, y + (size - Icons.SIZE) / 2, col);
        if (hov && tip != null) tooltip(tip);
        return pressed(x, y, size, size);
    }

    /** Schalter (22x12). Liefert den neuen Wert. */
    public boolean toggle(int x, int y, boolean value) {
        String key = owner + "#tg" + x + ":" + y;
        int w = 22, h = 12;
        boolean hov = hover(x, y, w, h);
        boolean result = value;
        if (pressed(x - 2, y - 2, w + 4, h + 4)) result = !value;
        float t = anim(key, result ? 1 : 0, 16f);
        int track = Theme.mix(Theme.SURFACE_3, Theme.accent(), t);
        if (hov) track = Theme.mix(track, 0xFFFFFFFF, 0.08f);
        Draw.round(ctx, x, y, w, h, 6, track);
        int knobX = x + 2 + Math.round(t * (w - 12));
        Draw.round(ctx, knobX, y + 2, 8, 8, 4, Theme.mix(0xFFB8C0C8, 0xFF0E1114, t));
        return result;
    }

    /** Zeile mit Beschriftung links und Schalter rechts; die ganze Zeile ist klickbar. */
    public boolean toggleRow(int x, int y, int w, String label, String hint, boolean value) {
        boolean hov = hover(x, y, w, 18);
        if (hov) Draw.round(ctx, x - 4, y - 2, w + 8, 18, 3, Theme.withAlpha(0xFFFFFF, 8));
        Draw.textTrim(ctx, label, x, y + 3, w - 30, Theme.TEXT);
        if (hint != null && hov) tooltip(hint);
        boolean v = toggle(x + w - 22, y + 2, value);
        if (v == value && pressed(x, y - 2, w - 26, 18)) v = !value;
        return v;
    }

    /** Reiter als Pillen. Liefert den ausgewählten Index. */
    public int tabs(String id, int x, int y, String[] labels, int selected) {
        int cx = x;
        int result = selected;
        for (int i = 0; i < labels.length; i++) {
            int w = Draw.width(labels[i]) + 14;
            boolean sel = i == selected;
            boolean hov = hover(cx, y, w, 15);
            float t = anim(owner + "#tab" + id + i, sel ? 1 : (hov ? 0.35f : 0), 16f);
            if (sel && t > 0.01f) Draw.round(ctx, cx, y, w, 15, 4, Theme.accent(Math.round(45 * t)));
            else if (t > 0.01f) Draw.round(ctx, cx, y, w, 15, 4, Theme.withAlpha(0xFFFFFF, Math.round(t * 20)));
            Draw.text(ctx, labels[i], cx + 7, y + 4, sel ? Theme.accent() : Theme.mix(Theme.TEXT_DIM, Theme.TEXT, t));
            if (pressed(cx, y, w, 15)) result = i;
            cx += w + 4;
        }
        return result;
    }

    public boolean submitted(String id) {
        return id.equals(submitted);
    }

    /** Einzeiliges Textfeld. Liefert den (eventuell geänderten) Wert. */
    public String textField(String id, int x, int y, int w, String value, String placeholder, int maxLen) {
        int h = FIELD_H;
        String v = value == null ? "" : value;
        boolean hov = hover(x, y, w, h);
        if (pressed(x, y, w, h)) {
            focused = id;
            focusTouched = true;
            cursors.put(id, v.length());
        }
        boolean foc = id.equals(focused);
        if (foc && clickPending && inside(clickX, clickY, x, y, w, h)) focusTouched = true;
        int cur = Math.max(0, Math.min(v.length(), cursors.getOrDefault(id, v.length())));
        if (foc) {
            if (typed.length() > 0) {
                StringBuilder clean = new StringBuilder();
                typed.codePoints().filter(cp -> cp >= 32 && cp != 127 && cp != '§').forEach(clean::appendCodePoint);
                String ins = clean.toString();
                if (v.length() + ins.length() > maxLen) ins = ins.substring(0, Math.max(0, maxLen - v.length()));
                v = v.substring(0, cur) + ins + v.substring(cur);
                cur += ins.length();
                typed.setLength(0);
            }
            for (int[] k : keys) {
                boolean ctrl = (k[1] & GLFW.GLFW_MOD_CONTROL) != 0;
                switch (k[0]) {
                    case GLFW.GLFW_KEY_BACKSPACE -> {
                        if (ctrl) {
                            v = v.substring(cur);
                            cur = 0;
                        } else if (cur > 0) {
                            v = v.substring(0, cur - 1) + v.substring(cur);
                            cur--;
                        }
                    }
                    case GLFW.GLFW_KEY_DELETE -> {
                        if (cur < v.length()) v = v.substring(0, cur) + v.substring(cur + 1);
                    }
                    case GLFW.GLFW_KEY_LEFT -> cur = Math.max(0, cur - 1);
                    case GLFW.GLFW_KEY_RIGHT -> cur = Math.min(v.length(), cur + 1);
                    case GLFW.GLFW_KEY_HOME -> cur = 0;
                    case GLFW.GLFW_KEY_END -> cur = v.length();
                    case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> submitted = id;
                    default -> { }
                }
            }
            keys.clear();
            cursors.put(id, cur);
        }
        float t = anim(owner + "#tf" + id, foc ? 1 : (hov ? 0.4f : 0), 16f);
        int border = Theme.mix(Theme.BORDER, Theme.accent(), foc ? t : 0);
        if (!foc && hov) border = Theme.mix(Theme.BORDER, 0xFF4A515A, t);
        Draw.panel(ctx, x, y, w, h, 4, foc ? Theme.SURFACE_3 : Theme.SURFACE_2, border);
        int innerW = w - 8;
        String before = v.substring(0, cur);
        int shift = Math.max(0, Draw.width(before) - innerW);
        ctx.enableScissor(x + 2, y, x + w - 2, y + h);
        if (v.isEmpty() && !foc && placeholder != null) {
            Draw.text(ctx, placeholder, x + 4, y + 4, Theme.TEXT_FAINT);
        } else {
            Draw.text(ctx, v, x + 4 - shift, y + 4, Theme.TEXT);
        }
        if (foc && (now / 500) % 2 == 0) {
            Draw.rect(ctx, x + 4 - shift + Draw.width(before), y + 3, 1, 10, Theme.accent());
        }
        ctx.disableScissor();
        return v;
    }

    // ------------------------------------------------------------------ Scrollbereiche

    /** Beginnt einen scrollbaren Bereich. Liefert den aktuellen Versatz in Pixeln. */
    public int beginScroll(String id, int x, int y, int w, int h, int contentH) {
        String key = owner + "#sc" + id;
        float max = Math.max(0, contentH - h);
        float target = scrollTargets.getOrDefault(key, 0f);
        if (scrollPending != 0 && hover(x, y, w, h)) {
            target -= (float) scrollPending * 22f;
            scrollPending = 0;
        }
        target = Math.max(0, Math.min(max, target));
        scrollTargets.put(key, target);
        float cur = scrolls.getOrDefault(key, target);
        cur = WindFwm.cfg().general.animations ? cur + (target - cur) * (1f - (float) Math.exp(-18f * dt)) : target;
        if (Math.abs(target - cur) < 0.3f) cur = target;
        cur = Math.max(0, Math.min(max, cur));
        scrolls.put(key, cur);
        ctx.enableScissor(x, y, x + w, y + h);
        clips.push(new int[]{x, y, w, h});
        return Math.round(cur);
    }

    public void endScroll(String id, int x, int y, int w, int h, int contentH) {
        ctx.disableScissor();
        clips.pop();
        if (contentH > h) {
            String key = owner + "#sc" + id;
            float cur = scrolls.getOrDefault(key, 0f);
            int barH = Math.max(14, Math.round(h * (h / (float) contentH)));
            int barY = y + Math.round((h - barH) * (cur / (contentH - h)));
            Draw.round(ctx, x + w - 3, barY, 2, barH, 1, Theme.withAlpha(0xFFFFFF, 40));
        }
    }

    public void resetScroll(String id) {
        String key = owner + "#sc" + id;
        scrollTargets.put(key, 0f);
        scrolls.put(key, 0f);
    }

    /** Rechteck sichtbar im aktuellen Scrollbereich? (Zeilen außerhalb nicht zeichnen.) */
    public boolean visible(int y, int h) {
        int[] c = clips.peek();
        return c == null || (y + h >= c[1] && y <= c[1] + c[3]);
    }

    // ------------------------------------------------------------------ Kleinteile

    public void label(String text, int x, int y) {
        Draw.text(ctx, text, x, y, Theme.TEXT_DIM);
    }

    public void sectionTitle(String text, int x, int y) {
        Draw.text(ctx, text.toUpperCase(java.util.Locale.ROOT), x, y, Theme.TEXT_FAINT);
    }
}
