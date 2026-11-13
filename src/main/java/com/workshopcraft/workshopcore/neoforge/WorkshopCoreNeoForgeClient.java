package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

//? if >=26.1 {
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import net.neoforged.neoforge.client.CustomCloudsRenderer;
import net.neoforged.neoforge.client.event.ExtractLevelRenderStateEvent;
import net.neoforged.neoforge.client.event.RegisterCustomEnvironmentEffectRendererEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;
//?}

@Mod(value = WorkshopCore.MOD_ID, dist = Dist.CLIENT)
public class WorkshopCoreNeoForgeClient {
    //? if >=26.1 {
    private static final CustomCloudsRenderer HIDE_CLOUDS = new CustomCloudsRenderer() {
        @Override
        public boolean renderClouds(net.minecraft.client.renderer.state.level.LevelRenderState state, net.minecraft.world.phys.Vec3 cam, net.minecraft.client.CloudStatus status, int color, float height, int range, org.joml.Matrix4fc mv) {
            return true;
        }
    };
    //?}

    public WorkshopCoreNeoForgeClient(IEventBus modBus) {
        //? if >=26.1 {
        modBus.addListener((RegisterCustomEnvironmentEffectRendererEvent e) ->
                e.registerSkyboxRenderer(BackdropSkybox.ID, BackdropSkybox.INSTANCE));
        NeoForge.EVENT_BUS.addListener((ExtractLevelRenderStateEvent e) -> {
            if (BackdropClient.hasActive()) {
                e.getRenderState().customSkyboxRenderer = BackdropSkybox.INSTANCE;
                e.getRenderState().customCloudsRenderer = HIDE_CLOUDS;
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel().isClientSide() && e.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel level) {
                BackdropClient.drop(level.dimension());
            }
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> {
            BackdropClient.clear();
            com.workshopcraft.workshopcore.hazard.client.HazeClient.clear();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ViewportEvent.RenderFog e) ->
                com.workshopcraft.workshopcore.hazard.client.HazeClient.apply(e.getFogData(), (float) e.getPartialTick()));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) ->
                e.registerBelowAll(net.minecraft.resources.Identifier.fromNamespaceAndPath(WorkshopCore.MOD_ID, "haze"),
                        com.workshopcraft.workshopcore.hazard.client.HazeClient::renderTint));
        //?}
    }
}
