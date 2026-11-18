package com.workshopcraft.workshopcore.forge;

import com.workshopcraft.workshopcore.WorkshopCore;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = WorkshopCore.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WorkshopCoreForgeClient {
    private WorkshopCoreForgeClient() {
    }

    @SubscribeEvent
    public static void bake(net.minecraftforge.client.event.ModelEvent.ModifyBakingResult event) {
        CtmForgeModel.bake(event);
    }

    @SubscribeEvent
    public static void overlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("haze", (gui, graphics, partialTick, width, height) ->
                HazeClient.renderTint(graphics, partialTick));
    }

    @Mod.EventBusSubscriber(modid = WorkshopCore.MOD_ID, value = Dist.CLIENT)
    public static final class Game {
        private Game() {
        }

        @SubscribeEvent
        public static void fog(ViewportEvent.RenderFog event) {
            float partial = (float) event.getPartialTick();
            if (!HazeClient.hasHaze(partial)) {
                return;
            }
            event.setNearPlaneDistance(HazeClient.nearPlane(event.getNearPlaneDistance(), partial));
            event.setFarPlaneDistance(HazeClient.farPlane(event.getFarPlaneDistance(), partial));
            event.setCanceled(true);
        }

        @SubscribeEvent
        public static void fogColor(ViewportEvent.ComputeFogColor event) {
            float partial = (float) event.getPartialTick();
            event.setRed(HazeClient.fogRed(event.getRed(), partial));
            event.setGreen(HazeClient.fogGreen(event.getGreen(), partial));
            event.setBlue(HazeClient.fogBlue(event.getBlue(), partial));
        }

        @SubscribeEvent
        public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
            BackdropClient.clear();
            HazeClient.clear();
        }

        @SubscribeEvent
        public static void joinLevel(EntityJoinLevelEvent event) {
            Minecraft minecraft = Minecraft.getInstance();
            if (event.getLevel() instanceof ClientLevel level && event.getEntity() == minecraft.player) {
                BackdropClient.afterClientLevelChange(level);
            }
        }
    }
}
