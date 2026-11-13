package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

//? if >=26.1 {
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(value = WorkshopCore.MOD_ID, dist = Dist.CLIENT)
public class WorkshopCoreNeoForgeClient {
    public WorkshopCoreNeoForgeClient(IEventBus modBus) {
        //? if >=26.1 {
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent e) ->
                e.registerSkyboxRenderer(BackdropSkybox.ID, BackdropSkybox.INSTANCE));
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent e) -> {
            if (BackdropClient.hasActive()) {
                e.getRenderState().customSkyboxRenderer = BackdropSkybox.INSTANCE;
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> BackdropClient.clear());
        //?}
    }
}
