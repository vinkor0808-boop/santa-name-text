package de.windfwm.ui;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.ChatSender;
import de.windfwm.core.GameContext;
import de.windfwm.core.Keybinds;
import de.windfwm.data.Models;
import de.windfwm.feature.AutoMessages;
import de.windfwm.feature.OrderTracker;
import de.windfwm.util.Fmt;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Menü des Add-ons: Schnellnachrichten, Auto-Nachrichten, Orders, Einstellungen. */
public final class WindScreen extends Screen {
    private static final String[] TABS = {"Schnellnachrichten", "Auto-Nachrichten", "Orders", "Einstellungen"};
    private static final Icons[] TAB_ICONS = {Icons.CHAT, Icons.TARGET, Icons.CHART, Icons.GEAR};
    private static final String[] CORNERS = {"Oben links", "Oben rechts", "Unten links", "Unten rechts"};
    private static int tab;

    private final UI ui = new UI();
    private int mouseX, mouseY;
    private int selectedQuick = -1;
    private final Map<WindConfig.AutoMessage, String> intervalText = new IdentityHashMap<>();
    private String notice = "";
    private long noticeTime;

    public WindScreen() {
        super(Text.literal("WindFWM"));
        ui.setAnim("screen", 0f);
    }

    private void notice(String s) {
        notice = s;
        noticeTime = System.currentTimeMillis();
    }

    private void dirty() {
        WindFwm.data().config.markDirty();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // eigener Hintergrund in render()
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        mouseX = mx;
        mouseY = my;
        ui.hoverOwner = "main";
        ui.begin(ctx, mx, my);
        Draw.alpha = ui.anim("screen", 1f, 12f);

        Draw.rect(ctx, 0, 0, width, height, Theme.BACKDROP);
        int pw = Math.min(width - 24, 500), ph = Math.min(height - 24, 320);
        int px = (width - pw) / 2, py = (height - ph) / 2 + Math.round((1 - Draw.alpha) * 10);
        Draw.shadow(ctx, px, py, pw, ph, 8, 140);
        Draw.panel(ctx, px, py, pw, ph, 8, Theme.BG, Theme.mix(Theme.BORDER, Theme.accent(), 0.3f));

        // Kopfzeile
        Draw.round(ctx, px + 10, py + 9, 18, 18, 5, Theme.accent());
        Draw.textCentered(ctx, "W", px + 19, py + 14, 0xFF0E1114);
        Draw.textScaled(ctx, "WindFWM", px + 34, py + 9, 1.25f, Theme.TEXT);
        Draw.text(ctx, "Add-on für FWM · " + GameContext.serverLabel(), px + 34, py + 22, Theme.TEXT_DIM);
        if (ui.iconButton(px + pw - 24, py + 9, 16, Icons.CLOSE, "Schließen", false)) close();
        Draw.hLine(ctx, px + 1, py + 34, pw - 2, Theme.BORDER_SOFT);

        // Navigation
        int navW = 122;
        Draw.rect(ctx, px + navW, py + 35, 1, ph - 36, Theme.BORDER_SOFT);
        for (int i = 0; i < TABS.length; i++) {
            int ny = py + 42 + i * 24;
            boolean active = tab == i;
            boolean hov = ui.hover(px + 6, ny, navW - 12, 21);
            float t = ui.anim("nav" + i, active ? 1 : (hov ? 0.4f : 0), 16f);
            if (t > 0.01f) Draw.round(ctx, px + 6, ny, navW - 12, 21, 5, active ? Theme.accent(Math.round(40 * t)) : Theme.withAlpha(0xFFFFFF, Math.round(t * 22)));
            if (active) Draw.round(ctx, px + 6, ny + 5, 2, 11, 1, Theme.accent());
            int col = active ? Theme.accent() : Theme.TEXT;
            Draw.icon(ctx, TAB_ICONS[i], px + 14, ny + 6, col);
            Draw.textTrim(ctx, TABS[i], px + 29, ny + 7, navW - 40, col);
            if (ui.pressed(px + 6, ny, navW - 12, 21)) tab = i;
        }

        int cx = px + navW + 12, cy = py + 44, cw = pw - navW - 24, ch = ph - 54;
        Draw.textScaled(ctx, TABS[tab], cx, cy - 2, 1.25f, Theme.TEXT);
        if (!notice.isEmpty() && ui.now - noticeTime < 3500) Draw.textRight(ctx, notice, cx + cw, cy, Theme.accent());
        cy += 18;
        ch -= 18;
        try {
            switch (tab) {
                case 0 -> quickTab(cx, cy, cw, ch);
                case 1 -> autoTab(cx, cy, cw, ch);
                case 2 -> ordersTab(cx, cy, cw, ch);
                default -> settingsTab(cx, cy, cw, ch);
            }
        } catch (Exception e) {
            Draw.text(ctx, "Fehler: " + e.getClass().getSimpleName(), cx, cy, Theme.RED);
            WindFwm.LOG.error("[WindFWM] Fehler im Menü", e);
        }
        Draw.alpha = 1f;
        ui.end();
    }

