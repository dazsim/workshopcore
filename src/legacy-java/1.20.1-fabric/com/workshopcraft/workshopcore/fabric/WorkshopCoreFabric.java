package com.workshopcraft.workshopcore.fabric;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionResult;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class WorkshopCoreFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        initBackdrops();
        initHazards();
        initScrewdrivers();
        FabricMachines.init();
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }

    private static void initBackdrops() {
        com.workshopcraft.workshopcore.backdrop.BackdropNetworking.setHooks(
                WorkshopCorePackets::send,
                (level, payload) -> {
                    for (var player : level.players()) {
                        WorkshopCorePackets.send(player, payload);
                    }
                });
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(identifiable(
                com.workshopcraft.workshopcore.backdrop.BackdropReload.ID,
                new com.workshopcraft.workshopcore.backdrop.BackdropReload()));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> com.workshopcraft.workshopcore.backdrop.BackdropCommands.register(dispatcher));
        ServerWorldEvents.LOAD.register((server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(world));
        ServerWorldEvents.UNLOAD.register((server, world) -> com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelUnload(world));
        ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(handler.player));
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
            if (success) {
                for (var player : server.getPlayerList().getPlayers()) {
                    com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAllTo(player);
                }
            }
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(destination);
            com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendDefsTo(player);
            com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAssignment(player, destination.dimension(), com.workshopcraft.workshopcore.backdrop.Backdrops.get(destination));
        });
    }

    private static void initHazards() {
        com.workshopcraft.workshopcore.hazard.HazeNetworking.setHook(WorkshopCorePackets::send);
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(identifiable(
                com.workshopcraft.workshopcore.hazard.HazardProtectionReload.ID,
                new com.workshopcraft.workshopcore.hazard.HazardProtectionReload()));
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registry, environment) -> com.workshopcraft.workshopcore.hazard.HazardCommands.register(dispatcher));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            com.workshopcraft.workshopcore.hazard.Haze.forget(handler.player);
            com.workshopcraft.workshopcore.hazard.HazardProtection.clearWear(handler.player);
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                com.workshopcraft.workshopcore.hazard.Haze.resend(newPlayer));
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) ->
                com.workshopcraft.workshopcore.hazard.Haze.resend(player));
    }

    private static void initScrewdrivers() {
        com.workshopcraft.workshopcore.screwdriver.Screwdrivers.registerItem(
                (id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        com.workshopcraft.workshopcore.screwdriver.Screwdrivers.registerSerializer(
                (id, serializer) -> Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, id, serializer));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            var result = com.workshopcraft.workshopcore.screwdriver.Screwdrivers.use(
                    level, hit.getBlockPos(), player, player.getItemInHand(hand), hand, hit);
            return switch (result) {
                case SUCCESS -> InteractionResult.SUCCESS;
                case FAIL -> InteractionResult.FAIL;
                case PASS -> InteractionResult.PASS;
            };
        });
    }

    private static IdentifiableResourceReloadListener identifiable(ResourceLocation id, PreparableReloadListener inner) {
        return new IdentifiableResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return id;
            }

            @Override
            public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, ProfilerFiller prepare, ProfilerFiller apply, Executor background, Executor game) {
                return inner.reload(barrier, manager, prepare, apply, background, game);
            }
        };
    }
}
