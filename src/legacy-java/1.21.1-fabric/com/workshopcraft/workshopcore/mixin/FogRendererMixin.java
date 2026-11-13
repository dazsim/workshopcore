package com.workshopcraft.workshopcore.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public class FogRendererMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private static void workshopcore$hazeFog(Camera camera, FogRenderer.FogMode mode, float renderDistance, boolean thick, float partialTick, CallbackInfo ci) {
        if (!HazeClient.hasHaze(partialTick)) {
            return;
        }
        RenderSystem.setShaderFogStart(HazeClient.nearPlane(RenderSystem.getShaderFogStart(), partialTick));
        RenderSystem.setShaderFogEnd(HazeClient.farPlane(RenderSystem.getShaderFogEnd(), partialTick));
    }

    @Inject(method = "setupColor", at = @At("RETURN"))
    private static void workshopcore$hazeColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float bossColor, CallbackInfo ci) {
        if (!HazeClient.hasHaze(partialTick)) {
            return;
        }
        fogRed = HazeClient.fogRed(fogRed, partialTick);
        fogGreen = HazeClient.fogGreen(fogGreen, partialTick);
        fogBlue = HazeClient.fogBlue(fogBlue, partialTick);
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
}
