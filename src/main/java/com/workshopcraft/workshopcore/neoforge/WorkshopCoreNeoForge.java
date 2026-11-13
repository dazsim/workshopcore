package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.LoggerFactory;

@Mod(WorkshopCore.MOD_ID)
public class WorkshopCoreNeoForge {
    public WorkshopCoreNeoForge(IEventBus modBus) {
        //? if >=26.1 {
        modBus.addListener((net.neoforged.neoforge.registries.RegisterEvent e) ->
                e.register(net.minecraft.core.registries.Registries.TICKET_TYPE, h ->
                        h.register(com.workshopcraft.workshopcore.compat.ChunkTickets.ID,
                                com.workshopcraft.workshopcore.compat.ChunkTickets.TYPE)));
        initBackdrops(modBus);
        //?}
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }

    //? if >=26.1 {
    private static void initBackdrops(IEventBus modBus) {
        com.workshopcraft.workshopcore.backdrop.BackdropNetworking.setHooks(
                net.neoforged.neoforge.network.PacketDistributor::sendToPlayer,
                net.neoforged.neoforge.network.PacketDistributor::sendToPlayersInDimension);
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1")
                .playToClient(com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.TYPE,
                        com.workshopcraft.workshopcore.backdrop.BackdropSyncPacket.STREAM_CODEC,
                        (payload, ctx) -> ctx.enqueueWork(payload::handle))
                .playToClient(com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.TYPE,
                        com.workshopcraft.workshopcore.backdrop.BackdropDefsPacket.STREAM_CODEC,
                        (payload, ctx) -> ctx.enqueueWork(payload::handle)));
        var game = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        game.addListener((net.neoforged.neoforge.event.AddServerReloadListenersEvent e) ->
                e.addListener(com.workshopcraft.workshopcore.backdrop.BackdropReload.ID, new com.workshopcraft.workshopcore.backdrop.BackdropReload()));
        game.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) ->
                com.workshopcraft.workshopcore.backdrop.BackdropCommands.register(e.getDispatcher()));
        game.addListener((net.neoforged.neoforge.event.level.LevelEvent.Load e) -> {
            if (e.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(level);
            }
        });
        game.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload e) -> {
            if (e.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
                com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelUnload(level);
            }
        });
        game.addListener((net.neoforged.neoforge.event.OnDatapackSyncEvent e) -> {
            e.getRelevantPlayers().forEach(com.workshopcraft.workshopcore.backdrop.BackdropNetworking::sendAllTo);
        });
        game.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
                var level = player.level();
                if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.workshopcraft.workshopcore.backdrop.Backdrops.onLevelLoad(serverLevel);
                    com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendDefsTo(player);
                    com.workshopcraft.workshopcore.backdrop.BackdropNetworking.sendAssignment(
                            player, serverLevel.dimension(), com.workshopcraft.workshopcore.backdrop.Backdrops.get(serverLevel));
                }
            }
        });
    }
    //?}
}
