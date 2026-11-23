package de.santa.santanametext.mixin;

import de.santa.santanametext.NameBuilder;
import de.santa.santanametext.SantaConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerRendererMixin {
    @Unique private static boolean santa$busy;

    @Shadow
    protected abstract void renderLabelIfPresent(PlayerEntityRenderState state, MatrixStack matrices,
                                                 OrderedRenderCommandQueue queue, CameraRenderState camera);

    /** Eigenen Namen setzen (Vanilla zeigt den eigenen Namen sonst nie an). */
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/PlayerLikeEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V", at = @At("TAIL"))
    private void santa$name(PlayerLikeEntity player, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        Object self = player;
        if (self != (Object) MinecraftClient.getInstance().player || !SantaConfig.get().enabled) return;
        Entity entity = (Entity) self;
        Text main = NameBuilder.main(entity.getName().getString(), state.displayName);
        NameBuilder.lastMain = main;
        state.displayName = main;
        Vec3d pos = state.nameLabelPos != null ? state.nameLabelPos : new Vec3d(0, entity.getHeight(), 0);
        state.nameLabelPos = pos.add(0, NameBuilder.bounce(), 0);
    }

    /** Zeile oben und unten dazurendern (nach dem normalen Namen). */
    @Inject(method = "renderLabelIfPresent(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At("TAIL"))
    private void santa$extra(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                             CameraRenderState camera, CallbackInfo ci) {
        if (santa$busy || state.displayName == null || state.displayName != NameBuilder.lastMain
                || state.nameLabelPos == null) return;
        SantaConfig c = SantaConfig.get();
        if (!c.enabled) return;
        Text oldName = state.displayName;
        Text oldScore = state.playerName;
        Vec3d oldPos = state.nameLabelPos;
        // Vanilla schiebt den Namen um 9*1.15*0.025 nach oben, wenn ein Score (Geld) darunter steht.
        double scoreShift = oldScore != null ? 0.2588 : 0.0;
        double topOff = 0.27 + scoreShift;
        double bottomOff = -0.27;
        // Vanilla zeichnet den Score (z.B. Geld) mit; bei unseren Zusatzzeilen weglassen, sonst doppelt.
        state.playerName = null;
        santa$busy = true;
        try {
            if (!c.top.text.isBlank()) {
                state.displayName = NameBuilder.line(c.top);
                state.nameLabelPos = oldPos.add(0, topOff, 0);
                this.renderLabelIfPresent(state, matrices, queue, camera);
            }
            if (!c.bottom.text.isBlank()) {
                state.displayName = NameBuilder.line(c.bottom);
                state.nameLabelPos = oldPos.add(0, bottomOff, 0);
                this.renderLabelIfPresent(state, matrices, queue, camera);
            }
        } finally {
            state.displayName = oldName;
            state.playerName = oldScore;
            state.nameLabelPos = oldPos;
            santa$busy = false;
        }
    }
}
