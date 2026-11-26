package de.windfwm.ui;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.core.WaypointService;
import de.windfwm.data.Models;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/** Kompaktes Overlay im Spiel: Kontostand, Einnahmen/h, verfolgter Waypoint. */
public final class Hud {
    private Hud() {}

    private record Line(String label, String value, int color) {}

    public static void render(DrawContext ctx) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            WindConfig.Hud cfg = WindFwm.cfg().hud;
            if (!cfg.enabled || mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof WindScreen) return;

            List<Line> lines = new ArrayList<>();
            Models.ServerProfile p = WindFwm.money().profile();
            if (cfg.showBalance) {
                lines.add(new Line("Konto", p.hasBalance ? Fmt.money(p.balance) : "—", Theme.TEXT));
            }
            if (cfg.showRate) {
                lines.add(new Line("pro Std.", Fmt.money(WindFwm.stats().ratePerHour), Theme.accent()));
            }
            if (cfg.showSession) {
                lines.add(new Line("Sitzung", Fmt.signed(WindFwm.stats().sessionIncome - WindFwm.stats().sessionExpense),
                        Theme.GREEN));
            }
            if (cfg.showWaypoint) {
                Models.Waypoint w = WaypointService.tracked();
                if (w != null) {
                    double d = WaypointService.distance(w);
                    String v = d < 0 ? "andere Dim." : WaypointService.arrow(w) + " " + Fmt.distance(d);
                    lines.add(new Line(Draw.trim(w.name, 60), v, Theme.TEXT));
                }
            }
            if (lines.isEmpty()) return;

            int labelW = 0, valueW = 0;
            for (Line l : lines) {
                labelW = Math.max(labelW, Draw.width(l.label));
                valueW = Math.max(valueW, Draw.width(l.value));
            }
            int w = labelW + valueW + 22;
            int h = lines.size() * 11 + 8;
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            int x = (cfg.corner == 1 || cfg.corner == 3) ? sw - w - 4 : 4;
            int y = (cfg.corner >= 2) ? sh - h - 4 : 4;

            Draw.alpha = 1f;
            Draw.round(ctx, x, y, w, h, 4, 0xC0121416);
            Draw.round(ctx, x, y + 4, 2, h - 8, 1, Theme.accent());
            int ly = y + 5;
            for (Line l : lines) {
                Draw.text(ctx, l.label, x + 8, ly, Theme.TEXT_DIM);
                Draw.textRight(ctx, l.value, x + w - 6, ly, l.color);
                ly += 11;
            }
        } catch (Exception e) {
            // HUD darf das Spiel niemals stören
        }
    }
}
