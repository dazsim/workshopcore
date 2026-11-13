package com.workshopcraft.workshopcore.mixin;

import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void workshopcore$haze(Camera camera, int renderDistance, DeltaTracker delta, float darken, ClientLevel level,
                                   CallbackInfoReturnable<FogData> cir) {
        HazeClient.apply(cir.getReturnValue(), delta.getGameTimeDeltaPartialTick(false));
    }
}
