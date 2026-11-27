package de.santa.oresim;

import net.minecraft.client.render.DrawStyle;
import net.minecraft.world.debug.gizmo.GizmoDrawing;

public final class OreRender {
    private OreRender() {}

    private static final int DIAMOND = 0xFF33E8FF;
    private static final int DIAMOND_FILL = 0x3033E8FF;
    private static final int DEBRIS = 0xFFFF7A1F;
    private static final int DEBRIS_FILL = 0x30FF7A1F;
    private static boolean errorLogged;

    public static void draw() {
        if (!OreConfig.INSTANCE.enabled) return;
        for (OreTracker.Visible v : OreTracker.visible()) {
            boolean dia = v.kind() == OreSim.Kind.DIAMOND;
            int stroke = dia ? DIAMOND : DEBRIS;
            DrawStyle style = v.confirmed()
                    ? DrawStyle.filledAndStroked(stroke, 3.0F, dia ? DIAMOND_FILL : DEBRIS_FILL)
                    : DrawStyle.stroked(stroke, 2.0F);
            GizmoDrawing.box(v.pos(), style).ignoreOcclusion();
        }
    }

    public static void onError(Throwable t) {
        if (!errorLogged) {
            errorLogged = true;
            SantaOreSim.LOGGER.error("OreSim Render-Fehler", t);
        }
    }
}
