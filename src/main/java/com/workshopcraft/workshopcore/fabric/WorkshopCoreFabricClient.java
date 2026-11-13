package com.workshopcraft.workshopcore.fabric;

import net.fabricmc.api.ClientModInitializer;

public class WorkshopCoreFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        //? if >=26.1 {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                (payload, context) -> payload.handle());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                (payload, context) -> payload.handle());
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> com.workshopcraft.workshopcore.backdrop.client.BackdropClient.clear());
        //?}
    }
}
