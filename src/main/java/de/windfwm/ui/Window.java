package de.windfwm.ui;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;

/**
 * Basis für ein frei verschiebbares Modulfenster mit Titelleiste,
 * Einstellungsansicht (Zahnrad), Schließen-Knopf und Größenänderung unten rechts.
 */
public abstract class Window {
    public static final int TITLE_H = 22;
    public static final int PAD = 8;

    public final String id;
    public final String title;
    public final Icons icon;
    public int x, y, w, h;
    public boolean open;
    protected boolean showSettings;
    protected final int defW, defH;
    protected int minW = 210, minH = 150;
    private boolean loaded;

    protected Window(String id, String title, Icons icon, int defW, int defH) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.defW = defW;
        this.defH = defH;
    }

    /** Inhalt zeichnen. (x, y, w, h) ist der Innenbereich. */
    protected abstract void content(UI ui, int x, int y, int w, int h);

    /** Moduleinstellungen zeichnen. */
    protected void settings(UI ui, int x, int y, int w, int h) {
        Draw.text(ui.ctx, "Keine Einstellungen", x, y, Theme.TEXT_DIM);
    }

    protected boolean hasSettings() {
        return true;
    }

    // ------------------------------------------------------------------ Zustand

    public void loadState(int sw, int sh, int index) {
        if (loaded) {
            clamp(sw, sh);
            return;
        }
        loaded = true;
        WindConfig.WindowState st = WindFwm.cfg().windows.get(id);
        w = defW;
        h = defH;
        x = -1;
        y = -1;
        if (st != null) {
            open = st.open;
            if (st.w > 0) w = st.w;
            if (st.h > 0) h = st.h;
            x = st.x;
            y = st.y;
        }
        if (x < 0 || y < 0) {
            // Gestaffelt rechts neben der Navigationsleiste platzieren
            x = WindScreen.SIDEBAR_W + 40 + index * 18;
            y = 30 + index * 16;
        }
        clamp(sw, sh);
    }

    public void saveState() {
        WindConfig.WindowState st = WindFwm.cfg().windows.computeIfAbsent(id, k -> new WindConfig.WindowState());
        st.x = x;
        st.y = y;
        st.w = w;
        st.h = h;
        st.open = open;
        WindFwm.data().config.markDirty();
    }

    public void clamp(int sw, int sh) {
        w = Math.max(Math.min(minW, sw - 8), Math.min(w, sw - 8));
        h = Math.max(Math.min(minH, sh - 8), Math.min(h, sh - 8));
        x = Math.max(0, Math.min(x, sw - w));
        y = Math.max(0, Math.min(y, sh - h));
    }

    public boolean hitTest(int px, int py) {
        return open && px >= x && py >= y && px < x + w && py < y + h;
    }

    // ------------------------------------------------------------------ Zeichnen

    public final void render(UI ui, WindScreen screen) {
        String animKey = "win#" + id;
        float a = ui.anim(animKey, open ? 1 : 0, 16f);
        if (!open && a <= 0.01f) return;
        float base = Draw.alpha;
        Draw.alpha = base * a;
        ui.owner = id;
        boolean front = screen.isFront(this);

        // leichtes "Hereingleiten" beim Öffnen
        int oy = Math.round((1 - a) * 8);
        int wx = x, wy = y + oy;

        Draw.shadow(ui.ctx, wx, wy, w, h, 6, front ? 120 : 70);
        Draw.panel(ui.ctx, wx, wy, w, h, 6, Theme.BG, front ? Theme.mix(Theme.BORDER, Theme.accent(), 0.25f) : Theme.BORDER);

        // Titelleiste
        Draw.round(ui.ctx, wx + 1, wy + 1, w - 2, TITLE_H, 5, Theme.SURFACE);
        Draw.rect(ui.ctx, wx + 1, wy + TITLE_H - 4, w - 2, 4, Theme.SURFACE);
        Draw.hLine(ui.ctx, wx + 1, wy + TITLE_H, w - 2, Theme.BORDER_SOFT);
        Draw.icon(ui.ctx, icon, wx + 8, wy + 7, Theme.accent());
        String t = showSettings ? title + "  ›  Einstellungen" : title;
        Draw.textTrim(ui.ctx, t, wx + 22, wy + 8, w - 70, Theme.TEXT);

        int bx = wx + w - 20;
        if (ui.iconButton(bx, wy + 4, 15, Icons.CLOSE, "Schließen", false)) {
            screen.closeWindow(this);
        }
        if (hasSettings()) {
            bx -= 18;
            if (ui.iconButton(bx, wy + 4, 15, showSettings ? Icons.BACK : Icons.GEAR,
                    showSettings ? "Zurück" : "Einstellungen", showSettings)) {
                showSettings = !showSettings;
            }
        }
        // Verschieben über die Titelleiste
        if (ui.pressed(wx, wy, bx - wx, TITLE_H)) {
            screen.beginDrag(this, ui.mx - x, ui.my - y);
        }

        int gx = wx + w - 9, gy = wy + h - 9;
        if (ui.pressed(gx - 2, gy - 2, 11, 11)) screen.beginResize(this);

        int cx = wx + PAD, cy = wy + TITLE_H + 7, cw = w - PAD * 2, ch = h - TITLE_H - 7 - PAD;
        try {
            if (showSettings) settings(ui, cx, cy, cw, ch);
            else content(ui, cx, cy, cw, ch);
        } catch (Exception e) {
            Draw.text(ui.ctx, "Fehler: " + e.getClass().getSimpleName(), cx, cy, Theme.RED);
            WindFwm.LOG.error("[WindFWM] Fehler im Fenster {}", id, e);
        }

        // Griff zur Größenänderung
        boolean gripHover = ui.hover(gx - 2, gy - 2, 11, 11);
        int gc = gripHover ? Theme.accent() : Theme.TEXT_FAINT;
        Draw.rect(ui.ctx, gx + 5, gy + 1, 1, 5, gc);
        Draw.rect(ui.ctx, gx + 1, gy + 5, 5, 1, gc);
        Draw.rect(ui.ctx, gx + 3, gy + 3, 1, 1, gc);

        ui.owner = "main";
        Draw.alpha = base;
    }
}
