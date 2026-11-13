package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.fabricmc.api.ModInitializer;
import org.slf4j.LoggerFactory;

public class WorkshopCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        //? if >=26.1 {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.TICKET_TYPE,
                com.workshopcraft.workshopcore.compat.ChunkTickets.ID,
                com.workshopcraft.workshopcore.compat.ChunkTickets.TYPE);
        initBackdrops();
        //?}
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }

    //? if >=26.1 {
    private static void initBackdrops() {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.STREAM_CODEC);
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.STREAM_CODEC);
        com.workshopcraft.workshopcore.backdrop.BackdropNetworking.setHooks(
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send,
                (level, payload) -> {
                    for (var player : level.players()) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
                    }
                });
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.workshopcraft.workshopcore.backdrop.BackdropReload.ID, new com.workshopcraft.workshopcore.backdrop.BackdropReload());
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> com.workshopcraft.workshopcore.backdrop.BackdropCommands.register(dispatcher));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.LOAD.register(
                (server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(world));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(handler.player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (var player : server.getPlayerList().getPlayers()) {
                    com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(player);
                }
            }
        });
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            if (destination instanceof net.minecraft.server.level.ServerLevel level) {
                com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(level);
                com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAssignment(player, level.dimension(), com.workshopcraft.workshopcore.backdrop.Backdrops.get(level));
            }
        });
    }
    //?}
}
