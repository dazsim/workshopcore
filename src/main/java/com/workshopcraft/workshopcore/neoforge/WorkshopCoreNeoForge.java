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
        //?}
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }
}
