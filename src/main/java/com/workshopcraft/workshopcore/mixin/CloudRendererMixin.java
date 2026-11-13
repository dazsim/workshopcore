package com.workshopcraft.workshopcore.mixin;

//? if >=26.3 {
/*import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.renderer.CloudRenderer;
import net.minecraft.client.renderer.oit.OitRenderPassProvider;
import net.minecraft.client.renderer.oit.OitStage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CloudRenderer.class)
public class CloudRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void workshopcore$hideClouds(CloudStatus cloudStatus, RenderPass renderPass, CallbackInfo ci) {
        if (BackdropClient.hasActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderOit", at = @At("HEAD"), cancellable = true)
    private void workshopcore$hideCloudsOit(CloudStatus cloudStatus, OitStage stage, GpuTextureView mainDepthTextureView, OitRenderPassProvider.Parameters params, CallbackInfo ci) {
        if (BackdropClient.hasActive()) {
            ci.cancel();
        }
    }
}*/
//?} else {
class CloudRendererMixin {
}
//?}
