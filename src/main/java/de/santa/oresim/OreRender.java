package de.santa.oresim;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public final class OreRender {
    private OreRender() {}

    private static boolean errorLogged;

    /** Aktuelle Randfarbe (ARGB) fuer eine Erzart, inkl. Regenbogen-Modus. */
    public static int color(OreSim.Kind kind, BlockPos pos) {
        OreConfig c = OreConfig.INSTANCE;
        if (c.rainbow) {
            float off = pos == null ? 0f : ((pos.getX() + pos.getY() + pos.getZ()) * 0.02f);
            float hue = ((System.currentTimeMillis() % 4000L) / 4000f + off) % 1f;
            return 0xFF000000 | (MathHelper.hsvToRgb(hue, 0.75f, 1f) & 0xFFFFFF);
        }
        return 0xFF000000 | ((kind == OreSim.Kind.DIAMOND ? c.diamondColor : c.debrisColor) & 0xFFFFFF);
    }

    public static int fillColor(int stroke, boolean confirmed) {
        OreConfig c = OreConfig.INSTANCE;
        float a = c.fillOpacity / 100f * (confirmed ? 1f : 0.55f);
        int alpha = Math.max(0, Math.min(255, Math.round(a * 255)));
        return (alpha << 24) | (stroke & 0xFFFFFF);
    }

    public static void draw() {
        OreConfig c = OreConfig.INSTANCE;
        if (!c.enabled) return;
        float width = c.lineWidth / 10f;
        for (OreTracker.Visible v : OreTracker.visible()) {
            int stroke = color(v.kind(), v.pos());
            DrawStyle style = c.fill && c.fillOpacity > 0
                    ? DrawStyle.filledAndStroked(stroke, width, fillColor(stroke, v.confirmed()))
                    : DrawStyle.stroked(stroke, width);
            var box = GizmoDrawing.box(v.pos(), style);
            if (c.throughWalls) box.ignoreOcclusion();
        }
    }

    public static void onError(Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            SantaOreSim.LOGGER.error("OreSim Render-Fehler", t);
        }
    }
}
