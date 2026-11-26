package de.windfwm.ui;

import de.windfwm.WindFwm;
import de.windfwm.config.WindConfig;
import de.windfwm.feature.OrderTracker;
import de.windfwm.util.Fmt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Kleines Order-HUD (optional). */
public final class Hud {
    private Hud() {}

    public static void render(DrawContext ctx) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            WindConfig.Orders cfg = WindFwm.cfg().orders;
            if (!cfg.hud || mc.player == null || mc.options.hudHidden || mc.currentScreen instanceof WindScreen) return;
            OrderTracker o = WindFwm.orders();
            o.update();
            String l1 = "Orders/h", v1 = Fmt.money(o.perHour);
            String l2 = "Sitzung", v2 = Fmt.money(o.sessionTotal) + " (" + o.sessionCount + ")";
            int lw = Math.max(Draw.width(l1), Draw.width(l2));
            int vw = Math.max(Draw.width(v1), Draw.width(v2));
            int w = lw + vw + 22, h = 30;
            int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
            int x = (cfg.hudCorner == 1 || cfg.hudCorner == 3) ? sw - w - 4 : 4;
            int y = cfg.hudCorner >= 2 ? sh - h - 4 : 4;
            Draw.alpha = 1f;
            Draw.round(ctx, x, y, w, h, 4, 0xC0121416);
            Draw.round(ctx, x, y + 4, 2, h - 8, 1, Theme.accent());
            Draw.text(ctx, l1, x + 8, y + 5, Theme.TEXT_DIM);
            Draw.textRight(ctx, v1, x + w - 6, y + 5, Theme.accent());
            Draw.text(ctx, l2, x + 8, y + 16, Theme.TEXT_DIM);
            Draw.textRight(ctx, v2, x + w - 6, y + 16, Theme.GREEN);
        } catch (Exception ignored) {
            // HUD darf das Spiel nie stören
        }
    }
}
