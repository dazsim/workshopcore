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
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }
}
