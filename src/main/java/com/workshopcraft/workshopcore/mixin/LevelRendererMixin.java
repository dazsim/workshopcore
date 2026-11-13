package com.workshopcraft.workshopcore.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Final
    private LevelTargetBundle targets;

    @Shadow
    @Final
    private LevelRenderState levelRenderState;

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void workshopcore$backdrop(FrameGraphBuilder frame, CameraRenderState cameraState, GpuBufferSlice skyFog, CallbackInfo ci) {
        if (!BackdropClient.hasActive()) {
            return;
        }
        FogType fogType = cameraState.fogType;
        if (fogType == FogType.POWDER_SNOW || fogType == FogType.LAVA || cameraState.entityRenderState.doesMobEffectBlockSky) {
            ci.cancel();
            return;
        }
        if (this.levelRenderState.skyRenderState.skybox == DimensionType.Skybox.NONE) {
            return;
        }
        FramePass pass = frame.addPass("workshopcore_sky");
        this.targets.main = pass.readsAndWrites(this.targets.main);
        pass.executes(() -> BackdropClient.render(RenderSystem.getModelViewMatrix(), () -> RenderSystem.setShaderFog(skyFog)));
        ci.cancel();
    }
}
