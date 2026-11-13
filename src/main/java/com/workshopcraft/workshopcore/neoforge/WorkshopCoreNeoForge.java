package com.workshopcraft.workshopcore.neoforge;

import com.workshopcraft.workshopcore.WorkshopCore;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.LoggerFactory;

@Mod(WorkshopCore.MOD_ID)
public class WorkshopCoreNeoForge {
    public WorkshopCoreNeoForge(IEventBus modBus) {
        LoggerFactory.getLogger(WorkshopCore.MOD_ID).info("Workshop Core {}", WorkshopCore.API_VERSION);
    }
}
