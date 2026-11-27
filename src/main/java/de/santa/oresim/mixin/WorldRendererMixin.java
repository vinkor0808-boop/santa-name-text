package de.santa.oresim.mixin;

import de.santa.oresim.OreRender;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.world.debug.gizmo.GizmoDrawing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Haengt die ESP-Boxen jedes Frame in Minecrafts eigenes Gizmo-System (durch Waende sichtbar). */
@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "collectGizmos", at = @At("HEAD"))
    private void santaoresim$collect(CallbackInfo ci) {
        try (GizmoDrawing.CollectorScope scope = ((WorldRenderer) (Object) this).startDrawingGizmos()) {
            OreRender.draw();
        } catch (Throwable t) {
            OreRender.onError(t);
        }
    }
}