    // ------------------------------------------------------------------ Schnellnachrichten

    private void quickTab(int x, int y, int w, int h) {
        List<WindConfig.QuickMessage> list = WindFwm.cfg().quick;
        Draw.textTrim(ui.ctx, "Ein Klick auf „Senden“ oder die Taste schickt die Nachricht einmal. Befehle mit / gehen auch.",
                x, y, w, Theme.TEXT_DIM);
        int editorH = selectedQuick >= 0 && selectedQuick < list.size() ? 62 : 0;
        int ly = y + 14, lh = h - 14 - 22 - editorH, rowH = 22;
        int contentH = list.size() * rowH;
        int off = ui.beginScroll("quick", x, ly, w, lh, contentH);
        int remove = -1;
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            WindConfig.QuickMessage q = list.get(i);
            boolean sel = i == selectedQuick;
            boolean hov = ui.hover(x, ry, w, rowH - 3);
            Draw.round(ui.ctx, x, ry, w - 4, rowH - 3, 4, sel ? Theme.accent(30) : (hov ? Theme.SURFACE_2 : Theme.SURFACE));
            String key = i < Keybinds.QUICK_KEYS ? Keybinds.label(Keybinds.quick[i]) : "–";
            Draw.round(ui.ctx, x + 4, ry + 3, 34, 13, 3, Theme.SURFACE_3);
            Draw.textCentered(ui.ctx, Draw.trim(key, 30), x + 21, ry + 6, i < Keybinds.QUICK_KEYS ? Theme.TEXT : Theme.TEXT_FAINT);
            Draw.textTrim(ui.ctx, q.name.isBlank() ? "(ohne Namen)" : q.name, x + 44, ry + 6, 80, Theme.TEXT);
            Draw.textTrim(ui.ctx, q.text, x + 128, ry + 6, w - 128 - 90, Theme.TEXT_DIM);
            if (ui.button(x + w - 82, ry + 2, 52, 15, "Senden", UI.Style.PRIMARY)) {
                notice(ChatSender.send(q.text) ? "Gesendet" : "Nicht verbunden");
            }
            if (ui.iconButton(x + w - 26, ry + 2, 15, Icons.TRASH, "Löschen", false)) remove = i;
            else if (ui.pressed(x, ry, w - 86, rowH - 3)) selectedQuick = sel ? -1 : i;
        }
        ui.endScroll("quick", x, ly, w, lh, contentH);
        if (list.isEmpty()) Draw.text(ui.ctx, "Noch keine Nachrichten.", x + 4, ly + 4, Theme.TEXT_FAINT);
        if (remove >= 0) {
            list.remove(remove);
            selectedQuick = -1;
            dirty();
        }
        int by = ly + lh + 4;
        if (ui.button(x, by, 140, 16, "+ Neue Nachricht", UI.Style.NORMAL)) {
            list.add(new WindConfig.QuickMessage("Neu", ""));
            selectedQuick = list.size() - 1;
            dirty();
        }
        Draw.textTrim(ui.ctx, "Die ersten 5 haben Tasten (Einstellungen)", x + 148, by + 4, w - 148, Theme.TEXT_FAINT);

