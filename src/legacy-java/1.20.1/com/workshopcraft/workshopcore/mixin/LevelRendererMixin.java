package com.workshopcraft.workshopcore.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void workshopcore$backdrop(PoseStack pose, Matrix4f projection, float partialTick, Camera camera, boolean isFoggy, Runnable setupFog, CallbackInfo ci) {
        if (!BackdropClient.hasActive() || isFoggy) {
            return;
        }
        if (BackdropClient.render(pose.last().pose(), setupFog)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderClouds", at = @At("HEAD"), cancellable = true)
    private void workshopcore$hideClouds(PoseStack pose, Matrix4f projection, float partialTick, double x, double y, double z, CallbackInfo ci) {
        if (BackdropClient.hasActive()) {
            ci.cancel();
        }
    }
}
