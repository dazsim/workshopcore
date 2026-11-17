package com.workshopcraft.workshopcore.fabric;

import net.fabricmc.api.ClientModInitializer;

public class WorkshopCoreFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CtmFabricModel.register();
        //? if >=26.1 {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register(
                (client, level) -> com.workshopcraft.workshopcore.backdrop.client.BackdropClient.afterClientLevelChange(level));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.workshopcraft.workshopcore.backdrop.client.BackdropClient.clear());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry.addFirst(
                com.workshopcraft.workshopcore.Ids.of(com.workshopcraft.workshopcore.WorkshopCore.MOD_ID, "haze"),
                com.workshopcraft.workshopcore.hazard.client.HazeClient::renderTint);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.workshopcraft.workshopcore.hazard.client.HazeClient.clear());
        //?} else {
        /*net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.workshopcraft.workshopcore.backdrop.client.BackdropClient.clear());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.TYPE,
                (payload, context) -> context.client().execute(payload::handle));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.workshopcraft.workshopcore.hazard.client.HazeClient.clear());
        net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register((graphics, tickDelta) ->
                com.workshopcraft.workshopcore.hazard.client.HazeClient.renderTint(graphics, net.minecraft.client.Minecraft.getInstance().getTimer()));*/
        //?}
    }
}
