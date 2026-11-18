package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket;
import com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket;
import com.workshopcraft.workshopcore.backdrop.client.BackdropClient;
import com.workshopcraft.workshopcore.hazard.HazeSyncPacket;
import com.workshopcraft.workshopcore.hazard.client.HazeClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

public class WorkshopCoreFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CtmFabricModel.register();
        ClientPlayNetworking.registerGlobalReceiver(BackdropSyncPacket.TYPE.id(), (client, handler, buf, responseSender) -> {
            BackdropSyncPacket packet = BackdropSyncPacket.read(buf);
            client.execute(packet::handle);
        });
        ClientPlayNetworking.registerGlobalReceiver(BackdropDefsPacket.TYPE.id(), (client, handler, buf, responseSender) -> {
            BackdropDefsPacket packet = BackdropDefsPacket.read(buf);
            client.execute(packet::handle);
        });
        ClientPlayNetworking.registerGlobalReceiver(HazeSyncPacket.TYPE.id(), (client, handler, buf, responseSender) -> {
            HazeSyncPacket packet = HazeSyncPacket.read(buf);
            client.execute(packet::handle);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            BackdropClient.clear();
            HazeClient.clear();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (client.level != null) {
                BackdropClient.afterClientLevelChange(client.level);
            }
        });
        HudRenderCallback.EVENT.register((graphics, tickDelta) -> HazeClient.renderTint(graphics, tickDelta));
    }
}
