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
        //?}
        initBackdrops();
        initHazards();
        initScrewdrivers();
        FabricMachines.init();
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }

    private static void initBackdrops() {
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.STREAM_CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.STREAM_CODEC);*/
        //?}
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.STREAM_CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.STREAM_CODEC);*/
        //?}
        com.workshopcraft.workshopcore.backdrop.BackdropNetworking.setHooks(
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send,
                (level, payload) -> {
                    for (var player : level.players()) {
                        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
                    }
                });
        //? if >=26.1 {
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.workshopcraft.workshopcore.backdrop.BackdropReload.ID, new com.workshopcraft.workshopcore.backdrop.BackdropReload());
        //?} else {
        /*net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener() {
                    private final com.workshopcraft.workshopcore.backdrop.BackdropReload inner = new com.workshopcraft.workshopcore.backdrop.BackdropReload();
                    @Override
                    public net.minecraft.resources.Identifier getFabricId() {
                        return com.workshopcraft.workshopcore.backdrop.BackdropReload.ID;
                    }
                    @Override
                    public java.util.concurrent.CompletableFuture<java.lang.Void> reload(net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier barrier, net.minecraft.server.packs.resources.ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller prepare, net.minecraft.util.profiling.ProfilerFiller apply, java.util.concurrent.Executor background, java.util.concurrent.Executor game) {
                        return inner.reload(barrier, manager, prepare, apply, background, game);
                    }
                });*/
        //?}
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> com.workshopcraft.workshopcore.backdrop.BackdropCommands.register(dispatcher));
        //? if >=26.1 {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.LOAD.register(
                (server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(world));
        //?} else {
        /*net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.LOAD.register(
                (server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(world));*/
        //?}
        //? if >=26.1 {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents.UNLOAD.register(
                (server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelUnload(world));
        //?} else {
        /*net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.UNLOAD.register(
                (server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelUnload(world));*/
        //?}
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(handler.player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (var player : server.getPlayerList().getPlayers()) {
                    com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(player);
                }
            }
        });
        //? if >=26.1 {
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
            if (destination instanceof net.minecraft.server.level.ServerLevel level) {
                com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(level);
                com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendDefsTo(player);
                com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAssignment(player, level.dimension(), com.workshopcraft.workshopcore.backdrop.Backdrops.get(level));
            }
        });
        //?} else {
        /*net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            if (destination instanceof net.minecraft.server.level.ServerLevel level) {
                com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(level);
                com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendDefsTo(player);
                com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAssignment(player, level.dimension(), com.workshopcraft.workshopcore.backdrop.Backdrops.get(level));
            }
        });*/
        //?}
    }

    private static void initHazards() {
        //? if >=26.1 {
        net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.TYPE,
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.STREAM_CODEC);
        //?} else {
        /*net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.playS2C().register(
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.TYPE,
                com.workshopcraft.workshopcore.hazard.HazeSyncPacket.STREAM_CODEC);*/
        //?}
        com.workshopcraft.workshopcore.hazard.HazeNetworking.setHook(
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking::send);
        //? if >=26.1 {
        net.fabricmc.fabric.api.resource.v1.ResourceLoader.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.workshopcraft.workshopcore.hazard.HazardProtectionReload.ID,
                        new com.workshopcraft.workshopcore.hazard.HazardProtectionReload());
        //?} else {
        /*net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener() {
                    private final com.workshopcraft.workshopcore.hazard.HazardProtectionReload inner = new com.workshopcraft.workshopcore.hazard.HazardProtectionReload();
                    @Override
                    public net.minecraft.resources.Identifier getFabricId() {
                        return com.workshopcraft.workshopcore.hazard.HazardProtectionReload.ID;
                    }
                    @Override
                    public java.util.concurrent.CompletableFuture<java.lang.Void> reload(net.minecraft.server.packs.resources.PreparableReloadListener.PreparationBarrier barrier, net.minecraft.server.packs.resources.ResourceManager manager, net.minecraft.util.profiling.ProfilerFiller prepare, net.minecraft.util.profiling.ProfilerFiller apply, java.util.concurrent.Executor background, java.util.concurrent.Executor game) {
                        return inner.reload(barrier, manager, prepare, apply, background, game);
                    }
                });*/
        //?}
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> com.workshopcraft.workshopcore.hazard.HazardCommands.register(dispatcher));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register(
                (handler, server) -> {
                    com.workshopcraft.workshopcore.hazard.Haze.forget(handler.player);
                    com.workshopcraft.workshopcore.hazard.HazardProtection.clearWear(handler.player);
                });
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register(
                (oldPlayer, newPlayer, alive) -> com.workshopcraft.workshopcore.hazard.Haze.resend(newPlayer));
        //? if >=26.1 {
        net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> com.workshopcraft.workshopcore.hazard.Haze.resend(player));
        //?} else {
        /*net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(
                (player, origin, destination) -> com.workshopcraft.workshopcore.hazard.Haze.resend(player));*/
        //?}
    }

    private static void initScrewdrivers() {
        com.workshopcraft.workshopcore.screwdriver.Screwdrivers.registerItem(
                (id, item) -> net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM, id, item));
        com.workshopcraft.workshopcore.screwdriver.Screwdrivers.registerSerializer(
                (id, serializer) -> net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.RECIPE_SERIALIZER, id, serializer));
        //? if >=26.1 {
        net.fabricmc.fabric.api.recipe.v1.sync.RecipeSynchronization.synchronizeRecipeSerializer(
                com.workshopcraft.workshopcore.screwdriver.KeepToolRecipe.SERIALIZER);
        //?}
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var result = com.workshopcraft.workshopcore.screwdriver.Screwdrivers.use(
                    level, hit.getBlockPos(), player, player.getItemInHand(hand), hand, hit);
            return switch (result) {
                case SUCCESS -> net.minecraft.world.InteractionResult.SUCCESS;
                case FAIL -> net.minecraft.world.InteractionResult.FAIL;
                case PASS -> net.minecraft.world.InteractionResult.PASS;
            };
        });
    }
}
