package de.windfwm.ui.modules;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.GameContext;
import de.windfwm.core.Keybinds;
import de.windfwm.core.WaypointService;
import de.windfwm.data.Models;
import de.windfwm.ui.Draw;
import de.windfwm.ui.Icons;
import de.windfwm.ui.Theme;
import de.windfwm.ui.UI;
import de.windfwm.ui.Window;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;

import java.util.List;

/** Waypoint Manager: Koordinaten speichern, benennen, Dimensionen, Entfernung und Richtung. */
public final class WaypointWindow extends Window {
    private String name = "", xs = "", ys = "", zs = "";
    private String message = "";
    private long messageTime;
    private String editing;

    public WaypointWindow() {
        super("waypoints", "Waypoint Manager", Icons.PIN, 350, 300);
    }

    private void info(String m) {
        message = m;
        messageTime = System.currentTimeMillis();
    }

    @Override
    protected void content(UI ui, int x, int y, int w, int h) {
        name = ui.textField("wp-name", x, y, w - 100, name, "Name des Waypoints", 40);
        if (ui.button(x + w - 96, y, 96, 16, "Hier speichern", UI.Style.PRIMARY) || ui.submitted("wp-name")) {
            Models.Waypoint wp = WaypointService.addAtPlayer(name);
            if (wp != null) {
                info("Gespeichert: " + wp.name);
                name = "";
            } else info("Nicht in einer Welt");
        }
        int fw = (w - 100 - 8) / 3;
        xs = ui.textField("wp-x", x, y + 20, fw, xs, "X", 9);
        ys = ui.textField("wp-y", x + fw + 4, y + 20, fw, ys, "Y", 5);
        zs = ui.textField("wp-z", x + (fw + 4) * 2, y + 20, fw, zs, "Z", 9);
        if (ui.button(x + w - 96, y + 20, 96, "Koordinaten")) {
            try {
                int px = Integer.parseInt(xs.trim()), py = ys.isBlank() ? 64 : Integer.parseInt(ys.trim()), pz = Integer.parseInt(zs.trim());
                Models.Waypoint wp = WaypointService.add(name, px, py, pz);
                info("Gespeichert: " + wp.name);
                name = xs = ys = zs = "";
            } catch (NumberFormatException e) {
                info("X und Z als ganze Zahlen angeben");
            }
        }
        WindConfig.Waypoints c = WindFwm.cfg().waypoints;
        String scope = (c.currentServerOnly ? "Dieser Server" : "Alle Server") + (c.currentDimensionOnly ? " · " + GameContext.dimensionLabel(GameContext.dimension()) : "");
        Draw.text(ui.ctx, scope, x, y + 42, Theme.TEXT_FAINT);
        if (!message.isEmpty() && System.currentTimeMillis() - messageTime < 4000) {
            Draw.textRight(ui.ctx, message, x + w, y + 42, Theme.accent());
        }

        List<Models.Waypoint> list = WaypointService.visible();
        int ly = y + 54, lh = h - 54, rowH = 24;
        int contentH = list.size() * rowH;
        int off = ui.beginScroll("wp-list", x, ly, w, lh, contentH);
        Models.Waypoint remove = null;
        Models.Waypoint tracked = WaypointService.tracked();
        for (int i = 0; i < list.size(); i++) {
            int ry = ly + i * rowH - off;
            if (!ui.visible(ry, rowH)) continue;
            Models.Waypoint wp = list.get(i);
            boolean hov = ui.hover(x, ry, w, rowH - 2);
            boolean isTracked = tracked == wp;
            Draw.round(ui.ctx, x, ry, w - 4, rowH - 2, 4, isTracked ? Theme.accent(26) : (hov ? Theme.SURFACE_2 : Theme.SURFACE));
            Draw.round(ui.ctx, x + 3, ry + 4, 2, rowH - 10, 1, 0xFF000000 | wp.color);
            int bx = x + w - 4 - 3 * 15 - 2;
            if (wp.id.equals(editing)) {
                String n = ui.textField("wp-edit", x + 10, ry + 3, bx - x - 14, wp.name, "Name", 40);
                if (!n.equals(wp.name)) {
                    wp.name = n;
                    WindFwm.data().waypoints.markDirty();
                }
                if (ui.submitted("wp-edit") || !"wp-edit".equals(ui.focused)) editing = null;
            } else {
                Draw.textTrim(ui.ctx, wp.name, x + 10, ry + 3, bx - x - 80, Theme.TEXT);
                double d = WaypointService.distance(wp);
                String dist = d < 0 ? "—" : WaypointService.arrow(wp) + " " + Fmt.distance(d);
                Draw.textRight(ui.ctx, dist, bx - 4, ry + 3, d < 0 ? Theme.TEXT_FAINT : Theme.accent());
                String coords = wp.x + " / " + wp.y + " / " + wp.z + "  ·  ";
                Draw.text(ui.ctx, coords, x + 10, ry + 12, Theme.TEXT_FAINT);
                Draw.textTrim(ui.ctx, GameContext.dimensionLabel(wp.dimension) + (c.currentServerOnly ? "" : " · " + wp.server),
                        x + 10 + Draw.width(coords), ry + 12, bx - x - 14 - Draw.width(coords), GameContext.dimensionColor(wp.dimension));
                if (hov) {
                    String hint = WaypointService.netherHint(wp);
                    if (!hint.isEmpty() && ui.hover(x + 10, ry + 11, Draw.width(coords), 10)) ui.tooltip(hint);
                }
            }
            if (ui.iconButton(bx, ry + 4, 14, Icons.TARGET, isTracked ? "Nicht mehr verfolgen" : "Im HUD verfolgen", isTracked)) {
                c.trackedId = isTracked ? null : wp.id;
                WindFwm.data().config.markDirty();
            }
            if (ui.iconButton(bx + 15, ry + 4, 14, Icons.COPY, "Koordinaten kopieren", false)) {
                MinecraftClient.getInstance().keyboard.setClipboard(wp.x + " " + wp.y + " " + wp.z);
                info("Koordinaten kopiert");
            }
            if (ui.iconButton(bx + 30, ry + 4, 14, Icons.TRASH, "Löschen", false)) remove = wp;
            if (ui.pressed(x, ry, bx - x, rowH - 2, 1)) {
                // Rechtsklick: umbenennen
                editing = wp.id;
                ui.focus("wp-edit");
            }
        }
        ui.endScroll("wp-list", x, ly, w, lh, contentH);
        if (list.isEmpty()) {
            Draw.textTrim(ui.ctx, "Keine Waypoints. Taste: " + Keybinds.label(Keybinds.addWaypoint), x + 3, ly + 3, w, Theme.TEXT_FAINT);
        } else {
            Draw.textRight(ui.ctx, "Rechtsklick = umbenennen", x + w, y + h - 1, Theme.TEXT_FAINT);
        }
        if (remove != null) WaypointService.remove(remove);
    }