        if (editorH > 0) {
            WindConfig.QuickMessage q = list.get(selectedQuick);
            int ey = by + 22;
            Draw.panel(ui.ctx, x, ey, w - 4, editorH - 4, 5, Theme.SURFACE, Theme.BORDER);
            String n = ui.textField("q-name", x + 6, ey + 6, 100, q.name, "Name", 24);
            String t = ui.textField("q-text", x + 112, ey + 6, w - 122, q.text, "Text oder /befehl", ChatSender.MAX_LENGTH);
            if (!n.equals(q.name) || !t.equals(q.text)) {
                q.name = n;
                q.text = t;
                dirty();
            }
            if (selectedQuick < Keybinds.QUICK_KEYS) {
                KeyBinding kb = Keybinds.quick[selectedQuick];
                boolean cap = ui.capture == kb;
                Draw.text(ui.ctx, "Taste:", x + 6, ey + 32, Theme.TEXT_DIM);
                if (ui.button(x + 44, ey + 28, 110, 16, cap ? "Taste drücken …" : Keybinds.label(kb), cap ? UI.Style.PRIMARY : UI.Style.NORMAL)) {
                    ui.capture = cap ? null : kb;
                }
            } else {
                Draw.text(ui.ctx, "Ohne Taste (nur die ersten 5)", x + 6, ey + 32, Theme.TEXT_FAINT);
            }
        }
    }

    // ------------------------------------------------------------------ Auto-Nachrichten

    private void autoTab(int x, int y, int w, int h) {
        WindConfig.Auto c = WindFwm.cfg().auto;
        AutoMessages auto = WindFwm.auto();
        boolean en = ui.toggleRow(x, y, w - 4, "Automatische Nachrichten aktiv", null, c.enabled);
        boolean only = ui.toggleRow(x, y + 20, w - 4, "Nur auf WindSMP senden", null, c.onlyWindSmp);
        if (en != c.enabled || only != c.onlyWindSmp) {
            c.enabled = en;
            c.onlyWindSmp = only;
            dirty();
        }
        String status;
        int statusCol;
        if (!c.enabled) {
            status = "Aus – es wird nichts gesendet";
            statusCol = Theme.TEXT_FAINT;
        } else if (auto.running()) {
            status = "Läuft";
            statusCol = Theme.GREEN;
        } else {
            status = c.onlyWindSmp && !GameContext.isWindSmp() ? "Pausiert – nicht auf WindSMP" : "Pausiert – nicht verbunden";
            statusCol = Theme.YELLOW;
        }
        Draw.dot(ui.ctx, x + 3, y + 46, statusCol);
        Draw.text(ui.ctx, status, x + 10, y + 42, statusCol);
        Draw.textRight(ui.ctx, "Intervall z. B. 90s, 5m, 1h · min. 30s", x + w - 4, y + 42, Theme.TEXT_FAINT);

        List<WindConfig.AutoMessage> list = c.messages;
        int ly = y + 56, lh = h - 56 - 22, rowH = 22;
        int contentH = list.size() * rowH;
        int off = ui.beginScroll("auto", x, ly, w, lh, contentH);
        WindConfig.AutoMessage remove = null;
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            WindConfig.AutoMessage m = list.get(i);
            Draw.round(ui.ctx, x, ry, w - 4, rowH - 3, 4, Theme.SURFACE);
            boolean me = ui.toggle(x + 4, ry + 4, m.enabled);
            if (me != m.enabled) {
                m.enabled = me;
                dirty();
            }
            int textW = w - 4 - 30 - 50 - 50 - 22;
            String t = ui.textField("a-text" + i, x + 30, ry + 2, textW, m.text, "Nachricht (kein /befehl)", ChatSender.MAX_LENGTH);
            if (!t.equals(m.text)) {
                m.text = t;
                dirty();
            }
            String buf = intervalText.computeIfAbsent(m, k -> formatInterval(k.intervalSeconds));
            String nb = ui.textField("a-int" + i, x + 34 + textW, ry + 2, 46, buf, "5m", 8);
            if (!nb.equals(buf)) {
                intervalText.put(m, nb);
                int secs = parseInterval(nb);
                if (secs > 0) {
                    m.intervalSeconds = AutoMessages.clamp(secs);
                    dirty();
                }
            }
            String info;
            int infoCol = Theme.TEXT_DIM;
            if (AutoMessages.isCommand(m.text)) {
                info = "kein /";
                infoCol = Theme.RED;
            } else {
                long s = auto.secondsUntilNext(m);
                info = s < 0 ? "—" : (s >= 60 ? (s / 60) + "m " + (s % 60) + "s" : s + "s");
            }
            Draw.textRight(ui.ctx, info, x + w - 30, ry + 6, infoCol);
            if (ui.iconButton(x + w - 24, ry + 2, 15, Icons.TRASH, "Löschen", false)) remove = m;
        }
        ui.endScroll("auto", x, ly, w, lh, contentH);
        if (list.isEmpty()) Draw.text(ui.ctx, "Noch keine automatischen Nachrichten.", x + 4, ly + 4, Theme.TEXT_FAINT);
        if (remove != null) {
            list.remove(remove);
            intervalText.remove(remove);
            dirty();
        }
        int by = ly + lh + 4;
        if (ui.button(x, by, 140, 16, "+ Neue Nachricht", UI.Style.NORMAL)) {
            list.add(new WindConfig.AutoMessage());
            dirty();
        }
        Draw.textTrim(ui.ctx, "Achte auf die Chatregeln von WindSMP.", x + 148, by + 4, w - 148, Theme.TEXT_FAINT);
    }

    static int parseInterval(String s) {
        String t = s.trim().toLowerCase(java.util.Locale.ROOT).replace(" ", "");
        if (t.isEmpty()) return -1;
        int mult = 1;
        if (t.endsWith("h")) {
            mult = 3600;
            t = t.substring(0, t.length() - 1);
        } else if (t.endsWith("m")) {
            mult = 60;
            t = t.substring(0, t.length() - 1);
        } else if (t.endsWith("s")) {
            t = t.substring(0, t.length() - 1);
        }
        try {
            return (int) Math.round(Double.parseDouble(t.replace(',', '.')) * mult);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String formatInterval(int secs) {
        if (secs % 3600 == 0) return (secs / 3600) + "h";
        if (secs % 60 == 0) return (secs / 60) + "m";
        return secs + "s";
    }

    // ------------------------------------------------------------------ Orders

    private void ordersTab(int x, int y, int w, int h) {
        OrderTracker o = WindFwm.orders();
        o.update();
        int tw = (w - 4 - 12) / 3;
        tile(x, y, tw, "Pro Stunde", Fmt.money(o.perHour), Theme.accent());
        tile(x + tw + 6, y, tw, "Sitzung (" + o.sessionCount + ")", Fmt.money(o.sessionTotal), Theme.GREEN);
        tile(x + (tw + 6) * 2, y, tw, "Heute (" + o.todayCount + ")", Fmt.money(o.todayTotal), Theme.GREEN);
        int ry = y + 44;
        row(x, ry, w - 4, "Gesamt (" + Fmt.number(o.allCount) + " Orders)", Fmt.money(o.allTotal), Theme.TEXT);
        row(x, ry + 13, w - 4, "Ø pro Order / größte", Fmt.money(o.average) + " / " + Fmt.money(o.biggest), Theme.TEXT);
        row(x, ry + 26, w - 4, "Abgeholte Items (Sitzung / heute / gesamt)",
                o.sessionClaimed + " / " + o.todayClaimed + " / " + Fmt.number(o.allClaimed), Theme.TEXT);

        int ly = ry + 46;
        ui.sectionTitle("Letzte Order payments", x, ly);
        ly += 12;
        Models.OrderProfile p = o.profile();
        int listH = h - (ly - y) - 22;
        int rows = Math.max(0, listH / 12);
        int shown = 0;
        for (int i = p.payments.size() - 1; i >= 0 && shown < rows; i--, shown++) {
            Models.Payment pay = p.payments.get(i);
            Draw.text(ui.ctx, Fmt.dateTime(pay.t), x + 2, ly + shown * 12, Theme.TEXT_FAINT);
            Draw.textRight(ui.ctx, "+" + Fmt.money(pay.amount), x + w - 6, ly + shown * 12, Theme.GREEN);
        }
        if (p.payments.isEmpty()) Draw.text(ui.ctx, "Noch keine Orders erfasst.", x + 2, ly, Theme.TEXT_FAINT);

        int by = y + h - 17;
        WindConfig.Orders cfg = WindFwm.cfg().orders;
        boolean hud = ui.toggle(x, by + 2, cfg.hud);
        Draw.text(ui.ctx, "HUD", x + 27, by + 4, Theme.TEXT);
        if (hud != cfg.hud) {
            cfg.hud = hud;
            dirty();
        }
        if (ui.button(x + 52, by, 90, 16, CORNERS[Math.floorMod(cfg.hudCorner, 4)], UI.Style.NORMAL)) {
            cfg.hudCorner = (cfg.hudCorner + 1) % 4;
            dirty();
        }
        if (ui.confirmButton("orders-reset", x + w - 94, by, 90, 16, "Zurücksetzen")) {
            o.reset();
            notice("Orders zurückgesetzt");
        }
    }

    private void tile(int x, int y, int w, String label, String value, int color) {
        Draw.panel(ui.ctx, x, y, w, 36, 5, Theme.SURFACE, Theme.BORDER_SOFT);
        Draw.textTrim(ui.ctx, label, x + 7, y + 6, w - 12, Theme.TEXT_DIM);
        Draw.textScaled(ui.ctx, Draw.trim(value, (int) ((w - 14) / 1.4f)), x + 7, y + 19, 1.4f, color);
    }

    private void row(int x, int y, int w, String label, String value, int color) {
        int vw = Draw.width(value);
        Draw.textTrim(ui.ctx, label, x, y, w - vw - 8, Theme.TEXT_DIM);
        Draw.textRight(ui.ctx, value, x + w, y, color);
    }

    // ------------------------------------------------------------------ Einstellungen

    private void settingsTab(int x, int y, int w, int h) {
        WindConfig.General g = WindFwm.cfg().general;
        int cy = y;
        cy = keyRow(x, cy, w - 4, "Menü öffnen", Keybinds.openMenu);
        for (int i = 0; i < Keybinds.QUICK_KEYS; i++) {
            List<WindConfig.QuickMessage> q = WindFwm.cfg().quick;
            String name = i < q.size() ? q.get(i).name : "(leer)";
            cy = keyRow(x, cy, w - 4, "Schnellnachricht " + (i + 1) + " – " + name, Keybinds.quick[i]);
        }
        Draw.textTrim(ui.ctx, "Klicken, Taste drücken · Entf = keine Taste · Esc = Abbruch · FWM nutzt J, M, Y",
                x, cy, w, Theme.TEXT_FAINT);
        cy += 16;
        boolean anim = ui.toggleRow(x, cy, w - 4, "Animationen", null, g.animations);
        if (anim != g.animations) {
            g.animations = anim;
            dirty();
        }
        cy += 22;
        Draw.text(ui.ctx, "Akzentfarbe", x, cy + 4, Theme.TEXT);
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            int sx = x + 80 + i * 20;
            boolean sel = (g.accent & 0xFFFFFF) == Theme.ACCENTS[i];
            if (sel) Draw.round(ui.ctx, sx - 2, cy - 2, 18, 18, 5, Theme.TEXT);
            Draw.round(ui.ctx, sx, cy, 14, 14, 4, 0xFF000000 | Theme.ACCENTS[i]);
            if (ui.pressed(sx, cy, 14, 14)) {
                g.accent = Theme.ACCENTS[i];
                dirty();
            }
        }
    }

    private int keyRow(int x, int y, int w, String label, KeyBinding binding) {
        Draw.textTrim(ui.ctx, label, x, y + 4, w - 110, Theme.TEXT);
        boolean cap = ui.capture == binding;
        if (ui.button(x + w - 100, y, 100, 16, cap ? "Taste drücken …" : Keybinds.label(binding), cap ? UI.Style.PRIMARY : UI.Style.NORMAL)) {
            ui.capture = cap ? null : binding;
        }
        return y + 19;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public void mouseMoved(double x, double y) {
        mouseX = (int) x;
        mouseY = (int) y;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        ui.clickOwner = "main";
        ui.onClick(mouseX, mouseY, click.button());
        return true;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        ui.onScroll(vertical);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (ui.capture != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) Keybinds.rebind(ui.capture, -1);
            else if (key != GLFW.GLFW_KEY_ESCAPE) Keybinds.rebind(ui.capture, key);
            ui.capture = null;
            return true;
        }
        if (ui.hasFocus()) {
            ui.onKey(key, input.modifiers());
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || Keybinds.openMenu.matchesKey(input)) {
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (ui.hasFocus()) {
            ui.onChar(input.asString());
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public void close() {
        WindFwm.data().config.saveAsync();
        super.close();
    }
}
