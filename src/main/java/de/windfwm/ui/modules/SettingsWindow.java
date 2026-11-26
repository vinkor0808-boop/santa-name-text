package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.GameContext;
import de.windfwm.core.Keybinds;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import net.minecraft.client.option.KeyBinding;

/** Globale Einstellungen: Darstellung, Tastenbelegung, HUD, Daten. */
public final class SettingsWindow extends Window {
    private static final String[] CORNERS = {"Oben links", "Oben rechts", "Unten links", "Unten rechts"};
    private String message = "";
    private long messageTime;

    public SettingsWindow() {
        super("settings", "Einstellungen", Icons.GEAR, 320, 320);
    }

    @Override
    protected boolean hasSettings() {
        return false;
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        WindConfig cfg = WindFwm.cfg();
        int contentH = 430;
        int off = ui.beginScroll("settings", x, y, w, h, contentH);
        int cy = y - off;
        int rw = w - 6;
        boolean changed = false;

        ui.sectionTitle("Allgemein", x, cy);
        cy += 12;
        boolean anim = ui.toggleRow(x, cy, rw, "Animationen", null, cfg.general.animations);
        cy += 20;
        boolean only = ui.toggleRow(x, cy, rw, "Geld/Chat-Funktionen nur auf WindSMP",
                "Ohne Haken laufen alle Funktionen auf jedem Server", cfg.general.windSmpOnly);
        cy += 20;
        if (anim != cfg.general.animations || only != cfg.general.windSmpOnly) {
            cfg.general.animations = anim;
            cfg.general.windSmpOnly = only;
            changed = true;
        }
        Draw.textTrim(ui.ctx, "Server: " + GameContext.serverKey() + (GameContext.isWindSmp() ? "  (WindSMP erkannt)" : ""),
                x, cy, rw, GameContext.isWindSmp() ? Theme.GREEN : Theme.TEXT_FAINT);
        cy += 16;

        ui.sectionTitle("Akzentfarbe", x, cy);
        cy += 12;
        for (int i = 0; i < Theme.ACCENTS.length; i++) {
            int sx = x + i * 22;
            int col = 0xFF000000 | Theme.ACCENTS[i];
            boolean sel = (cfg.general.accent & 0xFFFFFF) == Theme.ACCENTS[i];
            if (sel) Draw.round(ui.ctx, sx - 2, cy - 2, 20, 20, 5, Theme.TEXT);
            Draw.round(ui.ctx, sx, cy, 16, 16, 4, col);
            if (ui.pressed(sx, cy, 16, 16)) {
                cfg.general.accent = Theme.ACCENTS[i];
                changed = true;
            }
        }
        cy += 26;

        ui.sectionTitle("Tastenbelegung", x, cy);
        cy += 12;
        cy = keyRow(ui, x, cy, rw, "Menü öffnen", Keybinds.openMenu);
        cy = keyRow(ui, x, cy, rw, "Waypoint setzen", Keybinds.addWaypoint);
        cy = keyRow(ui, x, cy, rw, "HUD ein/aus", Keybinds.toggleHud);
        Draw.textTrim(ui.ctx, "Klicken, dann Taste drücken · Entf = keine Taste · Esc = Abbruch", x, cy, rw, Theme.TEXT_FAINT);
        cy += 16;

        ui.sectionTitle("HUD", x, cy);
        cy += 12;
        WindConfig.Hud hud = cfg.hud;
        boolean he = ui.toggleRow(x, cy, rw, "HUD anzeigen", null, hud.enabled);
        cy += 20;
        Draw.text(ui.ctx, "Position", x, cy + 4, Theme.TEXT);
        if (ui.button(x + rw - 90, cy, 90, CORNERS[Math.floorMod(hud.corner, 4)])) {
            hud.corner = (hud.corner + 1) % 4;
            changed = true;
        }
        cy += 22;
        boolean hb = ui.toggleRow(x, cy, rw, "Kontostand", null, hud.showBalance);
        cy += 20;
        boolean hr = ui.toggleRow(x, cy, rw, "Einnahmen pro Stunde", null, hud.showRate);
        cy += 20;
        boolean hs = ui.toggleRow(x, cy, rw, "Sitzungsbilanz", null, hud.showSession);
        cy += 20;
        boolean hw = ui.toggleRow(x, cy, rw, "Verfolgter Waypoint", null, hud.showWaypoint);
        cy += 24;
        if (he != hud.enabled || hb != hud.showBalance || hr != hud.showRate || hs != hud.showSession || hw != hud.showWaypoint) {
            hud.enabled = he;
            hud.showBalance = hb;
            hud.showRate = hr;
            hud.showSession = hs;
            hud.showWaypoint = hw;
            changed = true;
        }

        ui.sectionTitle("Daten", x, cy);
        cy += 12;
        Draw.textTrim(ui.ctx, "Ordner: .minecraft/config/windfwm/", x, cy, rw, Theme.TEXT_DIM);
        cy += 14;
        if (ui.button(x, cy, 110, "Jetzt speichern")) {
            WindFwm.data().saveAllNow();
            message = "Gespeichert";
            messageTime = ui.now;
        }
        if (!message.isEmpty() && ui.now - messageTime < 3000) Draw.text(ui.ctx, message, x + 118, cy + 4, Theme.accent());
        ui.endScroll("settings", x, y, w, h, contentH);

        if (changed) WindFwm.data().config.markDirty();
    }

    private int keyRow(UI ui, int x, int y, int w, String label, KeyBinding binding) {
        Draw.text(ui.ctx, label, x, y + 4, Theme.TEXT);
        boolean capturing = ui.capture == binding;
        String text = capturing ? "Taste drücken …" : Keybinds.label(binding);
        if (ui.button(x + w - 100, y, 100, 16, text, capturing ? UI.Style.PRIMARY : UI.Style.NORMAL)) {
            ui.capture = capturing ? null : binding;
        }
        return y + 20;
    }
}