    @Override
    protected void settings(UI ui, int x, int y, int w, int h) {
        WindConfig.Waypoints c = WindFwm.cfg().waypoints;
        boolean a = ui.toggleRow(x, y, w, "Nur Waypoints dieses Servers", null, c.currentServerOnly);
        boolean b = ui.toggleRow(x, y + 20, w, "Nur aktuelle Dimension", null, c.currentDimensionOnly);
        boolean hud = ui.toggleRow(x, y + 40, w, "Verfolgten Waypoint im HUD zeigen", null, WindFwm.cfg().hud.showWaypoint);
        if (a != c.currentServerOnly || b != c.currentDimensionOnly || hud != WindFwm.cfg().hud.showWaypoint) {
            c.currentServerOnly = a;
            c.currentDimensionOnly = b;
            WindFwm.cfg().hud.showWaypoint = hud;
            WindFwm.data().config.markDirty();
        }
        Draw.textTrim(ui.ctx, "Gespeichert: " + WaypointService.all().size() + " Waypoints insgesamt", x, y + 66, w, Theme.TEXT_FAINT);
        Draw.textTrim(ui.ctx, "Taste 'Waypoint setzen': " + Keybinds.label(Keybinds.addWaypoint), x, y + 78, w, Theme.TEXT_FAINT);
    }
}
